package com.company.daizhang.module.tax.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.bank.entity.BankReconciliation;
import com.company.daizhang.module.bank.mapper.BankReconciliationMapper;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.mapper.EmployeeMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.tax.service.ComplianceReportService;
import com.company.daizhang.module.tax.vo.ComplianceDimensionVO;
import com.company.daizhang.module.tax.vo.ComplianceIndicatorVO;
import com.company.daizhang.module.tax.vo.ComplianceIssueVO;
import com.company.daizhang.module.tax.vo.ComplianceReportVO;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceReportServiceImpl implements ComplianceReportService {

    private final AccountSetMapper accountSetMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final CustomerMapper customerMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final InputInvoiceMapper inputInvoiceMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;
    private final EmployeeMapper employeeMapper;
    private final BankReconciliationMapper bankReconciliationMapper;
    private final VoucherService voucherService;

    @Override
    @Transactional(readOnly = true)
    public ComplianceReportVO generateComplianceReport(Long accountSetId, Integer year, Integer month) {
        ComplianceReportVO report = new ComplianceReportVO();
        report.setAccountSetId(accountSetId);
        report.setYear(year);
        report.setMonth(month);
        report.setGenerateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            report.setOverallScore(BigDecimal.ZERO);
            report.setOverallRiskLevel("数据不足");
            report.setTotalIssueCount(0);
            report.setDimensions(new ArrayList<>());
            report.setIssues(new ArrayList<>());
            return report;
        }

        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Customer::getAccountSetId, accountSetId);
        Customer customer = customerMapper.selectOne(customerWrapper);
        report.setCustomerName(customer != null ? customer.getCustomerName() : accountSet.getName());

        List<ComplianceDimensionVO> dimensions = new ArrayList<>();
        List<ComplianceIssueVO> allIssues = new ArrayList<>();

        dimensions.add(evaluateFinanceDimension(accountSetId, year, month, allIssues));
        dimensions.add(evaluateTaxDimension(accountSetId, year, month, allIssues));
        dimensions.add(evaluateInvoiceDimension(accountSetId, year, month, allIssues));
        dimensions.add(evaluateBusinessDimension(accountSetId, year, month, allIssues));
        dimensions.add(evaluateOtherDimension(accountSetId, year, month, allIssues));

        report.setDimensions(dimensions);
        report.setIssues(allIssues);

        int highRisk = 0, mediumRisk = 0, lowRisk = 0;
        for (ComplianceIssueVO issue : allIssues) {
            if (issue.getRiskLevel() != null) {
                if (issue.getRiskLevel() == 1) highRisk++;
                else if (issue.getRiskLevel() == 2) mediumRisk++;
                else lowRisk++;
            }
        }
        report.setTotalIssueCount(allIssues.size());
        report.setHighRiskCount(highRisk);
        report.setMediumRiskCount(mediumRisk);
        report.setLowRiskCount(lowRisk);

        BigDecimal totalScore = BigDecimal.ZERO;
        for (ComplianceDimensionVO dim : dimensions) {
            totalScore = totalScore.add(dim.getScore());
        }
        BigDecimal avgScore = totalScore.divide(new BigDecimal(dimensions.size()), 2, RoundingMode.HALF_UP);
        report.setOverallScore(avgScore);

        if (highRisk > 0 || avgScore.compareTo(new BigDecimal("60")) < 0) {
            report.setOverallRiskLevel("高风险");
        } else if (mediumRisk > 2 || avgScore.compareTo(new BigDecimal("80")) < 0) {
            report.setOverallRiskLevel("中风险");
        } else {
            report.setOverallRiskLevel("低风险");
        }

        report.setOverallSuggestion(buildOverallSuggestion(highRisk, mediumRisk, lowRisk, avgScore));

        return report;
    }

    private ComplianceDimensionVO evaluateFinanceDimension(Long accountSetId, Integer year, Integer month,
                                                            List<ComplianceIssueVO> issues) {
        ComplianceDimensionVO dim = new ComplianceDimensionVO();
        dim.setDimensionCode("FINANCE");
        dim.setDimensionName("财务合规");
        List<ComplianceIndicatorVO> indicators = new ArrayList<>();
        int dimIssueCount = 0;
        int dimHigh = 0, dimMedium = 0, dimLow = 0;

        BigDecimal totalAssets = getCategoryBalance(accountSetId, year, month, "资产");
        BigDecimal totalLiabilities = getCategoryBalance(accountSetId, year, month, "负债");
        BigDecimal debtRatio = BigDecimal.ZERO;
        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
            debtRatio = totalLiabilities.divide(totalAssets, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String debtRatioStatus = debtRatio.compareTo(new BigDecimal("70")) > 0 ? "ABNORMAL"
                : (debtRatio.compareTo(new BigDecimal("50")) > 0 ? "WARNING" : "NORMAL");
        indicators.add(buildIndicator("DEBT_RATIO", "资产负债率", debtRatio, "%",
                "≤50%正常, 50-70%预警, >70%异常", debtRatioStatus, "衡量企业长期偿债能力"));
        if ("ABNORMAL".equals(debtRatioStatus)) {
            issues.add(buildIssue("FINANCE", "HIGH_DEBT_RATIO", "资产负债率过高", 2,
                    "资产负债率达" + debtRatio.setScale(2, RoundingMode.HALF_UP) + "%，超过70%警戒线",
                    "可能导致融资困难、财务风险增大", "优化资本结构，降低负债比例", null));
            dimIssueCount++;
            dimMedium++;
        }

        BigDecimal currentAssets = getSubjectsBalanceByPrefix(accountSetId, year, month, "资产",
                "10", "11", "12", "1401", "1403", "1411", "1471", "1511", "1531", "1532");
        BigDecimal currentLiabilities = getSubjectsBalanceByPrefix(accountSetId, year, month, "负债",
                "20", "21", "22", "23", "24", "2501");
        BigDecimal currentRatio = BigDecimal.ZERO;
        if (currentLiabilities.compareTo(BigDecimal.ZERO) > 0) {
            currentRatio = currentAssets.divide(currentLiabilities, 2, RoundingMode.HALF_UP);
        }
        String currentRatioStatus = currentRatio.compareTo(new BigDecimal("1")) < 0 ? "ABNORMAL"
                : (currentRatio.compareTo(new BigDecimal("2")) < 0 ? "WARNING" : "NORMAL");
        indicators.add(buildIndicator("CURRENT_RATIO", "流动比率", currentRatio, "倍",
                "≥2正常, 1-2预警, <1异常", currentRatioStatus, "衡量企业短期偿债能力"));
        if ("ABNORMAL".equals(currentRatioStatus)) {
            issues.add(buildIssue("FINANCE", "LOW_CURRENT_RATIO", "流动比率过低", 1,
                    "流动比率仅" + currentRatio + "，低于1倍警戒线",
                    "短期偿债能力不足，可能面临资金链断裂风险",
                    "增加流动资金，优化短期债务结构", currentLiabilities));
            dimIssueCount++;
            dimHigh++;
        }

        BigDecimal revenue = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 2, "50");
        BigDecimal cost = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 1, "54");
        BigDecimal expense = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 1, "56");
        BigDecimal profit = revenue.subtract(cost).subtract(expense);
        String profitStatus = profit.compareTo(BigDecimal.ZERO) < 0 ? "ABNORMAL" : "NORMAL";
        indicators.add(buildIndicator("NET_PROFIT", "净利润", profit, "元", "≥0正常", profitStatus,
                "当期净利润情况"));
        if ("ABNORMAL".equals(profitStatus)) {
            issues.add(buildIssue("FINANCE", "LOSS_STATUS", "经营亏损", 2,
                    "当期亏损" + profit.abs().setScale(2, RoundingMode.HALF_UP) + "元",
                    "长期亏损可能导致企业经营困难", "分析亏损原因，制定扭亏方案", profit.abs()));
            dimIssueCount++;
            dimMedium++;
        }

        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getDraftStatus, 0);
        long voucherCount = voucherService.count(voucherWrapper);
        String voucherStatus = voucherCount == 0 ? "ABNORMAL" : (voucherCount < 3 ? "WARNING" : "NORMAL");
        indicators.add(buildIndicator("VOUCHER_COUNT", "当期凭证数", new BigDecimal(voucherCount), "张",
                "≥5正常, 3-5预警, <3异常", voucherStatus, "衡量记账完整性"));
        if ("ABNORMAL".equals(voucherStatus)) {
            issues.add(buildIssue("FINANCE", "NO_VOUCHER", "当期无记账凭证", 1,
                    year + "年" + month + "月无任何记账凭证",
                    "账务处理不完整，影响税务申报准确性", "及时补充当期凭证", null));
            dimIssueCount++;
            dimHigh++;
        }

        dim.setIndicators(indicators);
        dim.setIssueCount(dimIssueCount);
        dim.setHighRiskCount(dimHigh);
        dim.setMediumRiskCount(dimMedium);
        dim.setLowRiskCount(dimLow);
        dim.setScore(calculateDimensionScore(dimHigh, dimMedium, dimLow));

        return dim;
    }

    private ComplianceDimensionVO evaluateTaxDimension(Long accountSetId, Integer year, Integer month,
                                                        List<ComplianceIssueVO> issues) {
        ComplianceDimensionVO dim = new ComplianceDimensionVO();
        dim.setDimensionCode("TAX");
        dim.setDimensionName("税务合规");
        List<ComplianceIndicatorVO> indicators = new ArrayList<>();
        int dimIssueCount = 0;
        int dimHigh = 0, dimMedium = 0, dimLow = 0;

        String[] taxTypes = {"VAT", "Surcharge", "IncomeTax", "PersonalTax", "StampTax"};
        int declaredCount = 0;
        int overdueCount = 0;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (String taxType : taxTypes) {
            LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaxDeclaration::getAccountSetId, accountSetId)
                    .eq(TaxDeclaration::getYear, year)
                    .eq(TaxDeclaration::getMonth, month)
                    .eq(TaxDeclaration::getTaxType, taxType);
            TaxDeclaration decl = taxDeclarationMapper.selectOne(wrapper);
            if (decl != null) {
                declaredCount++;
                if (decl.getTaxAmount() != null) {
                    totalTax = totalTax.add(decl.getTaxAmount());
                }
                if (decl.getStatus() != null && decl.getStatus() == 0) {
                    overdueCount++;
                }
            }
        }

        BigDecimal declarationRate = new BigDecimal(declaredCount)
                .divide(new BigDecimal(taxTypes.length), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        String declStatus = declarationRate.compareTo(new BigDecimal("100")) < 0 ? "ABNORMAL" : "NORMAL";
        indicators.add(buildIndicator("DECLARATION_RATE", "税种申报率", declarationRate, "%",
                "100%正常", declStatus, "已申报税种占应申报税种比例"));
        if ("ABNORMAL".equals(declStatus)) {
            issues.add(buildIssue("TAX", "INCOMPLETE_DECLARATION", "税种申报不完整", 1,
                    "当期申报率仅" + declarationRate.setScale(1, RoundingMode.HALF_UP) + "%",
                    "漏报税种可能面临税务处罚和滞纳金", "及时补报未申报税种", null));
            dimIssueCount++;
            dimHigh++;
        }

        BigDecimal revenue = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 2, "50");
        BigDecimal taxBurdenRate = BigDecimal.ZERO;
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            taxBurdenRate = totalTax.divide(revenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String burdenStatus = taxBurdenRate.compareTo(BigDecimal.ZERO) == 0
                && revenue.compareTo(BigDecimal.ZERO) > 0 ? "WARNING" : "NORMAL";
        indicators.add(buildIndicator("TAX_BURDEN_RATE", "综合税负率", taxBurdenRate, "%",
                "参考行业标准", burdenStatus, "综合税负率=纳税总额/营业收入"));

        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        List<OutputInvoice> outputs = outputInvoiceMapper.selectList(outputWrapper);
        BigDecimal outputTax = outputs.stream()
                .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        List<InputInvoice> inputs = inputInvoiceMapper.selectList(inputWrapper);
        BigDecimal inputTax = inputs.stream()
                .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inputOutputRatio = BigDecimal.ZERO;
        if (outputTax.compareTo(BigDecimal.ZERO) > 0) {
            inputOutputRatio = inputTax.divide(outputTax, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String ioStatus = inputOutputRatio.compareTo(new BigDecimal("90")) > 0
                && inputOutputRatio.compareTo(new BigDecimal("110")) < 0 ? "WARNING" : "NORMAL";
        indicators.add(buildIndicator("INPUT_OUTPUT_RATIO", "进销项税额比", inputOutputRatio, "%",
                "偏离过大预警", ioStatus, "进项税额/销项税额比例"));

        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getAccountSetId, accountSetId)
                .eq(Employee::getStatus, 1);
        Long empCount = employeeMapper.selectCount(empWrapper);
        indicators.add(buildIndicator("EMPLOYEE_COUNT", "员工人数",
                new BigDecimal(empCount != null ? empCount : 0), "人", "-", "NORMAL",
                "企业在职员工数量"));

        dim.setIndicators(indicators);
        dim.setIssueCount(dimIssueCount);
        dim.setHighRiskCount(dimHigh);
        dim.setMediumRiskCount(dimMedium);
        dim.setLowRiskCount(dimLow);
        dim.setScore(calculateDimensionScore(dimHigh, dimMedium, dimLow));

        return dim;
    }

    private ComplianceDimensionVO evaluateInvoiceDimension(Long accountSetId, Integer year, Integer month,
                                                            List<ComplianceIssueVO> issues) {
        ComplianceDimensionVO dim = new ComplianceDimensionVO();
        dim.setDimensionCode("INVOICE");
        dim.setDimensionName("发票合规");
        List<ComplianceIndicatorVO> indicators = new ArrayList<>();
        int dimIssueCount = 0;
        int dimHigh = 0, dimMedium = 0, dimLow = 0;

        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        Long outputCount = outputInvoiceMapper.selectCount(outputWrapper);
        indicators.add(buildIndicator("OUTPUT_INVOICE_COUNT", "销项发票数",
                new BigDecimal(outputCount != null ? outputCount : 0), "张", "-", "NORMAL",
                "当期开具的销项发票数量"));

        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month);
        Long inputCount = inputInvoiceMapper.selectCount(inputWrapper);
        indicators.add(buildIndicator("INPUT_INVOICE_COUNT", "进项发票数",
                new BigDecimal(inputCount != null ? inputCount : 0), "张", "-", "NORMAL",
                "当期取得的进项发票数量"));

        LambdaQueryWrapper<OutputInvoice> largeOutputWrapper = new LambdaQueryWrapper<>();
        largeOutputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month)
                .gt(OutputInvoice::getTotalAmount, new BigDecimal("100000"));
        Long largeCount = outputInvoiceMapper.selectCount(largeOutputWrapper);
        String largeStatus = (largeCount != null && largeCount > 5) ? "WARNING" : "NORMAL";
        indicators.add(buildIndicator("LARGE_INVOICE_COUNT", "大额发票(>10万)数",
                new BigDecimal(largeCount != null ? largeCount : 0), "张", ">5张预警", largeStatus,
                "单张含税金额大于10万的发票数量"));
        if ("WARNING".equals(largeStatus)) {
            issues.add(buildIssue("INVOICE", "MANY_LARGE_INVOICES", "大额发票数量较多", 2,
                    "当期大额发票(" + largeCount + "张)数量较多",
                    "大额发票是税务稽查重点关注对象",
                    "核实大额发票业务真实性，留存完整业务资料", null));
            dimIssueCount++;
            dimMedium++;
        }

        LambdaQueryWrapper<InputInvoice> unauthWrapper = new LambdaQueryWrapper<>();
        unauthWrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .apply("YEAR(invoice_date) = {0} AND MONTH(invoice_date) = {1}", year, month)
                .ne(InputInvoice::getAuthStatus, 1);
        Long unauthCount = inputInvoiceMapper.selectCount(unauthWrapper);
        BigDecimal authRate = BigDecimal.ZERO;
        if (inputCount != null && inputCount > 0) {
            authRate = new BigDecimal(inputCount - (unauthCount != null ? unauthCount : 0))
                    .divide(new BigDecimal(inputCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String authStatus = authRate.compareTo(new BigDecimal("95")) < 0
                && inputCount != null && inputCount > 0 ? "WARNING" : "NORMAL";
        indicators.add(buildIndicator("AUTH_RATE", "进项发票认证率", authRate, "%", "≥95%正常",
                authStatus, "已认证进项发票占全部进项发票比例"));
        if ("WARNING".equals(authStatus)) {
            issues.add(buildIssue("INVOICE", "LOW_AUTH_RATE", "进项发票认证率偏低", 3,
                    "当期进项发票认证率仅" + authRate.setScale(1, RoundingMode.HALF_UP) + "%",
                    "未及时认证发票可能导致当期多缴税款", "及时完成进项发票认证抵扣", null));
            dimIssueCount++;
            dimLow++;
        }

        dim.setIndicators(indicators);
        dim.setIssueCount(dimIssueCount);
        dim.setHighRiskCount(dimHigh);
        dim.setMediumRiskCount(dimMedium);
        dim.setLowRiskCount(dimLow);
        dim.setScore(calculateDimensionScore(dimHigh, dimMedium, dimLow));

        return dim;
    }

    private ComplianceDimensionVO evaluateBusinessDimension(Long accountSetId, Integer year, Integer month,
                                                             List<ComplianceIssueVO> issues) {
        ComplianceDimensionVO dim = new ComplianceDimensionVO();
        dim.setDimensionCode("BUSINESS");
        dim.setDimensionName("经营合规");
        List<ComplianceIndicatorVO> indicators = new ArrayList<>();
        int dimIssueCount = 0;
        int dimHigh = 0, dimMedium = 0, dimLow = 0;

        BigDecimal currentRevenue = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 2, "50");
        BigDecimal lastYearRevenue = getPeriodAmountByPrefix(accountSetId, year - 1, month, "损益", 2, "50");
        BigDecimal revenueGrowth = BigDecimal.ZERO;
        if (lastYearRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueGrowth = currentRevenue.subtract(lastYearRevenue)
                    .divide(lastYearRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String growthStatus = revenueGrowth.compareTo(new BigDecimal("-30")) < 0 ? "ABNORMAL"
                : (revenueGrowth.compareTo(new BigDecimal("50")) > 0 ? "WARNING" : "NORMAL");
        indicators.add(buildIndicator("REVENUE_GROWTH", "营业收入同比增长率", revenueGrowth, "%",
                "-30%~50%正常", growthStatus, "营业收入同比变化率"));
        if ("ABNORMAL".equals(growthStatus)) {
            issues.add(buildIssue("BUSINESS", "SHARP_REVENUE_DROP", "营业收入大幅下降", 2,
                    "营业收入同比下降" + revenueGrowth.abs().setScale(1, RoundingMode.HALF_UP) + "%",
                    "营收大幅下降可能引起税务机关关注",
                    "分析收入下降原因，确保数据真实准确", currentRevenue));
            dimIssueCount++;
            dimMedium++;
        }
        if ("WARNING".equals(growthStatus)) {
            issues.add(buildIssue("BUSINESS", "RAPID_REVENUE_GROWTH", "营业收入快速增长", 3,
                    "营业收入同比增长" + revenueGrowth.setScale(1, RoundingMode.HALF_UP) + "%",
                    "营收快速增长需注意税务风险",
                    "核实业务真实性，确保纳税合规", currentRevenue));
            dimIssueCount++;
            dimLow++;
        }

        BigDecimal costOfGoods = getPeriodAmountByPrefix(accountSetId, year, month, "损益", 1, "5401", "5402");
        BigDecimal grossProfitRate = BigDecimal.ZERO;
        if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            grossProfitRate = currentRevenue.subtract(costOfGoods)
                    .divide(currentRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        String gpStatus = grossProfitRate.compareTo(BigDecimal.ZERO) < 0 ? "ABNORMAL" : "NORMAL";
        indicators.add(buildIndicator("GROSS_PROFIT_RATE", "毛利率", grossProfitRate, "%",
                ">0正常", gpStatus, "毛利率=(营业收入-营业成本)/营业收入"));
        if ("ABNORMAL".equals(gpStatus)) {
            issues.add(buildIssue("BUSINESS", "NEGATIVE_GROSS_PROFIT", "毛利率为负", 1,
                    "当期毛利率为" + grossProfitRate.setScale(2, RoundingMode.HALF_UP) + "%",
                    "负毛利可能涉及成本收入不匹配问题",
                    "检查成本结转是否正确，核实业务合理性", null));
            dimIssueCount++;
            dimHigh++;
        }

        BigDecimal receivable = getSubjectsBalanceByPrefix(accountSetId, year, month, "资产", "1122");
        BigDecimal arTurnover = BigDecimal.ZERO;
        if (receivable.compareTo(BigDecimal.ZERO) > 0 && currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            arTurnover = currentRevenue.divide(receivable, 2, RoundingMode.HALF_UP);
        }
        indicators.add(buildIndicator("AR_TURNOVER", "应收账款周转率", arTurnover, "次",
                "参考行业", "NORMAL", "衡量应收账款回收速度"));

        dim.setIndicators(indicators);
        dim.setIssueCount(dimIssueCount);
        dim.setHighRiskCount(dimHigh);
        dim.setMediumRiskCount(dimMedium);
        dim.setLowRiskCount(dimLow);
        dim.setScore(calculateDimensionScore(dimHigh, dimMedium, dimLow));

        return dim;
    }

    private ComplianceDimensionVO evaluateOtherDimension(Long accountSetId, Integer year, Integer month,
                                                          List<ComplianceIssueVO> issues) {
        ComplianceDimensionVO dim = new ComplianceDimensionVO();
        dim.setDimensionCode("OTHER");
        dim.setDimensionName("其他合规");
        List<ComplianceIndicatorVO> indicators = new ArrayList<>();
        int dimIssueCount = 0;
        int dimHigh = 0, dimMedium = 0, dimLow = 0;

        LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                .eq(AccountPeriod::getYear, year)
                .eq(AccountPeriod::getMonth, month);
        AccountPeriod period = accountPeriodMapper.selectOne(periodWrapper);
        String closeStatus = period != null && period.getStatus() != null
                && period.getStatus() == 1 ? "NORMAL" : "WARNING";
        indicators.add(buildIndicator("PERIOD_CLOSE", "期间结账状态", BigDecimal.ZERO, "-",
                "已结账正常", closeStatus, "当期会计期间是否已结账"));
        if ("WARNING".equals(closeStatus)) {
            issues.add(buildIssue("OTHER", "PERIOD_NOT_CLOSED", "会计期间未结账", 3,
                    year + "年" + month + "月会计期间尚未结账",
                    "未结账期间数据可能不完整", "及时完成期间结账", null));
            dimIssueCount++;
            dimLow++;
        }

        LambdaQueryWrapper<BankReconciliation> recWrapper = new LambdaQueryWrapper<>();
        recWrapper.eq(BankReconciliation::getAccountSetId, accountSetId)
                .eq(BankReconciliation::getYear, year)
                .eq(BankReconciliation::getMonth, month);
        Long recCount = bankReconciliationMapper.selectCount(recWrapper);
        String recStatus = (recCount != null && recCount > 0) ? "NORMAL" : "WARNING";
        indicators.add(buildIndicator("BANK_RECONCILIATION", "银行对账完成",
                new BigDecimal(recCount != null ? recCount : 0), "份", ">0正常", recStatus,
                "当期银行对账完成情况"));
        if ("WARNING".equals(recStatus)) {
            issues.add(buildIssue("OTHER", "NO_BANK_REC", "当期银行未对账", 2,
                    year + "年" + month + "月未进行银行对账",
                    "长期不对账可能导致资金账实不符", "及时完成银行存款对账", null));
            dimIssueCount++;
            dimMedium++;
        }

        dim.setIndicators(indicators);
        dim.setIssueCount(dimIssueCount);
        dim.setHighRiskCount(dimHigh);
        dim.setMediumRiskCount(dimMedium);
        dim.setLowRiskCount(dimLow);
        dim.setScore(calculateDimensionScore(dimHigh, dimMedium, dimLow));

        return dim;
    }

    private BigDecimal getCategoryBalance(Long accountSetId, Integer year, Integer month, String category) {
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCategory, category)
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        BigDecimal total = BigDecimal.ZERO;
        for (Subject subject : subjects) {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, month)
                    .eq(AccountBalance::getSubjectId, subject.getId());
            AccountBalance balance = accountBalanceMapper.selectOne(balanceWrapper);
            if (balance != null) {
                BigDecimal ending = balance.getEndDebit() != null
                        ? balance.getEndDebit() : BigDecimal.ZERO;
                if (balance.getEndCredit() != null
                        && balance.getEndCredit().compareTo(BigDecimal.ZERO) > 0) {
                    ending = balance.getEndCredit();
                }
                total = total.add(ending);
            }
        }
        return total;
    }

    private BigDecimal getSubjectsBalanceByPrefix(Long accountSetId, Integer year, Integer month,
                                                   String category, String... prefixes) {
        BigDecimal total = BigDecimal.ZERO;
        for (String prefix : prefixes) {
            LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
            subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                    .likeRight(Subject::getCode, prefix)
                    .eq(Subject::getStatus, 1);
            if (category != null) {
                subjectWrapper.eq(Subject::getCategory, category);
            }
            List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

            for (Subject subject : subjects) {
                LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
                balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                        .eq(AccountBalance::getYear, year)
                        .eq(AccountBalance::getMonth, month)
                        .eq(AccountBalance::getSubjectId, subject.getId());
                AccountBalance balance = accountBalanceMapper.selectOne(balanceWrapper);
                if (balance != null) {
                    BigDecimal ending = balance.getEndDebit() != null
                            ? balance.getEndDebit() : BigDecimal.ZERO;
                    if (balance.getEndCredit() != null
                            && balance.getEndCredit().compareTo(BigDecimal.ZERO) > 0) {
                        ending = balance.getEndCredit();
                    }
                    total = total.add(ending);
                }
            }
        }
        return total;
    }

    private BigDecimal getPeriodAmountByPrefix(Long accountSetId, Integer year, Integer month,
                                                String category, Integer direction, String... prefixes) {
        BigDecimal total = BigDecimal.ZERO;
        for (String prefix : prefixes) {
            LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
            subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                    .likeRight(Subject::getCode, prefix)
                    .eq(Subject::getStatus, 1);
            if (category != null) {
                subjectWrapper.eq(Subject::getCategory, category);
            }
            List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

            for (Subject subject : subjects) {
                LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
                balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                        .eq(AccountBalance::getYear, year)
                        .eq(AccountBalance::getMonth, month)
                        .eq(AccountBalance::getSubjectId, subject.getId());
                AccountBalance balance = accountBalanceMapper.selectOne(balanceWrapper);
                if (balance != null) {
                    if (direction == 1) {
                        total = total.add(balance.getPeriodDebit() != null
                                ? balance.getPeriodDebit() : BigDecimal.ZERO);
                    } else {
                        total = total.add(balance.getPeriodCredit() != null
                                ? balance.getPeriodCredit() : BigDecimal.ZERO);
                    }
                }
            }
        }
        return total;
    }

    private ComplianceIndicatorVO buildIndicator(String code, String name, BigDecimal value, String unit,
                                                  String reference, String status, String description) {
        ComplianceIndicatorVO vo = new ComplianceIndicatorVO();
        vo.setIndicatorCode(code);
        vo.setIndicatorName(name);
        vo.setIndicatorValue(value);
        vo.setUnit(unit);
        vo.setReferenceRange(reference);
        vo.setStatus(status);
        vo.setDescription(description);
        return vo;
    }

    private ComplianceIssueVO buildIssue(String dimension, String code, String title, Integer riskLevel,
                                         String description, String riskImpact, String suggestion, BigDecimal amount) {
        ComplianceIssueVO vo = new ComplianceIssueVO();
        vo.setDimensionCode(dimension);
        vo.setIssueCode(code);
        vo.setIssueTitle(title);
        vo.setRiskLevel(riskLevel);
        vo.setDescription(description);
        vo.setRiskImpact(riskImpact);
        vo.setSuggestion(suggestion);
        vo.setInvolvedAmount(amount != null ? amount : BigDecimal.ZERO);
        return vo;
    }

    private BigDecimal calculateDimensionScore(int high, int medium, int low) {
        int deduction = high * 20 + medium * 10 + low * 5;
        BigDecimal score = new BigDecimal("100").subtract(new BigDecimal(deduction));
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score;
    }

    private String buildOverallSuggestion(int highRisk, int mediumRisk, int lowRisk, BigDecimal score) {
        StringBuilder sb = new StringBuilder();
        if (highRisk > 0) {
            sb.append("存在").append(highRisk).append("项高风险问题，建议立即处理，");
            if (score.compareTo(new BigDecimal("60")) < 0) {
                sb.append("重点关注财务记账完整性、税务申报合规性等核心问题。");
            } else {
                sb.append("重点关注高风险项的整改。");
            }
        } else if (mediumRisk > 0) {
            sb.append("存在").append(mediumRisk).append("项中风险问题，建议近期整改，");
            sb.append("关注财务指标合理性和发票合规性。");
        } else if (lowRisk > 0) {
            sb.append("存在").append(lowRisk).append("项低风险问题，总体合规情况良好，建议持续优化。");
        } else {
            sb.append("未发现明显合规风险，财税管理状况良好，请继续保持。");
        }
        return sb.toString();
    }
}
