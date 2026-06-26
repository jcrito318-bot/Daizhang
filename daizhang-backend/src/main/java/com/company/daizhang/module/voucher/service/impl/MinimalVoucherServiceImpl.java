package com.company.daizhang.module.voucher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.mapper.EmployeeMapper;
import com.company.daizhang.module.salary.mapper.SalarySheetMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.service.MinimalVoucherService;
import com.company.daizhang.module.voucher.service.VoucherService;
import com.company.daizhang.module.voucher.vo.MinimalAccountSetVO;
import com.company.daizhang.module.voucher.vo.MinimalVoucherBatchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinimalVoucherServiceImpl implements MinimalVoucherService {

    private final AccountSetMapper accountSetMapper;
    private final CustomerMapper customerMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final InputInvoiceMapper inputInvoiceMapper;
    private final BankTransactionMapper bankTransactionMapper;
    private final VoucherService voucherService;
    private final SubjectMapper subjectMapper;
    private final EmployeeMapper employeeMapper;
    private final SalarySheetMapper salarySheetMapper;

    @Override
    public List<MinimalAccountSetVO> identifyMinimalAccountSets(Integer year, Integer month) {
        List<AccountSet> accountSets = accountSetMapper.selectList(null);
        List<MinimalAccountSetVO> result = new ArrayList<>();

        for (AccountSet accountSet : accountSets) {
            MinimalAccountSetVO vo = buildMinimalAccountSetVO(accountSet, year, month);
            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MinimalVoucherBatchResultVO batchGenerateMinimalVouchers(List<Long> accountSetIds,
                                                                     Integer year, Integer month,
                                                                     List<String> voucherTypes) {
        MinimalVoucherBatchResultVO result = new MinimalVoucherBatchResultVO();
        result.setYear(year);
        result.setMonth(month);
        result.setTotalCount(accountSetIds.size());
        result.setSuccessCount(0);
        result.setFailCount(0);
        result.setVoucherCount(0);
        result.setFailItems(new ArrayList<>());

        int voucherCount = 0;
        int successCount = 0;
        int failCount = 0;
        List<MinimalVoucherBatchResultVO.MinimalVoucherFailItem> failItems = new ArrayList<>();

        for (Long accountSetId : accountSetIds) {
            try {
                AccountSet accountSet = accountSetMapper.selectById(accountSetId);
                if (accountSet == null) {
                    failCount++;
                    failItems.add(buildFailItem(accountSetId, "", "账套不存在"));
                    continue;
                }

                int generated = 0;
                for (String voucherType : voucherTypes) {
                    boolean success = generateMinimalVoucher(accountSet, year, month, voucherType);
                    if (success) {
                        generated++;
                    }
                }

                if (generated > 0) {
                    successCount++;
                    voucherCount += generated;
                } else {
                    failCount++;
                    failItems.add(buildFailItem(accountSetId, accountSet.getName(), "未生成任何凭证"));
                }
            } catch (Exception e) {
                failCount++;
                AccountSet accountSet = accountSetMapper.selectById(accountSetId);
                String name = accountSet != null ? accountSet.getName() : "";
                failItems.add(buildFailItem(accountSetId, name, e.getMessage()));
                log.error("极简记账失败 accountSetId={}: {}", accountSetId, e.getMessage(), e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setVoucherCount(voucherCount);
        result.setFailItems(failItems);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAuditMinimalVouchers(List<Long> accountSetIds, Integer year, Integer month) {
        int totalAudited = 0;
        for (Long accountSetId : accountSetIds) {
            LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .eq(Voucher::getStatus, 0)
                    .eq(Voucher::getDraftStatus, 0);
            List<Voucher> vouchers = voucherService.list(wrapper);
            List<Long> ids = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            if (!ids.isEmpty()) {
                totalAudited += voucherService.batchAuditVoucher(ids);
            }
        }
        return totalAudited;
    }

    private MinimalAccountSetVO buildMinimalAccountSetVO(AccountSet accountSet, Integer year, Integer month) {
        MinimalAccountSetVO vo = new MinimalAccountSetVO();
        vo.setAccountSetId(accountSet.getId());
        vo.setAccountSetName(accountSet.getName());
        vo.setYear(year);
        vo.setMonth(month);

        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Customer::getAccountSetId, accountSet.getId());
        Customer customer = customerMapper.selectOne(customerWrapper);
        vo.setCustomerName(customer != null ? customer.getCustomerName() : "");

        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSet.getId())
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        Long outputCount = outputInvoiceMapper.selectCount(outputWrapper);
        vo.setOutputInvoiceCount(outputCount != null ? outputCount.intValue() : 0);

        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSet.getId())
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        Long inputCount = inputInvoiceMapper.selectCount(inputWrapper);
        vo.setInputInvoiceCount(inputCount != null ? inputCount.intValue() : 0);

        LambdaQueryWrapper<BankTransaction> bankWrapper = new LambdaQueryWrapper<>();
        bankWrapper.eq(BankTransaction::getAccountSetId, accountSet.getId())
                .apply("YEAR(transaction_date) = {0} AND MONTH(transaction_date) = {1}", year, month);
        Long bankCount = bankTransactionMapper.selectCount(bankWrapper);
        vo.setBankTransactionCount(bankCount != null ? bankCount.intValue() : 0);

        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSet.getId())
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getDraftStatus, 0);
        Long vCount = voucherService.count(voucherWrapper);
        vo.setVoucherCount(vCount != null ? vCount.intValue() : 0);

        boolean isZero = vo.getOutputInvoiceCount() == 0
                && vo.getInputInvoiceCount() == 0
                && vo.getBankTransactionCount() == 0;
        vo.setIsZeroDeclaration(isZero);

        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getAccountSetId, accountSet.getId())
                .eq(Employee::getStatus, 1);
        Long empCount = employeeMapper.selectCount(empWrapper);
        vo.setEmployeeCount(empCount != null ? empCount.intValue() : 0);

        LambdaQueryWrapper<SalarySheet> sheetWrapper = new LambdaQueryWrapper<>();
        sheetWrapper.eq(SalarySheet::getAccountSetId, accountSet.getId())
                .eq(SalarySheet::getYear, year)
                .eq(SalarySheet::getMonth, month);
        List<SalarySheet> sheets = salarySheetMapper.selectList(sheetWrapper);
        BigDecimal totalNetSalary = sheets.stream()
                .map(s -> s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setEstimatedSalaryAmount(totalNetSalary);

        return vo;
    }

    private boolean generateMinimalVoucher(AccountSet accountSet, Integer year, Integer month, String voucherType) {
        VoucherCreateRequest request = new VoucherCreateRequest();
        request.setAccountSetId(accountSet.getId());
        request.setYear(year);
        request.setMonth(month);
        request.setVoucherDate(LocalDate.of(year, month, getLastDayOfMonth(year, month)));
        request.setAttachmentCount(0);
        request.setDraftStatus(0);

        List<VoucherDetailRequest> details = new ArrayList<>();
        String summary = "";

        switch (voucherType) {
            case "SALARY":
                summary = "计提" + year + "年" + month + "月工资";
                details = buildSalaryVoucherDetails(accountSet.getId(), year, month, summary);
                break;
            case "UTILITY":
                summary = "支付" + year + "年" + month + "月水电费";
                details = buildUtilityVoucherDetails(accountSet.getId(), summary);
                break;
            case "RENT":
                summary = "支付" + year + "年" + month + "月房租";
                details = buildRentVoucherDetails(accountSet.getId(), summary);
                break;
            default:
                return false;
        }

        if (details.isEmpty()) {
            return false;
        }

        request.setDetails(details);
        voucherService.createVoucher(request);
        return true;
    }

    private List<VoucherDetailRequest> buildSalaryVoucherDetails(Long accountSetId, Integer year, Integer month, String summary) {
        List<VoucherDetailRequest> details = new ArrayList<>();

        Long salaryExpenseSubjectId = getSubjectIdByCode(accountSetId, "5602.01");
        if (salaryExpenseSubjectId == null) {
            salaryExpenseSubjectId = getSubjectIdByCodePrefix(accountSetId, "5602");
        }
        Long payableSalarySubjectId = getSubjectIdByCode(accountSetId, "2211");
        if (payableSalarySubjectId == null) {
            payableSalarySubjectId = getSubjectIdByCodePrefix(accountSetId, "2211");
        }
        if (salaryExpenseSubjectId == null || payableSalarySubjectId == null) {
            return details;
        }

        LambdaQueryWrapper<SalarySheet> sheetWrapper = new LambdaQueryWrapper<>();
        sheetWrapper.eq(SalarySheet::getAccountSetId, accountSetId)
                .eq(SalarySheet::getYear, year)
                .eq(SalarySheet::getMonth, month);
        List<SalarySheet> sheets = salarySheetMapper.selectList(sheetWrapper);
        BigDecimal totalSalary = sheets.stream()
                .map(s -> {
                    BigDecimal base = s.getBaseSalary() != null ? s.getBaseSalary() : BigDecimal.ZERO;
                    BigDecimal allowance = s.getAllowance() != null ? s.getAllowance() : BigDecimal.ZERO;
                    BigDecimal bonus = s.getBonus() != null ? s.getBonus() : BigDecimal.ZERO;
                    return base.add(allowance).add(bonus);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = totalSalary.compareTo(BigDecimal.ZERO) > 0 ? totalSalary : new BigDecimal("3000");

        Subject expenseSubject = subjectMapper.selectById(salaryExpenseSubjectId);
        Subject payableSubject = subjectMapper.selectById(payableSalarySubjectId);

        VoucherDetailRequest debit = new VoucherDetailRequest();
        debit.setLineNo(1);
        debit.setSummary(summary);
        debit.setSubjectId(salaryExpenseSubjectId);
        debit.setSubjectCode(expenseSubject != null ? expenseSubject.getCode() : "5602");
        debit.setSubjectName(expenseSubject != null ? expenseSubject.getName() : "管理费用-工资");
        debit.setDebit(amount);
        debit.setCredit(BigDecimal.ZERO);
        debit.setSortOrder(1);
        details.add(debit);

        VoucherDetailRequest credit = new VoucherDetailRequest();
        credit.setLineNo(2);
        credit.setSummary(summary);
        credit.setSubjectId(payableSalarySubjectId);
        credit.setSubjectCode(payableSubject != null ? payableSubject.getCode() : "2211");
        credit.setSubjectName(payableSubject != null ? payableSubject.getName() : "应付职工薪酬");
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(amount);
        credit.setSortOrder(2);
        details.add(credit);

        return details;
    }

    private List<VoucherDetailRequest> buildUtilityVoucherDetails(Long accountSetId, String summary) {
        List<VoucherDetailRequest> details = new ArrayList<>();

        Long utilityExpenseSubjectId = getSubjectIdByCode(accountSetId, "5602.02");
        if (utilityExpenseSubjectId == null) {
            utilityExpenseSubjectId = getSubjectIdByCodePrefix(accountSetId, "5602");
        }
        Long cashSubjectId = getSubjectIdByCode(accountSetId, "1001");
        if (utilityExpenseSubjectId == null || cashSubjectId == null) {
            return details;
        }

        BigDecimal amount = new BigDecimal("500");

        Subject utilitySubject = subjectMapper.selectById(utilityExpenseSubjectId);
        Subject cashSubject = subjectMapper.selectById(cashSubjectId);

        VoucherDetailRequest debit = new VoucherDetailRequest();
        debit.setLineNo(1);
        debit.setSummary(summary);
        debit.setSubjectId(utilityExpenseSubjectId);
        debit.setSubjectCode(utilitySubject != null ? utilitySubject.getCode() : "5602");
        debit.setSubjectName(utilitySubject != null ? utilitySubject.getName() : "管理费用-水电费");
        debit.setDebit(amount);
        debit.setCredit(BigDecimal.ZERO);
        debit.setSortOrder(1);
        details.add(debit);

        VoucherDetailRequest credit = new VoucherDetailRequest();
        credit.setLineNo(2);
        credit.setSummary(summary);
        credit.setSubjectId(cashSubjectId);
        credit.setSubjectCode(cashSubject != null ? cashSubject.getCode() : "1001");
        credit.setSubjectName(cashSubject != null ? cashSubject.getName() : "库存现金");
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(amount);
        credit.setSortOrder(2);
        details.add(credit);

        return details;
    }

    private List<VoucherDetailRequest> buildRentVoucherDetails(Long accountSetId, String summary) {
        List<VoucherDetailRequest> details = new ArrayList<>();

        Long rentExpenseSubjectId = getSubjectIdByCode(accountSetId, "5602.03");
        if (rentExpenseSubjectId == null) {
            rentExpenseSubjectId = getSubjectIdByCodePrefix(accountSetId, "5602");
        }
        Long cashSubjectId = getSubjectIdByCode(accountSetId, "1001");
        if (rentExpenseSubjectId == null || cashSubjectId == null) {
            return details;
        }

        BigDecimal amount = new BigDecimal("3000");

        Subject rentSubject = subjectMapper.selectById(rentExpenseSubjectId);
        Subject cashSubject = subjectMapper.selectById(cashSubjectId);

        VoucherDetailRequest debit = new VoucherDetailRequest();
        debit.setLineNo(1);
        debit.setSummary(summary);
        debit.setSubjectId(rentExpenseSubjectId);
        debit.setSubjectCode(rentSubject != null ? rentSubject.getCode() : "5602");
        debit.setSubjectName(rentSubject != null ? rentSubject.getName() : "管理费用-租金");
        debit.setDebit(amount);
        debit.setCredit(BigDecimal.ZERO);
        debit.setSortOrder(1);
        details.add(debit);

        VoucherDetailRequest credit = new VoucherDetailRequest();
        credit.setLineNo(2);
        credit.setSummary(summary);
        credit.setSubjectId(cashSubjectId);
        credit.setSubjectCode(cashSubject != null ? cashSubject.getCode() : "1001");
        credit.setSubjectName(cashSubject != null ? cashSubject.getName() : "库存现金");
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(amount);
        credit.setSortOrder(2);
        details.add(credit);

        return details;
    }

    private Long getSubjectIdByCode(Long accountSetId, String code) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code)
                .eq(Subject::getStatus, 1);
        Subject subject = subjectMapper.selectOne(wrapper);
        return subject != null ? subject.getId() : null;
    }

    private Long getSubjectIdByCodePrefix(Long accountSetId, String prefix) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
                .likeRight(Subject::getCode, prefix)
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getCode)
                .last("LIMIT 1");
        Subject subject = subjectMapper.selectOne(wrapper);
        return subject != null ? subject.getId() : null;
    }

    private int getLastDayOfMonth(Integer year, Integer month) {
        return java.time.YearMonth.of(year, month).lengthOfMonth();
    }

    private MinimalVoucherBatchResultVO.MinimalVoucherFailItem buildFailItem(Long accountSetId, String name, String reason) {
        MinimalVoucherBatchResultVO.MinimalVoucherFailItem item = new MinimalVoucherBatchResultVO.MinimalVoucherFailItem();
        item.setAccountSetId(accountSetId);
        item.setAccountSetName(name);
        item.setFailReason(reason);
        return item;
    }
}
