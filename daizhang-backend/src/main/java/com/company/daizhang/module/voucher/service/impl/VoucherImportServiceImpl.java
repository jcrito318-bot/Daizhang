package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.ExcelImportUtil;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import com.company.daizhang.module.voucher.service.VoucherImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 凭证Excel导入服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherImportServiceImpl implements VoucherImportService {

    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final VoucherWordMapper voucherWordMapper;
    private final SubjectMapper subjectMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final TransactionTemplate transactionTemplate;

    private static final String COL_DATE = "凭证日期";
    private static final String COL_WORD = "凭证字";
    private static final String COL_SUMMARY = "摘要";
    private static final String COL_SUBJECT_CODE = "科目编码";
    private static final String COL_SUBJECT_NAME = "科目名称";
    private static final String COL_DEBIT = "借方金额";
    private static final String COL_CREDIT = "贷方金额";
    private static final String COL_ATTACHMENT = "附件数";

    @Override
    public ImportResultVO importVouchers(Long accountSetId, MultipartFile file) {
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

        // 大文件保护：Excel全量加载到内存，万行级可能OOM，单次最多5000行
        if (rows.size() > 5000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "导入行数过多(" + rows.size() + ")，单次最多5000行，请分批导入");
        }

        // 按凭证日期+凭证字分组
        LinkedHashMap<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String date = StrUtil.trim(row.get(COL_DATE));
            String word = StrUtil.trim(row.get(COL_WORD));
            String key = date + "|" + word;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        int groupIndex = 0;
        for (Map.Entry<String, List<Map<String, String>>> entry : groups.entrySet()) {
            groupIndex++;
            List<Map<String, String>> groupRows = entry.getValue();
            try {
                // 每组凭证独立事务，保证原子性
                transactionTemplate.execute(status -> {
                    createVoucherFromGroup(accountSetId, groupRows);
                    return null;
                });
                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMessages.add("第" + groupIndex + "组凭证(日期/凭证字:" + entry.getKey() + "): " + e.getMessage());
            }
        }

        log.info("凭证导入完成，账套ID: {}, 总凭证数: {}, 成功: {}, 失败: {}",
                accountSetId, groups.size(), successCount, failCount);

        return ImportResultVO.of(groups.size(), successCount, failCount, errorMessages);
    }

    /**
     * 根据分组数据创建一张凭证（含明细）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherFromGroup(Long accountSetId, List<Map<String, String>> groupRows) {
        Map<String, String> firstRow = groupRows.get(0);

        String dateStr = StrUtil.trim(firstRow.get(COL_DATE));
        String wordStr = StrUtil.trim(firstRow.get(COL_WORD));
        String attachmentStr = StrUtil.trim(firstRow.get(COL_ATTACHMENT));

        // 校验凭证日期
        if (StrUtil.isBlank(dateStr)) {
            throw new BusinessException("凭证日期不能为空");
        }
        LocalDate voucherDate = ExcelImportUtil.toLocalDate(dateStr);
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        // 查找凭证字
        Long voucherWordId = null;
        if (StrUtil.isNotBlank(wordStr)) {
            VoucherWord word = voucherWordMapper.selectOne(new LambdaQueryWrapper<VoucherWord>()
                    .eq(VoucherWord::getAccountSetId, accountSetId)
                    .eq(VoucherWord::getName, wordStr));
            if (word == null) {
                throw new BusinessException("凭证字不存在: " + wordStr);
            }
            voucherWordId = word.getId();
        }

        // 校验会计期间
        AccountPeriod period = accountPeriodMapper.selectOne(new LambdaQueryWrapper<AccountPeriod>()
                .eq(AccountPeriod::getAccountSetId, accountSetId)
                .eq(AccountPeriod::getYear, year)
                .eq(AccountPeriod::getMonth, month));
        if (period == null) {
            throw new BusinessException("会计期间不存在: " + year + "-" + month);
        }
        if (period.getStatus() != null && period.getStatus() == 1) {
            throw new BusinessException("会计期间已结账: " + year + "-" + month);
        }
        if (voucherDate.isBefore(period.getStartDate()) || voucherDate.isAfter(period.getEndDate())) {
            throw new BusinessException("凭证日期不在会计期间范围内");
        }

        // 构建凭证明细
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<VoucherDetail> details = new ArrayList<>();

        for (int i = 0; i < groupRows.size(); i++) {
            Map<String, String> row = groupRows.get(i);
            String summary = StrUtil.trim(row.get(COL_SUMMARY));
            String subjectCode = StrUtil.trim(row.get(COL_SUBJECT_CODE));
            String debitStr = StrUtil.trim(row.get(COL_DEBIT));
            String creditStr = StrUtil.trim(row.get(COL_CREDIT));

            if (StrUtil.isBlank(summary)) {
                throw new BusinessException("第" + (i + 1) + "行摘要不能为空");
            }
            if (StrUtil.isBlank(subjectCode)) {
                throw new BusinessException("第" + (i + 1) + "行科目编码不能为空");
            }

            // 按科目编码查找科目
            Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getAccountSetId, accountSetId)
                    .eq(Subject::getCode, subjectCode));
            if (subject == null) {
                throw new BusinessException("第" + (i + 1) + "行科目编码不存在: " + subjectCode);
            }
            if (subject.getStatus() != null && subject.getStatus() != 1) {
                throw new BusinessException("第" + (i + 1) + "行科目已停用: " + subjectCode);
            }

            BigDecimal debit = ExcelImportUtil.toBigDecimal(debitStr);
            BigDecimal credit = ExcelImportUtil.toBigDecimal(creditStr);

            // 校验金额
            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("第" + (i + 1) + "行金额不能为负数");
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException("第" + (i + 1) + "行借贷方金额不能同时为零");
            }
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("第" + (i + 1) + "行借贷方金额不能同时有值");
            }

            VoucherDetail detail = new VoucherDetail();
            detail.setLineNo(i + 1);
            detail.setSummary(summary);
            detail.setSubjectId(subject.getId());
            detail.setSubjectCode(subject.getCode());
            detail.setSubjectName(subject.getName());
            detail.setDebit(debit);
            detail.setCredit(credit);
            detail.setSortOrder(i + 1);
            details.add(detail);

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        // 校验借贷平衡
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("凭证借贷不平衡，借方合计: " + totalDebit + "，贷方合计: " + totalCredit);
        }
        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("凭证金额不能为零");
        }

        // 创建凭证
        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherWordId(voucherWordId);
        voucher.setVoucherDate(voucherDate);
        voucher.setYear(year);
        voucher.setMonth(month);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setAttachmentCount(StrUtil.isNotBlank(attachmentStr) ? ExcelImportUtil.toInteger(attachmentStr) : 0);
        voucher.setStatus(0);
        voucher.setSource(0);

        // 生成凭证号并插入：generateVoucherNo 仅查最大序号+1非原子，并发导入可能重号，
        // 此处通过捕获唯一键冲突重试，避免并发重号
        int maxRetry = 3;
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            String voucherNo = generateVoucherNo(accountSetId, year, month);
            voucher.setVoucherNo(voucherNo);
            try {
                voucherMapper.insert(voucher);
                break;
            } catch (DuplicateKeyException e) {
                if (attempt == maxRetry - 1) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "凭证号生成失败(并发冲突)，请重试");
                }
                log.warn("凭证号冲突,重试: {}", voucherNo);
                voucher.setId(null);
            }
        }

        // 保存凭证明细
        for (VoucherDetail detail : details) {
            detail.setVoucherId(voucher.getId());
            voucherDetailMapper.insert(detail);
        }
    }

    /**
     * 生成凭证号：格式 year-month-sequence，如 2026-06-001
     * 基于本期最大序号+1,排除TMP-%临时号(否则字符串排序下TMP号排最前,parse失败导致重号)
     */
    private String generateVoucherNo(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .notLike(Voucher::getVoucherNo, "TMP-%")
               .orderByDesc(Voucher::getVoucherNo)
               .last("LIMIT 1");
        List<Voucher> vouchers = voucherMapper.selectList(wrapper);

        int sequence = 1;
        if (!vouchers.isEmpty()) {
            String lastVoucherNo = vouchers.get(0).getVoucherNo();
            if (StrUtil.isNotBlank(lastVoucherNo)) {
                String[] parts = lastVoucherNo.split("-");
                if (parts.length == 3) {
                    try {
                        sequence = Integer.parseInt(parts[2]) + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return String.format("%d-%02d-%03d", year, month, sequence);
    }

    @Override
    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("凭证导入模板");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {COL_DATE, COL_WORD, COL_SUMMARY, COL_SUBJECT_CODE, COL_SUBJECT_NAME,
                    COL_DEBIT, COL_CREDIT, COL_ATTACHMENT};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 示例数据：一张凭证（两行明细，借贷平衡）
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("2026-01-05");
            r1.createCell(1).setCellValue("记");
            r1.createCell(2).setCellValue("收到投资款");
            r1.createCell(3).setCellValue("1002");
            r1.createCell(4).setCellValue("银行存款");
            r1.createCell(5).setCellValue("100000");
            r1.createCell(6).setCellValue("");
            r1.createCell(7).setCellValue("1");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("2026-01-05");
            r2.createCell(1).setCellValue("记");
            r2.createCell(2).setCellValue("收到投资款");
            r2.createCell(3).setCellValue("3001");
            r2.createCell(4).setCellValue("实收资本");
            r2.createCell(5).setCellValue("");
            r2.createCell(6).setCellValue("100000");
            r2.createCell(7).setCellValue("1");

            // 列宽
            sheet.setColumnWidth(0, 14 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 20 * 256);
            sheet.setColumnWidth(5, 12 * 256);
            sheet.setColumnWidth(6, 12 * 256);
            sheet.setColumnWidth(7, 10 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成凭证导入模板失败", e);
            throw new BusinessException("生成模板失败");
        }
    }
}
