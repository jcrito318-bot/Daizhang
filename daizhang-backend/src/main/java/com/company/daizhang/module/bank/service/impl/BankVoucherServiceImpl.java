package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.bank.service.BankVoucherService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 银行流水生成凭证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankVoucherServiceImpl implements BankVoucherService {

    private final BankTransactionMapper bankTransactionMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SubjectMapper subjectMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final VoucherWordMapper voucherWordMapper;

    // 科目编码常量
    private static final String CODE_BANK_DEPOSIT = "1002";          // 银行存款
    private static final String CODE_ACCOUNTS_PAYABLE = "2202";      // 应付账款
    private static final String CODE_INVENTORY = "1405";             // 库存商品
    private static final String CODE_MAIN_REVENUE = "6001";          // 主营业务收入
    private static final String CODE_SALARY_PAYABLE = "2211";        // 应付职工薪酬
    private static final String CODE_TAX_PAYABLE = "2221";           // 应交税费
    private static final String CODE_MANAGEMENT_EXPENSE = "6602";    // 管理费用
    private static final String CODE_FINANCE_EXPENSE = "6603";      // 财务费用
    private static final String CODE_NON_OPERATING_INCOME = "6301";  // 营业外收入
    private static final String CODE_NON_OPERATING_EXPENSE = "6711"; // 营业外支出

    /**
     * 交易类型：收入
     */
    private static final int TRANSACTION_TYPE_INCOME = 1;
    /**
     * 交易类型：支出
     */
    private static final int TRANSACTION_TYPE_EXPENSE = 2;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateVoucher(Long transactionId) {
        BankTransaction transaction = bankTransactionMapper.selectById(transactionId);
        if (transaction == null) {
            throw new BusinessException("银行流水不存在");
        }

        if (transaction.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该银行流水已生成凭证");
        }

        Long accountSetId = transaction.getAccountSetId();
        LocalDate voucherDate = transaction.getTransactionDate() != null
                ? transaction.getTransactionDate() : LocalDate.now();
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        // 校验会计期间
        AccountPeriod period = checkPeriodExists(accountSetId, year, month);
        validateVoucherDateInRange(voucherDate, period.getStartDate(), period.getEndDate());
        checkPeriodNotClosed(period);

        // 查询银行存款科目ID
        Long bankSubjectId = getSubjectIdByCode(accountSetId, CODE_BANK_DEPOSIT, "银行存款");

        BigDecimal amount = nullToZero(transaction.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "银行流水金额必须大于零");
        }

        Integer transactionType = transaction.getTransactionType();
        String summary = StrUtil.isBlank(transaction.getSummary()) ? "银行流水" : transaction.getSummary();

        // 创建凭证
        Voucher voucher = buildVoucher(accountSetId, voucherDate, year, month, amount, amount);
        voucherMapper.insert(voucher);

        if (transactionType != null && transactionType == TRANSACTION_TYPE_INCOME) {
            // 收入(贷方): 借:银行存款 贷:根据摘要判断
            Long offsetSubjectId = getIncomeOffsetSubjectId(accountSetId, summary);
            String offsetCode = getIncomeOffsetSubjectCode(summary);

            // 借: 银行存款
            VoucherDetail debitDetail = buildDetail(voucher.getId(), 1, summary, bankSubjectId,
                    accountSetId, CODE_BANK_DEPOSIT, amount, BigDecimal.ZERO);
            voucherDetailMapper.insert(debitDetail);

            // 贷: 对方科目
            VoucherDetail creditDetail = buildDetail(voucher.getId(), 2, summary, offsetSubjectId,
                    accountSetId, offsetCode, BigDecimal.ZERO, amount);
            voucherDetailMapper.insert(creditDetail);

        } else if (transactionType != null && transactionType == TRANSACTION_TYPE_EXPENSE) {
            // 支出(借方): 借:根据摘要判断 贷:银行存款
            Long offsetSubjectId = getExpenseOffsetSubjectId(accountSetId, summary);
            String offsetCode = getExpenseOffsetSubjectCode(summary);

            // 借: 对方科目
            VoucherDetail debitDetail = buildDetail(voucher.getId(), 1, summary, offsetSubjectId,
                    accountSetId, offsetCode, amount, BigDecimal.ZERO);
            voucherDetailMapper.insert(debitDetail);

            // 贷: 银行存款
            VoucherDetail creditDetail = buildDetail(voucher.getId(), 2, summary, bankSubjectId,
                    accountSetId, CODE_BANK_DEPOSIT, BigDecimal.ZERO, amount);
            voucherDetailMapper.insert(creditDetail);

        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "银行流水交易类型不正确");
        }

        // 更新流水的匹配状态和凭证ID
        transaction.setMatchedStatus(1);
        transaction.setVoucherId(voucher.getId());
        bankTransactionMapper.updateById(transaction);

        log.info("银行流水生成凭证成功，流水ID: {}, 凭证ID: {}, 凭证号: {}", transactionId, voucher.getId(), voucher.getVoucherNo());
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateVouchers(Long accountSetId) {
        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getAccountSetId, accountSetId)
               .eq(BankTransaction::getMatchedStatus, 0)
               .isNull(BankTransaction::getVoucherId)
               .orderByAsc(BankTransaction::getTransactionDate);

        List<BankTransaction> transactions = bankTransactionMapper.selectList(wrapper);
        List<Long> voucherIds = new ArrayList<>();
        for (BankTransaction transaction : transactions) {
            try {
                Long voucherId = generateVoucher(transaction.getId());
                voucherIds.add(voucherId);
            } catch (BusinessException e) {
                log.warn("批量生成银行流水凭证跳过，流水ID: {}, 原因: {}", transaction.getId(), e.getMessage());
            }
        }
        log.info("批量生成银行流水凭证完成，账套ID: {}, 成功数量: {}", accountSetId, voucherIds.size());
        return voucherIds;
    }

    // ==================== 摘要关键词匹配 ====================

    /**
     * 根据摘要获取收入类对方科目ID
     */
    private Long getIncomeOffsetSubjectId(Long accountSetId, String summary) {
        String code = getIncomeOffsetSubjectCode(summary);
        return getSubjectIdByCode(accountSetId, code, getSubjectNameByCode(code));
    }

    /**
     * 根据摘要获取支出类对方科目ID
     */
    private Long getExpenseOffsetSubjectId(Long accountSetId, String summary) {
        String code = getExpenseOffsetSubjectCode(summary);
        return getSubjectIdByCode(accountSetId, code, getSubjectNameByCode(code));
    }

    /**
     * 根据摘要获取收入类对方科目编码
     * 收入类：贷方科目
     */
    private String getIncomeOffsetSubjectCode(String summary) {
        if (StrUtil.isBlank(summary)) {
            return CODE_NON_OPERATING_INCOME;
        }
        // 包含"销售"/"收入" → 主营业务收入
        if (summary.contains("销售") || summary.contains("收入")) {
            return CODE_MAIN_REVENUE;
        }
        // 包含"货款"/"采购" → 应付账款（收回货款）或主营业务收入
        if (summary.contains("货款") || summary.contains("采购")) {
            return CODE_ACCOUNTS_PAYABLE;
        }
        // 包含"利息" → 财务费用（利息收入冲减财务费用）
        if (summary.contains("利息")) {
            return CODE_FINANCE_EXPENSE;
        }
        // 其他 → 营业外收入
        return CODE_NON_OPERATING_INCOME;
    }

    /**
     * 根据摘要获取支出类对方科目编码
     * 支出类：借方科目
     */
    private String getExpenseOffsetSubjectCode(String summary) {
        if (StrUtil.isBlank(summary)) {
            return CODE_NON_OPERATING_EXPENSE;
        }
        // 包含"货款"/"采购" → 库存商品或应付账款
        if (summary.contains("货款") || summary.contains("采购")) {
            return CODE_ACCOUNTS_PAYABLE;
        }
        // 包含"工资" → 应付职工薪酬
        if (summary.contains("工资") || summary.contains("薪酬") || summary.contains("社保")) {
            return CODE_SALARY_PAYABLE;
        }
        // 包含"税"/"税费" → 应交税费
        if (summary.contains("税") || summary.contains("税费")) {
            return CODE_TAX_PAYABLE;
        }
        // 包含"差旅"/"办公" → 管理费用
        if (summary.contains("差旅") || summary.contains("办公")) {
            return CODE_MANAGEMENT_EXPENSE;
        }
        // 包含"利息" → 财务费用
        if (summary.contains("利息") || summary.contains("手续费")) {
            return CODE_FINANCE_EXPENSE;
        }
        // 其他 → 营业外支出
        return CODE_NON_OPERATING_EXPENSE;
    }

    /**
     * 根据科目编码获取科目名称（用于日志和异常信息）
     */
    private String getSubjectNameByCode(String code) {
        switch (code) {
            case CODE_BANK_DEPOSIT: return "银行存款";
            case CODE_ACCOUNTS_PAYABLE: return "应付账款";
            case CODE_INVENTORY: return "库存商品";
            case CODE_MAIN_REVENUE: return "主营业务收入";
            case CODE_SALARY_PAYABLE: return "应付职工薪酬";
            case CODE_TAX_PAYABLE: return "应交税费";
            case CODE_MANAGEMENT_EXPENSE: return "管理费用";
            case CODE_FINANCE_EXPENSE: return "财务费用";
            case CODE_NON_OPERATING_INCOME: return "营业外收入";
            case CODE_NON_OPERATING_EXPENSE: return "营业外支出";
            default: return "未知科目";
        }
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
            throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID.getCode(),
                    "未查询到科目[" + subjectName + "]，编码: " + code);
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
               .orderByDesc(Voucher::getVoucherNo)
               .last("LIMIT 1");
        Voucher lastVoucher = voucherMapper.selectOne(wrapper);

        int sequence = 1;
        if (lastVoucher != null && StrUtil.isNotBlank(lastVoucher.getVoucherNo())) {
            String[] parts = lastVoucher.getVoucherNo().split("-");
            if (parts.length == 3) {
                try {
                    sequence = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("%d-%02d-%03d", year, month, sequence);
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
