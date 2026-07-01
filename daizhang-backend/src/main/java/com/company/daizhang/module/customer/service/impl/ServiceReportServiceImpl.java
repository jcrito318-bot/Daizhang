package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.ServiceReportRequest;
import com.company.daizhang.module.customer.entity.ServiceReport;
import com.company.daizhang.module.customer.mapper.ServiceReportMapper;
import com.company.daizhang.module.customer.service.ServiceReportService;
import com.company.daizhang.module.customer.vo.ServiceReportVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户服务报告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceReportServiceImpl extends ServiceImpl<ServiceReportMapper, ServiceReport> implements ServiceReportService {

    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SubjectMapper subjectMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;

    @Override
    public PageResult<ServiceReportVO> pageReports(Long accountSetId, Long customerId, Integer reportYear,
                                                   Integer reportMonth, int pageNum, int pageSize) {
        Page<ServiceReport> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ServiceReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(accountSetId != null, ServiceReport::getAccountSetId, accountSetId)
               .eq(customerId != null, ServiceReport::getCustomerId, customerId)
               .eq(reportYear != null, ServiceReport::getReportYear, reportYear)
               .eq(reportMonth != null, ServiceReport::getReportMonth, reportMonth)
               .orderByDesc(ServiceReport::getCreateTime);

        Page<ServiceReport> result = this.page(page, wrapper);

        List<ServiceReportVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public ServiceReportVO getReportById(Long id) {
        ServiceReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(404, "服务报告不存在");
        }
        return convertToVO(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReport(ServiceReportRequest request) {
        ServiceReport report = new ServiceReport();
        BeanUtil.copyProperties(request, report);
        // 默认为草稿状态
        if (report.getStatus() == null) {
            report.setStatus(0);
        }
        // 金额默认值
        if (report.getTotalRevenue() == null) {
            report.setTotalRevenue(BigDecimal.ZERO);
        }
        if (report.getTotalExpense() == null) {
            report.setTotalExpense(BigDecimal.ZERO);
        }
        if (report.getNetProfit() == null) {
            report.setNetProfit(BigDecimal.ZERO);
        }
        if (report.getTaxAmount() == null) {
            report.setTaxAmount(BigDecimal.ZERO);
        }
        this.save(report);
        log.info("创建服务报告成功，账套ID: {}, 年度: {}, 月份: {}", 
                report.getAccountSetId(), report.getReportYear(), report.getReportMonth());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(Long id, ServiceReportRequest request) {
        ServiceReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(404, "服务报告不存在");
        }
        // 已发布的报告不能修改
        if (report.getStatus() != null && report.getStatus() == 1) {
            throw new BusinessException(400, "已发布的报告不能修改");
        }

        // 使用ignoreNullValue避免request中status为null时把已有status覆盖为null,导致已审核状态丢失
        BeanUtil.copyProperties(request, report, cn.hutool.core.bean.copier.CopyOptions.create().ignoreNullValue());
        report.setId(id);
        this.updateById(report);
        log.info("更新服务报告成功，报告ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        ServiceReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(404, "服务报告不存在");
        }
        this.removeById(id);
        log.info("删除服务报告成功，报告ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishReport(Long id) {
        ServiceReport report = this.getById(id);
        if (report == null) {
            throw new BusinessException(404, "服务报告不存在");
        }
        // 已发布的报告不能重复发布
        if (report.getStatus() != null && report.getStatus() == 1) {
            throw new BusinessException(400, "报告已发布，不能重复发布");
        }
        report.setStatus(1);
        this.updateById(report);
        log.info("发布服务报告成功，报告ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceReportVO generateReport(Long accountSetId, Long customerId, Integer year, Integer month) {
        // 1. 查询该账套该期间的凭证数据（已审核或已过账）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(month != null, Voucher::getMonth, month)
                .ge(Voucher::getStatus, 1);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        // 2. 查询损益类科目（用于区分收入和支出）
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCategory, "损益")
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 3. 汇总收入和支出
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        if (!vouchers.isEmpty()) {
            List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
            List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

            for (VoucherDetail detail : details) {
                Subject subject = subjectMap.get(detail.getSubjectId());
                if (subject == null) {
                    continue;
                }
                BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                // balanceDirection=1 借方(费用)，balanceDirection=2 贷方(收入)
                if (subject.getBalanceDirection() != null && subject.getBalanceDirection() == 2) {
                    totalRevenue = totalRevenue.add(credit);
                } else if (subject.getBalanceDirection() != null && subject.getBalanceDirection() == 1) {
                    totalExpense = totalExpense.add(debit);
                }
            }
        }

        // 4. 查询税务数据，汇总纳税额
        BigDecimal taxAmount = BigDecimal.ZERO;
        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        taxWrapper.eq(TaxDeclaration::getAccountSetId, accountSetId)
                .eq(TaxDeclaration::getYear, year)
                .eq(month != null, TaxDeclaration::getMonth, month);
        List<TaxDeclaration> taxDeclarations = taxDeclarationMapper.selectList(taxWrapper);
        for (TaxDeclaration tax : taxDeclarations) {
            BigDecimal amount = tax.getActualAmount() != null ? tax.getActualAmount()
                    : (tax.getTaxAmount() != null ? tax.getTaxAmount() : BigDecimal.ZERO);
            taxAmount = taxAmount.add(amount);
        }

        // 5. 计算净利润
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        // 6. 生成财务摘要文本
        String periodDesc = month != null ? year + "年" + month + "月" : year + "年度";
        String financialSummary = String.format(
                "%s财务报告：本期总收入%s元，总支出%s元，净利润%s元，纳税总额%s元。",
                periodDesc,
                totalRevenue.toPlainString(),
                totalExpense.toPlainString(),
                netProfit.toPlainString(),
                taxAmount.toPlainString());

        // 7. 生成风险提示
        String riskWarning = generateRiskWarning(totalRevenue, totalExpense, netProfit, taxAmount);

        // 8. 生成经营建议
        String suggestion = generateSuggestion(totalRevenue, totalExpense, netProfit);

        // 9. 保存为草稿状态
        ServiceReport report = new ServiceReport();
        report.setAccountSetId(accountSetId);
        report.setCustomerId(customerId);
        report.setReportYear(year);
        report.setReportMonth(month);
        // 根据是否有月份确定报告类型
        report.setReportType(month == null ? "年度" : "月度");
        report.setTotalRevenue(totalRevenue);
        report.setTotalExpense(totalExpense);
        report.setNetProfit(netProfit);
        report.setTaxAmount(taxAmount);
        report.setFinancialSummary(financialSummary);
        report.setRiskWarning(riskWarning);
        report.setSuggestion(suggestion);
        report.setStatus(0);
        this.save(report);

        log.info("自动生成服务报告成功，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month);
        return convertToVO(report);
    }

    /**
     * 生成风险提示
     */
    private String generateRiskWarning(BigDecimal totalRevenue, BigDecimal totalExpense,
                                        BigDecimal netProfit, BigDecimal taxAmount) {
        StringBuilder warning = new StringBuilder();
        if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            warning.append("本期出现亏损，净利润为负，需关注经营状况。");
        }
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expenseRate = totalExpense.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP);
            if (expenseRate.compareTo(new BigDecimal("0.9")) > 0) {
                warning.append("支出占收入比例过高（超过90%），成本控制存在风险。");
            }
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) == 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            warning.append("本期有收入但无纳税记录，请核实税务申报情况。");
        }
        if (warning.length() == 0) {
            warning.append("本期财务状况正常，暂无明显风险。");
        }
        return warning.toString();
    }

    /**
     * 生成经营建议
     */
    private String generateSuggestion(BigDecimal totalRevenue, BigDecimal totalExpense, BigDecimal netProfit) {
        StringBuilder suggestion = new StringBuilder();
        if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            suggestion.append("建议优化成本结构，控制费用支出，提升盈利能力。");
        } else if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitRate = netProfit.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP);
            if (profitRate.compareTo(new BigDecimal("0.1")) < 0) {
                suggestion.append("本期利润率偏低，建议加强成本管控，提高运营效率。");
            } else {
                suggestion.append("本期经营状况良好，建议保持现有经营策略，持续提升业绩。");
            }
        } else {
            suggestion.append("本期无收入数据，建议积极拓展业务。");
        }
        return suggestion.toString();
    }

    /**
     * 报告实体转VO
     */
    private ServiceReportVO convertToVO(ServiceReport report) {
        ServiceReportVO vo = new ServiceReportVO();
        BeanUtil.copyProperties(report, vo);
        return vo;
    }
}
