package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.utils.ExcelImportUtil;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.SubjectBalanceMapper;
import com.company.daizhang.module.accountset.service.SubjectBalanceImportService;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 期初余额Excel导入服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectBalanceImportServiceImpl implements SubjectBalanceImportService {

    private final SubjectBalanceMapper subjectBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final AccountBalanceMapper accountBalanceMapper;

    /**
     * 期次：期初
     */
    private static final Integer PERIOD_BEGIN = 1;

    private static final String COL_CODE = "科目编码";
    private static final String COL_NAME = "科目名称";
    private static final String COL_BEGIN_DEBIT = "期初借方";
    private static final String COL_BEGIN_CREDIT = "期初贷方";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importBalances(Long accountSetId, Integer year, MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = ExcelImportUtil.parseExcel(file, 0);
        } catch (IOException e) {
            log.error("解析Excel失败", e);
            return ImportResultVO.of(0, 0, 0, Collections.singletonList("Excel文件解析失败: " + e.getMessage()));
        }

        if (rows.isEmpty()) {
            return ImportResultVO.of(0, 0, 0, Collections.singletonList("Excel文件中没有数据行"));
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;
            try {
                successCount += importOneBalance(accountSetId, year, row);
            } catch (Exception e) {
                failCount++;
                errorMessages.add("第" + rowNum + "行: " + e.getMessage());
            }
        }

        log.info("期初余额导入完成，账套ID: {}, 年度: {}, 总数: {}, 成功: {}, 失败: {}",
                accountSetId, year, rows.size(), successCount, failCount);

        return ImportResultVO.of(rows.size(), successCount, failCount, errorMessages);
    }

    /**
     * 导入单条期初余额
     *
     * @return 成功返回1，失败抛出异常
     */
    private int importOneBalance(Long accountSetId, Integer year, Map<String, String> row) {
        // 校验必填字段
        ExcelImportUtil.validateRequired(row, COL_CODE);

        String code = row.get(COL_CODE).trim();
        String name = StrUtil.trim(row.get(COL_NAME));
        String debitStr = StrUtil.trim(row.get(COL_BEGIN_DEBIT));
        String creditStr = StrUtil.trim(row.get(COL_BEGIN_CREDIT));

        // 按科目编码查找科目
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (subject == null) {
            throw new BusinessException("科目编码不存在: " + code);
        }

        // 转换金额
        BigDecimal beginDebit = ExcelImportUtil.toBigDecimal(debitStr);
        BigDecimal beginCredit = ExcelImportUtil.toBigDecimal(creditStr);

        // 校验金额不能为负数
        if (beginDebit.compareTo(BigDecimal.ZERO) < 0 || beginCredit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能为负数");
        }

        // 校验借贷方不能同时有值
        if (beginDebit.compareTo(BigDecimal.ZERO) > 0 && beginCredit.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("期初借方和期初贷方不能同时有值");
        }

        // 查询是否已存在该科目的期初余额（存在则更新，不存在则新增）
        SubjectBalance existing = subjectBalanceMapper.selectOne(new LambdaQueryWrapper<SubjectBalance>()
                .eq(SubjectBalance::getAccountSetId, accountSetId)
                .eq(SubjectBalance::getYear, year)
                .eq(SubjectBalance::getPeriod, PERIOD_BEGIN)
                .eq(SubjectBalance::getSubjectId, subject.getId()));

        if (existing != null) {
            existing.setSubjectCode(subject.getCode());
            existing.setSubjectName(subject.getName());
            existing.setBeginDebit(beginDebit);
            existing.setBeginCredit(beginCredit);
            subjectBalanceMapper.updateById(existing);
        } else {
            SubjectBalance balance = new SubjectBalance();
            balance.setAccountSetId(accountSetId);
            balance.setSubjectId(subject.getId());
            balance.setSubjectCode(subject.getCode());
            balance.setSubjectName(StrUtil.isNotBlank(name) ? name : subject.getName());
            balance.setYear(year);
            balance.setPeriod(PERIOD_BEGIN);
            balance.setBeginDebit(beginDebit);
            balance.setBeginCredit(beginCredit);
            subjectBalanceMapper.insert(balance);
        }

        // 同步到 AccountBalance(year, month=1):与 SubjectBalanceServiceImpl.saveBatch 保持一致,
        // 否则导入的期初余额不会进入月度余额表/资产负债表(这些报表读 AccountBalance)。
        syncAccountBalanceBegin(accountSetId, year, subject.getId(), beginDebit, beginCredit, subject.getBalanceDirection());

        return 1;
    }

    /**
     * 同步期初余额到 AccountBalance(year, month=1)。
     * 仅更新 beginDebit/beginCredit 并重算 endDebit/endCredit,保留已过账凭证产生的 period/year 累计。
     */
    private void syncAccountBalanceBegin(Long accountSetId, Integer year, Long subjectId,
                                         BigDecimal beginDebit, BigDecimal beginCredit, Integer balanceDirectionRaw) {
        if (subjectId == null) {
            return;
        }
        BigDecimal bd = beginDebit != null ? beginDebit : BigDecimal.ZERO;
        BigDecimal bc = beginCredit != null ? beginCredit : BigDecimal.ZERO;
        int balanceDirection = balanceDirectionRaw != null ? balanceDirectionRaw : 1;

        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
               .eq(AccountBalance::getSubjectId, subjectId)
               .eq(AccountBalance::getYear, year)
               .eq(AccountBalance::getMonth, 1);
        AccountBalance ab = accountBalanceMapper.selectOne(wrapper);

        if (ab == null) {
            ab = new AccountBalance();
            ab.setAccountSetId(accountSetId);
            ab.setSubjectId(subjectId);
            ab.setYear(year);
            ab.setMonth(1);
            ab.setBeginDebit(bd);
            ab.setBeginCredit(bc);
            ab.setPeriodDebit(BigDecimal.ZERO);
            ab.setPeriodCredit(BigDecimal.ZERO);
            ab.setYearDebit(BigDecimal.ZERO);
            ab.setYearCredit(BigDecimal.ZERO);
            applyEndBalance(ab, balanceDirection);
            accountBalanceMapper.insert(ab);
        } else {
            ab.setBeginDebit(bd);
            ab.setBeginCredit(bc);
            applyEndBalance(ab, balanceDirection);
            accountBalanceMapper.updateById(ab);
        }
    }

    private void applyEndBalance(AccountBalance ab, int balanceDirection) {
        BigDecimal beginD = ab.getBeginDebit() != null ? ab.getBeginDebit() : BigDecimal.ZERO;
        BigDecimal beginC = ab.getBeginCredit() != null ? ab.getBeginCredit() : BigDecimal.ZERO;
        BigDecimal periodD = ab.getPeriodDebit() != null ? ab.getPeriodDebit() : BigDecimal.ZERO;
        BigDecimal periodC = ab.getPeriodCredit() != null ? ab.getPeriodCredit() : BigDecimal.ZERO;
        if (balanceDirection == 2) {
            BigDecimal net = beginC.add(periodC).subtract(periodD);
            if (net.compareTo(BigDecimal.ZERO) >= 0) {
                ab.setEndCredit(net);
                ab.setEndDebit(BigDecimal.ZERO);
            } else {
                ab.setEndCredit(BigDecimal.ZERO);
                ab.setEndDebit(net.abs());
            }
        } else {
            BigDecimal net = beginD.add(periodD).subtract(periodC);
            if (net.compareTo(BigDecimal.ZERO) >= 0) {
                ab.setEndDebit(net);
                ab.setEndCredit(BigDecimal.ZERO);
            } else {
                ab.setEndDebit(BigDecimal.ZERO);
                ab.setEndCredit(net.abs());
            }
        }
    }

    @Override
    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("期初余额导入模板");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {COL_CODE, COL_NAME, COL_BEGIN_DEBIT, COL_BEGIN_CREDIT};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 示例数据
            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("1001");
            sample1.createCell(1).setCellValue("库存现金");
            sample1.createCell(2).setCellValue("10000");
            sample1.createCell(3).setCellValue("");

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("2001");
            sample2.createCell(1).setCellValue("短期借款");
            sample2.createCell(2).setCellValue("");
            sample2.createCell(3).setCellValue("50000");

            // 列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 15 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成期初余额导入模板失败", e);
            throw new BusinessException("生成模板失败");
        }
    }
}
