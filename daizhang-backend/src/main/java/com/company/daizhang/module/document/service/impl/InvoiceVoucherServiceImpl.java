package com.company.daizhang.module.document.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.document.service.InvoiceVoucherService;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票生成凭证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceVoucherServiceImpl implements InvoiceVoucherService {

    private final InputInvoiceMapper inputInvoiceMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SubjectMapper subjectMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final VoucherWordMapper voucherWordMapper;
    private final AccountSetAccessService accountSetAccessService;

    // 进项发票相关科目编码
    private static final String CODE_INVENTORY = "1405";          // 库存商品
    // 进项税额应记"应交税费-应交增值税(进项税额)"子科目,与销项区分,
    // 否则进项/销项混记在同一"2221"下,科目余额表无法反映应交增值税明细,税务申报无法取数。
    // getSubjectIdByCode 会先精确匹配"2221.01",找不到再回退到父科目"2221",保持兼容。
    private static final String CODE_INPUT_TAX = "2221.01";       // 应交税费-应交增值税(进项税额)
    private static final String CODE_ACCOUNTS_PAYABLE = "2202";  // 应付账款

    // 销项发票相关科目编码
    private static final String CODE_ACCOUNTS_RECEIVABLE = "1122"; // 应收账款
    private static final String CODE_MAIN_REVENUE = "5001";       // 主营业务收入
    private static final String CODE_OUTPUT_TAX = "2221.02";      // 应交税费-应交增值税(销项税额)

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateInputVoucher(Long invoiceId) {
        InputInvoice invoice = inputInvoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_NOT_FOUND);
        }

        if (invoice.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该进项发票已生成凭证");
        }

        // 校验发票认证状态：未认证发票不可生成进项抵扣凭证
        if (invoice.getAuthStatus() == null || invoice.getAuthStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "进项发票未认证，不可生成凭证抵扣进项税");
        }

        Long accountSetId = invoice.getAccountSetId();
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限（写凭证）
        accountSetAccessService.checkOwner(accountSetId);
        LocalDate voucherDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        // 校验会计期间
        AccountPeriod period = checkPeriodExists(accountSetId, year, month);
        validateVoucherDateInRange(voucherDate, period.getStartDate(), period.getEndDate());
        checkPeriodNotClosed(period);

        // 查询科目ID
        Long inventorySubjectId = getSubjectIdByCode(accountSetId, CODE_INVENTORY, "库存商品");
        Long inputTaxSubjectId = getSubjectIdByCode(accountSetId, CODE_INPUT_TAX, "应交税费-进项税额");
        Long payableSubjectId = getSubjectIdByCode(accountSetId, CODE_ACCOUNTS_PAYABLE, "应付账款");

        // 计算金额
        BigDecimal amount = nullToZero(invoice.getAmount());
        BigDecimal taxAmount = nullToZero(invoice.getTaxAmount());
        BigDecimal totalAmount = nullToZero(invoice.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            totalAmount = amount.add(taxAmount);
        }

        String sellerName = StrUtil.isBlank(invoice.getSellerName()) ? "进项发票" : invoice.getSellerName();
        String summary = "采购入库-" + sellerName;

        // 创建凭证
        Voucher voucher = buildVoucher(accountSetId, voucherDate, year, month, totalAmount, totalAmount);
        voucherMapper.insert(voucher);

        // 创建凭证明细
        List<VoucherDetail> details = new ArrayList<>();
        // 借: 库存商品 金额=不含税金额
        details.add(buildDetail(voucher.getId(), 1, summary, inventorySubjectId, accountSetId, CODE_INVENTORY, amount, BigDecimal.ZERO));
        // 借: 应交税费-进项税额 金额=税额
        details.add(buildDetail(voucher.getId(), 2, summary, inputTaxSubjectId, accountSetId, CODE_INPUT_TAX, taxAmount, BigDecimal.ZERO));
        // 贷: 应付账款 金额=价税合计
        details.add(buildDetail(voucher.getId(), 3, summary, payableSubjectId, accountSetId, CODE_ACCOUNTS_PAYABLE, BigDecimal.ZERO, totalAmount));
        for (VoucherDetail detail : details) {
            voucherDetailMapper.insert(detail);
        }

        // 更新发票的voucherId
        invoice.setVoucherId(voucher.getId());
        inputInvoiceMapper.updateById(invoice);

        log.info("进项发票生成凭证成功，发票ID: {}, 凭证ID: {}, 凭证号: {}", invoiceId, voucher.getId(), voucher.getVoucherNo());
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateOutputVoucher(Long invoiceId) {
        OutputInvoice invoice = outputInvoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_NOT_FOUND);
        }

        if (invoice.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该销项发票已生成凭证");
        }

        // 校验发票状态：作废/红冲发票不可生成收入凭证
        if (invoice.getInvoiceStatus() == null || invoice.getInvoiceStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "销项发票非正常状态，不可生成凭证");
        }

        Long accountSetId = invoice.getAccountSetId();
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限（写凭证）
        accountSetAccessService.checkOwner(accountSetId);
        LocalDate voucherDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        // 校验会计期间
        AccountPeriod period = checkPeriodExists(accountSetId, year, month);
        validateVoucherDateInRange(voucherDate, period.getStartDate(), period.getEndDate());
        checkPeriodNotClosed(period);

        // 查询科目ID
        Long receivableSubjectId = getSubjectIdByCode(accountSetId, CODE_ACCOUNTS_RECEIVABLE, "应收账款");
        Long revenueSubjectId = getSubjectIdByCode(accountSetId, CODE_MAIN_REVENUE, "主营业务收入");
        Long outputTaxSubjectId = getSubjectIdByCode(accountSetId, CODE_OUTPUT_TAX, "应交税费-销项税额");

        // 计算金额
        BigDecimal amount = nullToZero(invoice.getAmount());
        BigDecimal taxAmount = nullToZero(invoice.getTaxAmount());
        BigDecimal totalAmount = nullToZero(invoice.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            totalAmount = amount.add(taxAmount);
        }

        // 红冲/负数发票：金额为负时借贷方向反转，金额取绝对值，避免应交税费贷方余额为负（多交税款应挂借方）
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absAmount = amount.abs();
        BigDecimal absTaxAmount = taxAmount.abs();
        BigDecimal absTotalAmount = totalAmount.abs();

        String buyerName = StrUtil.isBlank(invoice.getBuyerName()) ? "销项发票" : invoice.getBuyerName();
        String summary = "销售收款-" + buyerName;

        // 创建凭证
        Voucher voucher = buildVoucher(accountSetId, voucherDate, year, month, absTotalAmount, absTotalAmount);
        voucherMapper.insert(voucher);

        // 创建凭证明细
        List<VoucherDetail> details = new ArrayList<>();
        if (!isNegative) {
            // 正常销售：借:应收账款(价税合计) 贷:主营业务收入(不含税) 贷:应交税费-销项税额(税额)
            details.add(buildDetail(voucher.getId(), 1, summary, receivableSubjectId, accountSetId, CODE_ACCOUNTS_RECEIVABLE, absTotalAmount, BigDecimal.ZERO));
            details.add(buildDetail(voucher.getId(), 2, summary, revenueSubjectId, accountSetId, CODE_MAIN_REVENUE, BigDecimal.ZERO, absAmount));
            details.add(buildDetail(voucher.getId(), 3, summary, outputTaxSubjectId, accountSetId, CODE_OUTPUT_TAX, BigDecimal.ZERO, absTaxAmount));
        } else {
            // 红冲/负数：借贷方向反转 借:主营业务收入(不含税) 借:应交税费-销项税额(税额) 贷:应收账款(价税合计)
            details.add(buildDetail(voucher.getId(), 1, summary, revenueSubjectId, accountSetId, CODE_MAIN_REVENUE, absAmount, BigDecimal.ZERO));
            details.add(buildDetail(voucher.getId(), 2, summary, outputTaxSubjectId, accountSetId, CODE_OUTPUT_TAX, absTaxAmount, BigDecimal.ZERO));
            details.add(buildDetail(voucher.getId(), 3, summary, receivableSubjectId, accountSetId, CODE_ACCOUNTS_RECEIVABLE, BigDecimal.ZERO, absTotalAmount));
        }
        for (VoucherDetail detail : details) {
            voucherDetailMapper.insert(detail);
        }

        // 更新发票的voucherId
        invoice.setVoucherId(voucher.getId());
        outputInvoiceMapper.updateById(invoice);

        log.info("销项发票生成凭证成功，发票ID: {}, 凭证ID: {}, 凭证号: {}", invoiceId, voucher.getId(), voucher.getVoucherNo());
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateInputVouchers(Long accountSetId, String startDate, String endDate) {
        LambdaQueryWrapper<InputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InputInvoice::getAccountSetId, accountSetId)
               .isNull(InputInvoice::getVoucherId);
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(InputInvoice::getInvoiceDate, LocalDate.parse(startDate, DATE_FORMATTER));
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(InputInvoice::getInvoiceDate, LocalDate.parse(endDate, DATE_FORMATTER));
        }
        wrapper.orderByAsc(InputInvoice::getInvoiceDate);

        List<InputInvoice> invoices = inputInvoiceMapper.selectList(wrapper);
        List<Long> voucherIds = new ArrayList<>();
        for (InputInvoice invoice : invoices) {
            try {
                Long voucherId = generateInputVoucher(invoice.getId());
                voucherIds.add(voucherId);
            } catch (BusinessException e) {
                log.warn("批量生成进项发票凭证跳过，发票ID: {}, 原因: {}", invoice.getId(), e.getMessage());
            }
        }
        log.info("批量生成进项发票凭证完成，账套ID: {}, 成功数量: {}", accountSetId, voucherIds.size());
        return voucherIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateOutputVouchers(Long accountSetId, String startDate, String endDate) {
        LambdaQueryWrapper<OutputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
               .isNull(OutputInvoice::getVoucherId);
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(OutputInvoice::getInvoiceDate, LocalDate.parse(startDate, DATE_FORMATTER));
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(OutputInvoice::getInvoiceDate, LocalDate.parse(endDate, DATE_FORMATTER));
        }
        wrapper.orderByAsc(OutputInvoice::getInvoiceDate);

        List<OutputInvoice> invoices = outputInvoiceMapper.selectList(wrapper);
        List<Long> voucherIds = new ArrayList<>();
        for (OutputInvoice invoice : invoices) {
            try {
                Long voucherId = generateOutputVoucher(invoice.getId());
                voucherIds.add(voucherId);
            } catch (BusinessException e) {
                log.warn("批量生成销项发票凭证跳过，发票ID: {}, 原因: {}", invoice.getId(), e.getMessage());
            }
        }
        log.info("批量生成销项发票凭证完成，账套ID: {}, 成功数量: {}", accountSetId, voucherIds.size());
        return voucherIds;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建凭证实体
     */
    private Voucher buildVoucher(Long accountSetId, LocalDate voucherDate, int year, int month,
                                 BigDecimal totalDebit, BigDecimal totalCredit) {
        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherWordId(getDefaultVoucherWordId(accountSetId));
        voucher.setVoucherNo(generateVoucherNo(accountSetId, year, month));
        voucher.setVoucherDate(voucherDate);
        voucher.setYear(year);
        voucher.setMonth(month);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setAttachmentCount(0);
        voucher.setStatus(0);
        voucher.setSource(1);
        return voucher;
    }

    /**
     * 构建凭证明细实体
     */
    private VoucherDetail buildDetail(Long voucherId, int lineNo, String summary, Long subjectId,
                                      Long accountSetId, String subjectCode,
                                      BigDecimal debit, BigDecimal credit) {
        VoucherDetail detail = new VoucherDetail();
        detail.setVoucherId(voucherId);
        detail.setLineNo(lineNo);
        detail.setSummary(summary);
        detail.setSubjectId(subjectId);
        detail.setSubjectCode(subjectCode);
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject != null) {
            detail.setSubjectName(subject.getName());
        }
        detail.setDebit(debit);
        detail.setCredit(credit);
        detail.setSortOrder(lineNo);
        return detail;
    }

    /**
     * 通过科目编码查询科目ID
     */
    private Long getSubjectIdByCode(Long accountSetId, String code, String subjectName) {
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (subject == null) {
            // 精确匹配失败，尝试前缀匹配（如2221.01.01匹配2221）
            String prefix = code.split("\\.")[0];
            subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getAccountSetId, accountSetId)
                    .eq(Subject::getCode, prefix));
            if (subject == null) {
                throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID.getCode(),
                        "未查询到科目[" + subjectName + "]，编码: " + code);
            }
            log.warn("科目[{}]精确编码{}未找到，使用前缀编码{}替代", subjectName, code, prefix);
        }
        return subject.getId();
    }

    /**
     * 获取默认凭证字ID
     */
    private Long getDefaultVoucherWordId(Long accountSetId) {
        LambdaQueryWrapper<VoucherWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherWord::getAccountSetId, accountSetId)
               .eq(VoucherWord::getStatus, 1)
               .orderByAsc(VoucherWord::getSortOrder)
               .last("LIMIT 1");
        VoucherWord voucherWord = voucherWordMapper.selectOne(wrapper);
        if (voucherWord == null) {
            LambdaQueryWrapper<VoucherWord> fallback = new LambdaQueryWrapper<>();
            fallback.eq(VoucherWord::getAccountSetId, accountSetId)
                    .last("LIMIT 1");
            voucherWord = voucherWordMapper.selectOne(fallback);
        }
        return voucherWord != null ? voucherWord.getId() : null;
    }

    /**
     * 生成凭证号：格式 year-month-sequence，如 2026-06-001
     */
    private String generateVoucherNo(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .notLike(Voucher::getVoucherNo, "TMP-%");
        List<Voucher> list = voucherMapper.selectList(wrapper);

        int sequence = 1;
        if (list != null && !list.isEmpty()) {
            // Java层提取序号取最大值,避免数据库CAST/SUBSTRING兼容性问题(凭证号含两个'-')
            int maxSeq = list.stream()
                    .map(Voucher::getVoucherNo)
                    .filter(StrUtil::isNotBlank)
                    .mapToInt(this::extractVoucherSequence)
                    .max()
                    .orElse(0);
            sequence = maxSeq + 1;
        }
        return String.format("%d-%02d-%03d", year, month, sequence);
    }

    /**
     * 从凭证号中提取序号(用于排序)
     * 凭证号格式: 2026-01-001,取最后一个连字符后的数字部分
     */
    private int extractVoucherSequence(String voucherNo) {
        if (StrUtil.isBlank(voucherNo)) {
            return 0;
        }
        int lastDash = voucherNo.lastIndexOf('-');
        if (lastDash < 0 || lastDash == voucherNo.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(voucherNo.substring(lastDash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 检查会计期间是否存在
     */
    private AccountPeriod checkPeriodExists(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getYear, year)
               .eq(AccountPeriod::getMonth, month);
        AccountPeriod period = accountPeriodMapper.selectOne(wrapper);
        if (period == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_PERIOD_NOT_FOUND);
        }
        return period;
    }

    /**
     * 校验凭证日期是否在会计期间范围内
     */
    private void validateVoucherDateInRange(LocalDate voucherDate, LocalDate startDate, LocalDate endDate) {
        if (voucherDate.isBefore(startDate) || voucherDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VOUCHER_DATE_INVALID);
        }
    }

    /**
     * 检查会计期间是否已结账
     */
    private void checkPeriodNotClosed(AccountPeriod period) {
        if (period.getStatus() != null && period.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PERIOD_CLOSED);
        }
    }

    /**
     * null转0
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
