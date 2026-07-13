package com.company.daizhang.module.tax.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.mapper.SalarySheetMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.tax.service.TaxCalculateService;
import com.company.daizhang.module.tax.vo.TaxCalculationDetailVO;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 税种自动计算服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaxCalculateServiceImpl implements TaxCalculateService {

    private final InputInvoiceMapper inputInvoiceMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final SalarySheetMapper salarySheetMapper;

    /**
     * 税率常量
     */
    private static final BigDecimal VAT_RATE_GENERAL = new BigDecimal("0.13");
    private static final BigDecimal SURCHARGE_CITY = new BigDecimal("0.07");
    private static final BigDecimal SURCHARGE_EDU = new BigDecimal("0.03");
    private static final BigDecimal SURCHARGE_LOCAL_EDU = new BigDecimal("0.02");
    private static final BigDecimal CIT_RATE_NORMAL = new BigDecimal("0.25");
    private static final BigDecimal CIT_RATE_SMALL = new BigDecimal("0.20");
    /**
     * 小微企业优惠判定阈值（年度累计利润，现行政策300万）
     */
    private static final BigDecimal SMALL_ENTERPRISE_THRESHOLD = new BigDecimal("3000000");

    @Override
    public List<TaxCalculationResultVO> calculateAllTaxes(Long accountSetId, Integer year, Integer month) {
        List<TaxCalculationResultVO> results = new ArrayList<>();
        results.add(calculateVAT(accountSetId, year, month));
        results.add(calculateSurchargeTax(accountSetId, year, month));
        results.add(calculateCorporateIncomeTax(accountSetId, year, month));
        results.add(calculatePersonalIncomeTax(accountSetId, year, month));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxCalculationResultVO calculateVAT(Long accountSetId, Integer year, Integer month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        // 查询销项发票税额合计（状态正常 0）
        List<OutputInvoice> outputInvoices = listOutputInvoices(accountSetId, startDate, endDate);
        BigDecimal outputTax = sumOutputTax(outputInvoices);
        BigDecimal outputAmount = outputInvoices.stream()
                .map(OutputInvoice::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 查询进项已认证发票税额合计（认证状态 1）
        List<InputInvoice> inputInvoices = listInputInvoices(accountSetId, startDate, endDate);
        BigDecimal inputTax = sumInputTax(inputInvoices);
        BigDecimal inputAmount = inputInvoices.stream()
                .map(InputInvoice::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 期初留抵税额:从本年1月累计计算截至上月末的留抵。
        // 留抵税额可结转下期继续抵扣,不能直接丢弃(原实现留抵时vatAmount按0处理,留抵永久丢失,
        // 企业无法享受留抵抵扣权益,后续月份多缴增值税)。
        BigDecimal beginningCredit = calculateCreditCarryforward(accountSetId, year, month);

        // 本期可抵扣进项 = 本期认证进项 + 期初留抵
        BigDecimal deductibleInput = inputTax.add(beginningCredit);
        // 应纳增值税 = 销项 - 可抵扣进项;为负表示本期留抵
        BigDecimal vatRaw = outputTax.subtract(deductibleInput);
        // 期末留抵(本期留抵结转下期)
        BigDecimal endingCredit = vatRaw.compareTo(BigDecimal.ZERO) < 0 ? vatRaw.negate() : BigDecimal.ZERO;
        // 应纳增值税(留抵时为0)
        BigDecimal vatAmount = vatRaw.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : vatRaw;

        TaxCalculationResultVO vo = new TaxCalculationResultVO();
        vo.setTaxType("VAT");
        vo.setTaxTypeName("增值税");
        vo.setTaxableAmount(outputTax);
        vo.setTaxRate(VAT_RATE_GENERAL);
        vo.setTaxAmount(vatAmount);
        vo.setCalculationFormula("应纳增值税 = 销项税额 - (进项税额 + 期初留抵)；期末留抵结转下期");

        List<TaxCalculationDetailVO> details = new ArrayList<>();

        TaxCalculationDetailVO outputDetail = new TaxCalculationDetailVO();
        outputDetail.setItemName("销项税额");
        outputDetail.setAmount(outputAmount);
        outputDetail.setRate(VAT_RATE_GENERAL);
        outputDetail.setTaxAmount(outputTax);
        details.add(outputDetail);

        TaxCalculationDetailVO inputDetail = new TaxCalculationDetailVO();
        inputDetail.setItemName("进项税额(已认证)");
        inputDetail.setAmount(inputAmount);
        inputDetail.setRate(VAT_RATE_GENERAL);
        inputDetail.setTaxAmount(inputTax);
        details.add(inputDetail);

        TaxCalculationDetailVO creditDetail = new TaxCalculationDetailVO();
        creditDetail.setItemName("期初留抵税额");
        creditDetail.setAmount(beginningCredit);
        creditDetail.setRate(null);
        creditDetail.setTaxAmount(beginningCredit);
        details.add(creditDetail);

        TaxCalculationDetailVO endingCreditDetail = new TaxCalculationDetailVO();
        endingCreditDetail.setItemName("期末留抵税额(结转下期)");
        endingCreditDetail.setAmount(endingCredit);
        endingCreditDetail.setRate(null);
        endingCreditDetail.setTaxAmount(endingCredit);
        details.add(endingCreditDetail);

        vo.setDetails(details);
        return vo;
    }

    /**
     * 查询销项发票(状态正常0,日期范围内)
     */
    private List<OutputInvoice> listOutputInvoices(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OutputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .eq(OutputInvoice::getInvoiceStatus, 0)
                .ge(OutputInvoice::getInvoiceDate, startDate)
                .le(OutputInvoice::getInvoiceDate, endDate);
        return outputInvoiceMapper.selectList(wrapper);
    }

    /**
     * 查询进项已认证发票(认证状态1,日期范围内)
     */
    private List<InputInvoice> listInputInvoices(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<InputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .eq(InputInvoice::getAuthStatus, 1)
                .ge(InputInvoice::getInvoiceDate, startDate)
                .le(InputInvoice::getInvoiceDate, endDate);
        return inputInvoiceMapper.selectList(wrapper);
    }

    private BigDecimal sumOutputTax(List<OutputInvoice> invoices) {
        return invoices.stream()
                .map(OutputInvoice::getTaxAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumInputTax(List<InputInvoice> invoices) {
        return invoices.stream()
                .map(InputInvoice::getTaxAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算截至指定月份上月末的期初留抵税额。
     * 从本年1月累计:credit(end of m) = max(0, credit(end of m-1) + inputTax_m - outputTax_m)。
     * 留抵可结转下期抵扣,本方法保证历史留抵不丢失。
     */
    private BigDecimal calculateCreditCarryforward(Long accountSetId, Integer year, Integer month) {
        BigDecimal credit = BigDecimal.ZERO;
        for (int m = 1; m < month; m++) {
            LocalDate s = LocalDate.of(year, m, 1);
            LocalDate e = YearMonth.of(year, m).atEndOfMonth();
            BigDecimal outTax = sumOutputTax(listOutputInvoices(accountSetId, s, e));
            BigDecimal inTax = sumInputTax(listInputInvoices(accountSetId, s, e));
            BigDecimal net = credit.add(inTax).subtract(outTax);
            credit = net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
        }
        return credit;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxCalculationResultVO calculateSurchargeTax(Long accountSetId, Integer year, Integer month) {
        // 先计算增值税
        TaxCalculationResultVO vatResult = calculateVAT(accountSetId, year, month);
        BigDecimal vatAmount = vatResult.getTaxAmount();
        // 附加税基于实际应纳增值税（留抵时按0计算）
        BigDecimal baseVat = vatAmount.compareTo(BigDecimal.ZERO) > 0 ? vatAmount : BigDecimal.ZERO;

        BigDecimal cityTax = baseVat.multiply(SURCHARGE_CITY).setScale(2, RoundingMode.HALF_UP);
        BigDecimal eduSurcharge = baseVat.multiply(SURCHARGE_EDU).setScale(2, RoundingMode.HALF_UP);
        BigDecimal localEduSurcharge = baseVat.multiply(SURCHARGE_LOCAL_EDU).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSurcharge = cityTax.add(eduSurcharge).add(localEduSurcharge);

        TaxCalculationResultVO vo = new TaxCalculationResultVO();
        vo.setTaxType("SURTAX");
        vo.setTaxTypeName("附加税");
        vo.setTaxableAmount(baseVat);
        vo.setTaxRate(SURCHARGE_CITY.add(SURCHARGE_EDU).add(SURCHARGE_LOCAL_EDU));
        vo.setTaxAmount(totalSurcharge);
        vo.setCalculationFormula("附加税 = 应纳增值税 × (城建税7% + 教育附加3% + 地方教育附加2%)");

        List<TaxCalculationDetailVO> details = new ArrayList<>();

        TaxCalculationDetailVO cityDetail = new TaxCalculationDetailVO();
        cityDetail.setItemName("城市维护建设税");
        cityDetail.setAmount(baseVat);
        cityDetail.setRate(SURCHARGE_CITY);
        cityDetail.setTaxAmount(cityTax);
        details.add(cityDetail);

        TaxCalculationDetailVO eduDetail = new TaxCalculationDetailVO();
        eduDetail.setItemName("教育费附加");
        eduDetail.setAmount(baseVat);
        eduDetail.setRate(SURCHARGE_EDU);
        eduDetail.setTaxAmount(eduSurcharge);
        details.add(eduDetail);

        TaxCalculationDetailVO localEduDetail = new TaxCalculationDetailVO();
        localEduDetail.setItemName("地方教育附加");
        localEduDetail.setAmount(baseVat);
        localEduDetail.setRate(SURCHARGE_LOCAL_EDU);
        localEduDetail.setTaxAmount(localEduSurcharge);
        details.add(localEduDetail);

        vo.setDetails(details);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxCalculationResultVO calculateCorporateIncomeTax(Long accountSetId, Integer year, Integer month) {
        // 查询损益类科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCategory, "损益")
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 收入类科目（贷方余额方向 balanceDirection=2）
        List<Subject> revenueSubjects = subjects.stream()
                .filter(s -> s.getBalanceDirection() != null && s.getBalanceDirection() == 2)
                .collect(Collectors.toList());
        // 成本费用类科目（借方余额方向 balanceDirection=1）
        List<Subject> expenseSubjects = subjects.stream()
                .filter(s -> s.getBalanceDirection() != null && s.getBalanceDirection() == 1)
                .collect(Collectors.toList());

        // 计算年度累计利润（季度预缴按年度累计预缴，累计1~month各月）
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (int m = 1; m <= month; m++) {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, m);
            List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
            Map<Long, AccountBalance> balanceMap = balances.stream()
                    .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

            for (Subject subject : revenueSubjects) {
                AccountBalance balance = balanceMap.get(subject.getId());
                if (balance != null) {
                    // 收入类按净额:贷方-借方(销售退回等借方冲减),与利润表口径一致
                    BigDecimal credit = balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO;
                    BigDecimal debit = balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO;
                    totalRevenue = totalRevenue.add(credit.subtract(debit));
                }
            }
            for (Subject subject : expenseSubjects) {
                AccountBalance balance = balanceMap.get(subject.getId());
                if (balance != null) {
                    // 费用类按净额:借方-贷方(红字冲销/转回等贷方),与利润表口径一致
                    BigDecimal debit = balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO;
                    BigDecimal credit = balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO;
                    totalExpense = totalExpense.add(debit.subtract(credit));
                }
            }
        }

        BigDecimal totalProfit = totalRevenue.subtract(totalExpense);

        // 判定税率：小微优惠（年度累计利润<=300万按20%）
        BigDecimal taxRate = totalProfit.compareTo(SMALL_ENTERPRISE_THRESHOLD) <= 0
                ? CIT_RATE_SMALL : CIT_RATE_NORMAL;
        // 仅当利润为正时计税
        BigDecimal taxableProfit = totalProfit.compareTo(BigDecimal.ZERO) > 0 ? totalProfit : BigDecimal.ZERO;
        BigDecimal citAmount = taxableProfit.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        TaxCalculationResultVO vo = new TaxCalculationResultVO();
        vo.setTaxType("CIT");
        vo.setTaxTypeName("企业所得税");
        vo.setTaxableAmount(taxableProfit);
        vo.setTaxRate(taxRate);
        vo.setTaxAmount(citAmount);
        vo.setCalculationFormula("企业所得税(季度预缴) = 利润总额 × 税率(25%/小微20%)");

        List<TaxCalculationDetailVO> details = new ArrayList<>();

        TaxCalculationDetailVO revenueDetail = new TaxCalculationDetailVO();
        revenueDetail.setItemName("收入类科目合计");
        revenueDetail.setAmount(totalRevenue);
        revenueDetail.setRate(null);
        revenueDetail.setTaxAmount(null);
        details.add(revenueDetail);

        TaxCalculationDetailVO expenseDetail = new TaxCalculationDetailVO();
        expenseDetail.setItemName("成本费用类科目合计");
        expenseDetail.setAmount(totalExpense);
        expenseDetail.setRate(null);
        expenseDetail.setTaxAmount(null);
        details.add(expenseDetail);

        TaxCalculationDetailVO profitDetail = new TaxCalculationDetailVO();
        profitDetail.setItemName("利润总额");
        profitDetail.setAmount(totalProfit);
        profitDetail.setRate(taxRate);
        profitDetail.setTaxAmount(citAmount);
        details.add(profitDetail);

        vo.setDetails(details);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public TaxCalculationResultVO calculatePersonalIncomeTax(Long accountSetId, Integer year, Integer month) {
        // 查询薪资表个税合计
        LambdaQueryWrapper<SalarySheet> salaryWrapper = new LambdaQueryWrapper<>();
        salaryWrapper.eq(SalarySheet::getAccountSetId, accountSetId)
                .eq(SalarySheet::getYear, year)
                .eq(SalarySheet::getMonth, month);
        List<SalarySheet> salarySheets = salarySheetMapper.selectList(salaryWrapper);

        BigDecimal totalIncomeTax = salarySheets.stream()
                .map(SalarySheet::getIncomeTax)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTaxableIncome = salarySheets.stream()
                .map(SalarySheet::getTaxableIncome)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TaxCalculationResultVO vo = new TaxCalculationResultVO();
        vo.setTaxType("PIT");
        vo.setTaxTypeName("个人所得税");
        vo.setTaxableAmount(totalTaxableIncome);
        vo.setTaxRate(null);
        vo.setTaxAmount(totalIncomeTax);
        vo.setCalculationFormula("个人所得税 = 工资薪金应纳税所得额 × 适用税率 - 速算扣除数（按薪资表个税合计）");

        List<TaxCalculationDetailVO> details = new ArrayList<>();

        TaxCalculationDetailVO salaryDetail = new TaxCalculationDetailVO();
        salaryDetail.setItemName("工资薪金应纳税所得额合计");
        salaryDetail.setAmount(totalTaxableIncome);
        salaryDetail.setRate(null);
        salaryDetail.setTaxAmount(totalIncomeTax);
        details.add(salaryDetail);

        TaxCalculationDetailVO countDetail = new TaxCalculationDetailVO();
        countDetail.setItemName("纳税人数");
        countDetail.setAmount(new BigDecimal(salarySheets.size()));
        countDetail.setRate(null);
        countDetail.setTaxAmount(null);
        details.add(countDetail);

        vo.setDetails(details);
        return vo;
    }
}
