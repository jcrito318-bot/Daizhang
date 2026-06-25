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
     * 小微企业优惠判定阈值（季度利润）
     */
    private static final BigDecimal SMALL_ENTERPRISE_THRESHOLD = new BigDecimal("1000000");

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
        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .eq(OutputInvoice::getInvoiceStatus, 0)
                .ge(OutputInvoice::getInvoiceDate, startDate)
                .le(OutputInvoice::getInvoiceDate, endDate);
        List<OutputInvoice> outputInvoices = outputInvoiceMapper.selectList(outputWrapper);
        BigDecimal outputTax = outputInvoices.stream()
                .map(OutputInvoice::getTaxAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outputAmount = outputInvoices.stream()
                .map(OutputInvoice::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 查询进项已认证发票税额合计（认证状态 1）
        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .eq(InputInvoice::getAuthStatus, 1)
                .ge(InputInvoice::getInvoiceDate, startDate)
                .le(InputInvoice::getInvoiceDate, endDate);
        List<InputInvoice> inputInvoices = inputInvoiceMapper.selectList(inputWrapper);
        BigDecimal inputTax = inputInvoices.stream()
                .map(InputInvoice::getTaxAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inputAmount = inputInvoices.stream()
                .map(InputInvoice::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vatAmount = outputTax.subtract(inputTax);

        TaxCalculationResultVO vo = new TaxCalculationResultVO();
        vo.setTaxType("VAT");
        vo.setTaxTypeName("增值税");
        vo.setTaxableAmount(outputTax);
        vo.setTaxRate(VAT_RATE_GENERAL);
        vo.setTaxAmount(vatAmount);
        vo.setCalculationFormula("应纳增值税 = 销项税额 - 进项税额");

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

        vo.setDetails(details);
        return vo;
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

        // 计算季度利润（季度预缴：累计本季度3个月）
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (int m = quarterStartMonth; m <= month; m++) {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, m);
            List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
            Map<Long, AccountBalance> balanceMap = balances.stream()
                    .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

            for (Subject subject : revenueSubjects) {
                AccountBalance balance = balanceMap.get(subject.getId());
                if (balance != null && balance.getPeriodCredit() != null) {
                    totalRevenue = totalRevenue.add(balance.getPeriodCredit());
                }
            }
            for (Subject subject : expenseSubjects) {
                AccountBalance balance = balanceMap.get(subject.getId());
                if (balance != null && balance.getPeriodDebit() != null) {
                    totalExpense = totalExpense.add(balance.getPeriodDebit());
                }
            }
        }

        BigDecimal totalProfit = totalRevenue.subtract(totalExpense);

        // 判定税率：小微优惠（季度利润<=100万按20%）
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
