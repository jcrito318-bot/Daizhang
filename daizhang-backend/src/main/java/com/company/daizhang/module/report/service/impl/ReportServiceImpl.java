package com.company.daizhang.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.service.ReportService;
import com.company.daizhang.module.report.util.ReportExcelUtil;
import com.company.daizhang.module.report.vo.*;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务报表服务实现
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final ReportExcelUtil reportExcelUtil;

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetVO balanceSheet(ReportQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        // 查询所有科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 查询当月科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 查询年初余额（1月份的期初余额）
        LambdaQueryWrapper<AccountBalance> beginWrapper = new LambdaQueryWrapper<>();
        beginWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, 1);
        List<AccountBalance> beginBalances = accountBalanceMapper.selectList(beginWrapper);
        Map<Long, AccountBalance> beginBalanceMap = beginBalances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 按科目类别分组：资产、负债、权益
        List<Subject> assetSubjects = subjects.stream()
                .filter(s -> "资产".equals(s.getCategory()))
                .sorted(Comparator.comparing(Subject::getCode))
                .collect(Collectors.toList());
        List<Subject> liabilitySubjects = subjects.stream()
                .filter(s -> "负债".equals(s.getCategory()))
                .sorted(Comparator.comparing(Subject::getCode))
                .collect(Collectors.toList());
        List<Subject> equitySubjects = subjects.stream()
                .filter(s -> "所有者权益".equals(s.getCategory()))
                .sorted(Comparator.comparing(Subject::getCode))
                .collect(Collectors.toList());

        // 构建资产负债表项目
        List<BalanceSheetItem> assets = buildBalanceSheetItems(assetSubjects, balanceMap, beginBalanceMap, true);
        List<BalanceSheetItem> liabilities = buildBalanceSheetItems(liabilitySubjects, balanceMap, beginBalanceMap, false);
        List<BalanceSheetItem> equity = buildBalanceSheetItems(equitySubjects, balanceMap, beginBalanceMap, false);

        // 计算合计
        BigDecimal totalAssets = assets.stream()
                .map(BalanceSheetItem::getEndingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilities = liabilities.stream()
                .map(BalanceSheetItem::getEndingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = equity.stream()
                .map(BalanceSheetItem::getEndingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        // 平衡校验
        boolean balanceCheck = totalAssets.compareTo(totalLiabilitiesAndEquity) == 0;

        BalanceSheetVO vo = new BalanceSheetVO();
        vo.setAssets(assets);
        vo.setTotalAssets(totalAssets);
        vo.setLiabilities(liabilities);
        vo.setTotalLiabilities(totalLiabilities);
        vo.setEquity(equity);
        vo.setTotalEquity(totalEquity);
        vo.setTotalLiabilitiesAndEquity(totalLiabilitiesAndEquity);
        vo.setBalanceCheck(balanceCheck);

        if (!balanceCheck) {
            throw new BusinessException("资产负债表不平衡：资产=" + totalAssets + 
                    "，负债+所有者权益=" + totalLiabilitiesAndEquity);
        }

        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeStatementVO incomeStatement(ReportQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        // 查询损益类科目（isCurrent=1）
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getIsCurrent, 1)
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 查询当月科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 按科目类别分组：收入、费用
        List<Subject> revenueSubjects = subjects.stream()
                .filter(s -> "损益".equals(s.getCategory()) && s.getBalanceDirection() == 2)
                .sorted(Comparator.comparing(Subject::getCode))
                .collect(Collectors.toList());
        List<Subject> expenseSubjects = subjects.stream()
                .filter(s -> "损益".equals(s.getCategory()) && s.getBalanceDirection() == 1)
                .sorted(Comparator.comparing(Subject::getCode))
                .collect(Collectors.toList());

        // 构建利润表项目
        List<IncomeStatementItem> items = new ArrayList<>();
        int rowNo = 1;

        // 一、营业收入
        BigDecimal operatingRevenue = BigDecimal.ZERO;
        BigDecimal operatingRevenueYear = BigDecimal.ZERO;
        for (Subject subject : revenueSubjects) {
            if (subject.getCode().startsWith("5001") || subject.getCode().startsWith("5051")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodCredit() != null 
                        ? balance.getPeriodCredit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearCredit() != null 
                        ? balance.getYearCredit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                operatingRevenue = operatingRevenue.add(currentAmount);
                operatingRevenueYear = operatingRevenueYear.add(yearAmount);
            }
        }

        // 二、营业成本
        BigDecimal operatingCost = BigDecimal.ZERO;
        BigDecimal operatingCostYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5401") || subject.getCode().startsWith("5402")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                operatingCost = operatingCost.add(currentAmount);
                operatingCostYear = operatingCostYear.add(yearAmount);
            }
        }

        // 三、营业税金及附加
        BigDecimal businessTax = BigDecimal.ZERO;
        BigDecimal businessTaxYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5403")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                businessTax = businessTax.add(currentAmount);
                businessTaxYear = businessTaxYear.add(yearAmount);
            }
        }

        // 四、销售费用
        BigDecimal sellingExpense = BigDecimal.ZERO;
        BigDecimal sellingExpenseYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5601")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                sellingExpense = sellingExpense.add(currentAmount);
                sellingExpenseYear = sellingExpenseYear.add(yearAmount);
            }
        }

        // 五、管理费用
        BigDecimal adminExpense = BigDecimal.ZERO;
        BigDecimal adminExpenseYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5602")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                adminExpense = adminExpense.add(currentAmount);
                adminExpenseYear = adminExpenseYear.add(yearAmount);
            }
        }

        // 六、财务费用
        BigDecimal financeExpense = BigDecimal.ZERO;
        BigDecimal financeExpenseYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5603")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                financeExpense = financeExpense.add(currentAmount);
                financeExpenseYear = financeExpenseYear.add(yearAmount);
            }
        }

        // 七、投资收益
        BigDecimal investmentIncome = BigDecimal.ZERO;
        BigDecimal investmentIncomeYear = BigDecimal.ZERO;
        for (Subject subject : revenueSubjects) {
            if (subject.getCode().startsWith("5111")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodCredit() != null 
                        ? balance.getPeriodCredit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearCredit() != null 
                        ? balance.getYearCredit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                investmentIncome = investmentIncome.add(currentAmount);
                investmentIncomeYear = investmentIncomeYear.add(yearAmount);
            }
        }

        // 八、营业利润 = 营业收入 - 营业成本 - 营业税金及附加 - 销售费用 - 管理费用 - 财务费用 + 投资收益
        BigDecimal operatingProfit = operatingRevenue.subtract(operatingCost)
                .subtract(businessTax)
                .subtract(sellingExpense)
                .subtract(adminExpense)
                .subtract(financeExpense)
                .add(investmentIncome);
        BigDecimal operatingProfitYear = operatingRevenueYear.subtract(operatingCostYear)
                .subtract(businessTaxYear)
                .subtract(sellingExpenseYear)
                .subtract(adminExpenseYear)
                .subtract(financeExpenseYear)
                .add(investmentIncomeYear);

        // 九、营业外收入
        BigDecimal nonOperatingIncome = BigDecimal.ZERO;
        BigDecimal nonOperatingIncomeYear = BigDecimal.ZERO;
        for (Subject subject : revenueSubjects) {
            if (subject.getCode().startsWith("5301")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodCredit() != null 
                        ? balance.getPeriodCredit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearCredit() != null 
                        ? balance.getYearCredit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                nonOperatingIncome = nonOperatingIncome.add(currentAmount);
                nonOperatingIncomeYear = nonOperatingIncomeYear.add(yearAmount);
            }
        }

        // 十、营业外支出
        BigDecimal nonOperatingExpense = BigDecimal.ZERO;
        BigDecimal nonOperatingExpenseYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5711")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                nonOperatingExpense = nonOperatingExpense.add(currentAmount);
                nonOperatingExpenseYear = nonOperatingExpenseYear.add(yearAmount);
            }
        }

        // 十一、利润总额 = 营业利润 + 营业外收入 - 营业外支出
        BigDecimal totalProfit = operatingProfit.add(nonOperatingIncome).subtract(nonOperatingExpense);
        BigDecimal totalProfitYear = operatingProfitYear.add(nonOperatingIncomeYear).subtract(nonOperatingExpenseYear);

        // 十二、所得税费用
        BigDecimal incomeTax = BigDecimal.ZERO;
        BigDecimal incomeTaxYear = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            if (subject.getCode().startsWith("5801")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                BigDecimal currentAmount = balance != null && balance.getPeriodDebit() != null 
                        ? balance.getPeriodDebit() : BigDecimal.ZERO;
                BigDecimal yearAmount = balance != null && balance.getYearDebit() != null 
                        ? balance.getYearDebit() : BigDecimal.ZERO;

                IncomeStatementItem item = new IncomeStatementItem();
                item.setRowNo(rowNo++);
                item.setName(subject.getName());
                item.setCode(subject.getCode());
                item.setCurrentAmount(currentAmount);
                item.setYearAmount(yearAmount);
                items.add(item);

                incomeTax = incomeTax.add(currentAmount);
                incomeTaxYear = incomeTaxYear.add(yearAmount);
            }
        }

        // 十三、净利润 = 利润总额 - 所得税费用
        BigDecimal netProfit = totalProfit.subtract(incomeTax);
        BigDecimal netProfitYear = totalProfitYear.subtract(incomeTaxYear);

        IncomeStatementVO vo = new IncomeStatementVO();
        vo.setItems(items);
        vo.setTotalRevenue(operatingRevenue);
        vo.setTotalExpense(operatingCost.add(businessTax).add(sellingExpense).add(adminExpense)
                .add(financeExpense).add(nonOperatingExpense).add(incomeTax));
        vo.setNetProfit(netProfit);

        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectBalanceTableVO subjectBalanceTable(ReportQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        // 查询所有科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getCode);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 查询当月科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 构建科目余额表（支持层级展开）
        List<SubjectBalanceRow> rows = buildSubjectBalanceRows(subjects, balanceMap);

        // 计算合计
        BigDecimal totalBeginDebit = rows.stream()
                .map(SubjectBalanceRow::getBeginDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBeginCredit = rows.stream()
                .map(SubjectBalanceRow::getBeginCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPeriodDebit = rows.stream()
                .map(SubjectBalanceRow::getPeriodDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPeriodCredit = rows.stream()
                .map(SubjectBalanceRow::getPeriodCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEndDebit = rows.stream()
                .map(SubjectBalanceRow::getEndDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEndCredit = rows.stream()
                .map(SubjectBalanceRow::getEndCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 试算平衡校验
        boolean trialBalanceCheck = totalBeginDebit.compareTo(totalBeginCredit) == 0
                && totalPeriodDebit.compareTo(totalPeriodCredit) == 0
                && totalEndDebit.compareTo(totalEndCredit) == 0;

        SubjectBalanceTableVO vo = new SubjectBalanceTableVO();
        vo.setRows(rows);
        vo.setTotalBeginDebit(totalBeginDebit);
        vo.setTotalBeginCredit(totalBeginCredit);
        vo.setTotalPeriodDebit(totalPeriodDebit);
        vo.setTotalPeriodCredit(totalPeriodCredit);
        vo.setTotalEndDebit(totalEndDebit);
        vo.setTotalEndCredit(totalEndCredit);
        vo.setTrialBalanceCheck(trialBalanceCheck);

        if (!trialBalanceCheck) {
            throw new BusinessException("试算不平衡：期初借方=" + totalBeginDebit + 
                    "，期初贷方=" + totalBeginCredit + 
                    "；本期借方=" + totalPeriodDebit + 
                    "，本期贷方=" + totalPeriodCredit + 
                    "；期末借方=" + totalEndDebit + 
                    "，期末贷方=" + totalEndCredit);
        }

        return vo;
    }

    @Override
    public void exportBalanceSheet(ReportQueryRequest request, HttpServletResponse response) {
        BalanceSheetVO data = balanceSheet(request);
        reportExcelUtil.exportBalanceSheet(data, request.getYear(), request.getMonth(), response);
    }

    @Override
    public void exportIncomeStatement(ReportQueryRequest request, HttpServletResponse response) {
        IncomeStatementVO data = incomeStatement(request);
        reportExcelUtil.exportIncomeStatement(data, request.getYear(), request.getMonth(), response);
    }

    @Override
    public void exportSubjectBalanceTable(ReportQueryRequest request, HttpServletResponse response) {
        SubjectBalanceTableVO data = subjectBalanceTable(request);
        reportExcelUtil.exportSubjectBalanceTable(data, request.getYear(), request.getMonth(), response);
    }

    /**
     * 构建资产负债表项目
     */
    private List<BalanceSheetItem> buildBalanceSheetItems(List<Subject> subjects, 
                                                           Map<Long, AccountBalance> balanceMap,
                                                           Map<Long, AccountBalance> beginBalanceMap,
                                                           boolean isAsset) {
        List<BalanceSheetItem> items = new ArrayList<>();
        int rowNo = 1;

        for (Subject subject : subjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            AccountBalance beginBalance = beginBalanceMap.get(subject.getId());

            BigDecimal beginning = calculateBeginningBalance(beginBalance, subject.getBalanceDirection(), isAsset);
            BigDecimal ending = calculateEndingBalance(balance, subject.getBalanceDirection(), isAsset);

            BalanceSheetItem item = new BalanceSheetItem();
            item.setRowNo(rowNo++);
            item.setName(subject.getName());
            item.setCode(subject.getCode());
            item.setBeginningBalance(beginning);
            item.setEndingBalance(ending);
            items.add(item);
        }

        return items;
    }

    /**
     * 构建科目余额表行（支持层级展开）
     */
    private List<SubjectBalanceRow> buildSubjectBalanceRows(List<Subject> subjects, 
                                                             Map<Long, AccountBalance> balanceMap) {
        List<SubjectBalanceRow> rows = new ArrayList<>();

        for (Subject subject : subjects) {
            AccountBalance balance = balanceMap.get(subject.getId());

            SubjectBalanceRow row = new SubjectBalanceRow();
            row.setSubjectId(subject.getId());
            row.setSubjectCode(subject.getCode());
            row.setSubjectName(subject.getName());
            row.setLevel(subject.getLevel());
            row.setBalanceDirection(subject.getBalanceDirection());
            row.setHasChildren(hasChildren(subjects, subject.getId()));

            if (balance != null) {
                row.setBeginDebit(balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO);
                row.setBeginCredit(balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO);
                row.setPeriodDebit(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO);
                row.setPeriodCredit(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO);
                row.setEndDebit(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
                row.setEndCredit(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);
                row.setYearDebit(balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO);
                row.setYearCredit(balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO);
            } else {
                row.setBeginDebit(BigDecimal.ZERO);
                row.setBeginCredit(BigDecimal.ZERO);
                row.setPeriodDebit(BigDecimal.ZERO);
                row.setPeriodCredit(BigDecimal.ZERO);
                row.setEndDebit(BigDecimal.ZERO);
                row.setEndCredit(BigDecimal.ZERO);
                row.setYearDebit(BigDecimal.ZERO);
                row.setYearCredit(BigDecimal.ZERO);
            }

            rows.add(row);
        }

        return rows;
    }

    /**
     * 计算期初余额
     */
    private BigDecimal calculateBeginningBalance(AccountBalance balance, Integer balanceDirection, boolean isAsset) {
        if (balance == null) {
            return BigDecimal.ZERO;
        }

        if (isAsset) {
            // 资产类：期初借方余额
            return balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO;
        } else {
            // 负债/权益类：期初贷方余额
            return balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO;
        }
    }

    /**
     * 计算期末余额
     */
    private BigDecimal calculateEndingBalance(AccountBalance balance, Integer balanceDirection, boolean isAsset) {
        if (balance == null) {
            return BigDecimal.ZERO;
        }

        if (isAsset) {
            // 资产类：期末借方余额
            return balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
        } else {
            // 负债/权益类：期末贷方余额
            return balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
        }
    }

    /**
     * 判断是否有子科目
     */
    private boolean hasChildren(List<Subject> subjects, Long parentId) {
        return subjects.stream().anyMatch(s -> parentId.equals(s.getParentId()));
    }
}
