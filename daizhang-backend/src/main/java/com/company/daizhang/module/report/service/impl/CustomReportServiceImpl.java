package com.company.daizhang.module.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.report.dto.CustomReportRequest;
import com.company.daizhang.module.report.entity.CustomReport;
import com.company.daizhang.module.report.entity.CustomReportItem;
import com.company.daizhang.module.report.mapper.CustomReportItemMapper;
import com.company.daizhang.module.report.mapper.CustomReportMapper;
import com.company.daizhang.module.report.service.CustomReportService;
import com.company.daizhang.module.report.vo.CustomReportDataVO;
import com.company.daizhang.module.report.vo.CustomReportVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自定义报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomReportServiceImpl extends ServiceImpl<CustomReportMapper, CustomReport> implements CustomReportService {

    private final CustomReportItemMapper customReportItemMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public PageResult<CustomReportVO> pageReports(String reportName, int pageNum, int pageSize) {
        Page<CustomReport> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<CustomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(reportName), CustomReport::getReportName, reportName)
               .orderByDesc(CustomReport::getCreateTime);

        Page<CustomReport> result = this.page(page, wrapper);

        List<CustomReportVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public CustomReportVO getReportById(Long id) {
        CustomReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "自定义报表不存在");
        }

        CustomReportVO vo = convertToVO(report);

        // 查询报表项目
        LambdaQueryWrapper<CustomReportItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(CustomReportItem::getReportId, id)
                  .orderByAsc(CustomReportItem::getRowNo);
        List<CustomReportItem> items = customReportItemMapper.selectList(itemWrapper);

        List<CustomReportVO.CustomReportItemVO> itemVOs = items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReport(CustomReportRequest request) {
        // 检查编码是否已存在
        LambdaQueryWrapper<CustomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomReport::getReportCode, request.getReportCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表编码已存在");
        }

        // 保存报表
        CustomReport report = new CustomReport();
        BeanUtil.copyProperties(request, report);
        if (report.getStatus() == null) {
            report.setStatus(1);
        }
        this.save(report);

        // 保存报表项目
        saveItems(report.getId(), request.getItems());

        log.info("创建自定义报表成功，报表ID: {}, 报表名称: {}", report.getId(), report.getReportName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(Long id, CustomReportRequest request) {
        CustomReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "自定义报表不存在");
        }

        // 检查编码是否与其他记录冲突
        LambdaQueryWrapper<CustomReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomReport::getReportCode, request.getReportCode())
               .ne(CustomReport::getId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表编码已存在");
        }

        // 更新报表
        BeanUtil.copyProperties(request, report);
        report.setId(id);
        this.updateById(report);

        // 删除旧项目
        LambdaQueryWrapper<CustomReportItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(CustomReportItem::getReportId, id);
        customReportItemMapper.delete(deleteWrapper);

        // 保存新项目
        saveItems(id, request.getItems());

        log.info("更新自定义报表成功，报表ID: {}, 报表名称: {}", id, report.getReportName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        CustomReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "自定义报表不存在");
        }

        // 删除报表项目
        LambdaQueryWrapper<CustomReportItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(CustomReportItem::getReportId, id);
        customReportItemMapper.delete(itemWrapper);

        // 删除报表
        this.removeById(id);

        log.info("删除自定义报表成功，报表ID: {}, 报表名称: {}", id, report.getReportName());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomReportDataVO executeReport(Long id, Long accountSetId, Integer year, Integer month) {
        CustomReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "自定义报表不存在");
        }

        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);

        // 查询报表项目
        LambdaQueryWrapper<CustomReportItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(CustomReportItem::getReportId, id)
                  .orderByAsc(CustomReportItem::getRowNo);
        List<CustomReportItem> items = customReportItemMapper.selectList(itemWrapper);

        // 查询该账套该期间所有科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 查询该账套所有科目，构建科目编码到科目ID的映射
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<String, Long> subjectCodeToIdMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getCode, Subject::getId, (a, b) -> a));

        // 构建科目ID到余额的映射
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b, (a, b) -> a));

        // 计算每个项目的金额
        List<CustomReportDataVO.CustomReportDataRow> rows = new ArrayList<>();
        for (CustomReportItem item : items) {
            CustomReportDataVO.CustomReportDataRow row = new CustomReportDataVO.CustomReportDataRow();
            row.setRowNo(item.getRowNo());
            row.setItemName(item.getItemName());
            row.setFormula(item.getFormula());
            row.setDisplayDirection(item.getDisplayDirection());
            row.setIsTotal(item.getIsTotal());
            row.setParentRowNo(item.getParentRowNo());

            // 计算金额
            BigDecimal amount = calculateItemAmount(item, subjectCodeToIdMap, balanceMap);
            row.setAmount(amount);

            rows.add(row);
        }

        CustomReportDataVO vo = new CustomReportDataVO();
        vo.setReportId(report.getId());
        vo.setReportName(report.getReportName());
        vo.setReportCode(report.getReportCode());
        vo.setAccountSetId(accountSetId);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setRows(rows);

        log.info("执行自定义报表取数成功，报表ID: {}, 账套ID: {}, 年度: {}, 月份: {}", id, accountSetId, year, month);

        return vo;
    }

    /**
     * 计算报表项目金额
     * 解析公式(如"1001+1002-1003")，查询对应科目余额并按显示方向汇总
     */
    private BigDecimal calculateItemAmount(CustomReportItem item, Map<String, Long> subjectCodeToIdMap,
                                            Map<Long, AccountBalance> balanceMap) {
        // 合计行不参与取数
        if (item.getIsTotal() != null && item.getIsTotal() == 1) {
            return BigDecimal.ZERO;
        }

        String formula = item.getFormula();
        if (StrUtil.isBlank(formula)) {
            return BigDecimal.ZERO;
        }

        // 显示方向: 0-借方 1-贷方，默认借方
        Integer displayDirection = item.getDisplayDirection();
        boolean useCredit = displayDirection != null && displayDirection == 1;

        // 解析公式，支持 + 和 - 运算符
        BigDecimal result = BigDecimal.ZERO;
        // 使用正则将公式拆分为科目编码和运算符
        // 先按 + 拆分，再处理每个部分中的 - 
        // 简单实现：使用正则匹配科目编码和符号
        String parsedFormula = formula.replaceAll("\\s+", "");
        // 按运算符拆分，保留运算符
        String[] tokens = parsedFormula.split("(?=[+-])|(?<=[+-])");
        
        boolean subtract = false;
        for (String token : tokens) {
            if ("+".equals(token)) {
                subtract = false;
                continue;
            }
            if ("-".equals(token)) {
                subtract = true;
                continue;
            }
            if (token.isEmpty()) {
                continue;
            }

            // token 为科目编码
            Long subjectId = subjectCodeToIdMap.get(token);
            if (subjectId == null) {
                log.warn("报表项目取数时未找到科目编码: {}", token);
                continue;
            }

            AccountBalance balance = balanceMap.get(subjectId);
            BigDecimal amount = BigDecimal.ZERO;
            if (balance != null) {
                if (useCredit) {
                    amount = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                } else {
                    amount = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                }
            }

            if (subtract) {
                result = result.subtract(amount);
            } else {
                result = result.add(amount);
            }
        }

        return result;
    }

    /**
     * 保存报表项目
     */
    private void saveItems(Long reportId, List<CustomReportRequest.CustomReportItemRequest> itemRequests) {
        for (int i = 0; i < itemRequests.size(); i++) {
            CustomReportRequest.CustomReportItemRequest request = itemRequests.get(i);
            CustomReportItem item = new CustomReportItem();
            item.setReportId(reportId);
            item.setRowNo(request.getRowNo() != null ? request.getRowNo() : i + 1);
            item.setItemName(request.getItemName());
            item.setFormula(request.getFormula());
            item.setDisplayDirection(request.getDisplayDirection() != null ? request.getDisplayDirection() : 0);
            item.setIsTotal(request.getIsTotal() != null ? request.getIsTotal() : 0);
            item.setParentRowNo(request.getParentRowNo());
            customReportItemMapper.insert(item);
        }
    }

    /**
     * 报表实体转VO
     */
    private CustomReportVO convertToVO(CustomReport report) {
        CustomReportVO vo = new CustomReportVO();
        BeanUtil.copyProperties(report, vo);
        return vo;
    }

    /**
     * 报表项目实体转VO
     */
    private CustomReportVO.CustomReportItemVO convertItemToVO(CustomReportItem item) {
        CustomReportVO.CustomReportItemVO vo = new CustomReportVO.CustomReportItemVO();
        BeanUtil.copyProperties(item, vo);
        return vo;
    }
}
