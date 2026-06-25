package com.company.daizhang.module.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.entity.CashFlowAdjustment;
import com.company.daizhang.module.report.mapper.CashFlowAdjustmentMapper;
import com.company.daizhang.module.report.service.ReportService;
import com.company.daizhang.module.report.util.ReportExcelUtil;
import com.company.daizhang.module.report.vo.*;
import com.company.daizhang.module.subject.entity.AuxiliaryCategory;
import com.company.daizhang.module.subject.entity.AuxiliaryItem;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.AuxiliaryCategoryMapper;
import com.company.daizhang.module.subject.mapper.AuxiliaryItemMapper;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务报表服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final ReportExcelUtil reportExcelUtil;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final CashFlowAdjustmentMapper cashFlowAdjustmentMapper;
    private final AuxiliaryCategoryMapper auxiliaryCategoryMapper;
    private final AuxiliaryItemMapper auxiliaryItemMapper;

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
    @Transactional(readOnly = true)
    public CashFlowStatementVO cashFlowStatement(Long accountSetId, Integer year, Integer month) {
        // 1. 查询该账套该期间所有已过账凭证（status=2）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getStatus, 2);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        BigDecimal operatingInflow = BigDecimal.ZERO;
        BigDecimal operatingOutflow = BigDecimal.ZERO;
        BigDecimal investingInflow = BigDecimal.ZERO;
        BigDecimal investingOutflow = BigDecimal.ZERO;
        BigDecimal financingInflow = BigDecimal.ZERO;
        BigDecimal financingOutflow = BigDecimal.ZERO;
        List<CashFlowItemVO> items = new ArrayList<>();

        if (vouchers.isEmpty()) {
            return buildCashFlowStatementVO(year, month, operatingInflow, operatingOutflow,
                    investingInflow, investingOutflow, financingInflow, financingOutflow, items);
        }

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 2. 查询这些凭证的明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
        List<VoucherDetail> allDetails = voucherDetailMapper.selectList(detailWrapper);

        // 按凭证ID分组
        Map<Long, List<VoucherDetail>> detailsByVoucher = allDetails.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getVoucherId));

        // 3. 分析每张凭证
        for (Voucher voucher : vouchers) {
            List<VoucherDetail> details = detailsByVoucher.get(voucher.getId());
            if (details == null || details.isEmpty()) {
                continue;
            }

            // 分离现金科目行和非现金科目行
            List<VoucherDetail> cashLines = new ArrayList<>();
            List<VoucherDetail> nonCashLines = new ArrayList<>();
            for (VoucherDetail d : details) {
                if (isCashSubject(d.getSubjectCode())) {
                    cashLines.add(d);
                } else {
                    nonCashLines.add(d);
                }
            }

            // 没有现金科目，跳过
            if (cashLines.isEmpty()) {
                continue;
            }

            // 根据非现金科目编码判断现金流类别
            String category = determineCashFlowCategory(nonCashLines);
            if (category == null) {
                continue;
            }

            // 构建项目名称
            String itemName = buildCashFlowItemName(nonCashLines);

            // 计算现金流入流出（现金科目借方为流入，贷方为流出）
            for (VoucherDetail cashLine : cashLines) {
                BigDecimal debit = cashLine.getDebit() != null ? cashLine.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = cashLine.getCredit() != null ? cashLine.getCredit() : BigDecimal.ZERO;

                if (debit.compareTo(BigDecimal.ZERO) > 0) {
                    // 借方 - 现金流入
                    CashFlowItemVO item = new CashFlowItemVO();
                    item.setCategory(category);
                    item.setItemName(itemName);
                    item.setAmount(debit);
                    items.add(item);

                    if ("经营".equals(category)) {
                        operatingInflow = operatingInflow.add(debit);
                    } else if ("投资".equals(category)) {
                        investingInflow = investingInflow.add(debit);
                    } else if ("筹资".equals(category)) {
                        financingInflow = financingInflow.add(debit);
                    }
                }
                if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    // 贷方 - 现金流出
                    CashFlowItemVO item = new CashFlowItemVO();
                    item.setCategory(category);
                    item.setItemName(itemName);
                    item.setAmount(credit);
                    items.add(item);

                    if ("经营".equals(category)) {
                        operatingOutflow = operatingOutflow.add(credit);
                    } else if ("投资".equals(category)) {
                        investingOutflow = investingOutflow.add(credit);
                    } else if ("筹资".equals(category)) {
                        financingOutflow = financingOutflow.add(credit);
                    }
                }
            }
        }

        // 4. 汇总计算净额
        return buildCashFlowStatementVO(year, month, operatingInflow, operatingOutflow,
                investingInflow, investingOutflow, financingInflow, financingOutflow, items);
    }

    /**
     * 构建现金流量表VO
     */
    private CashFlowStatementVO buildCashFlowStatementVO(Integer year, Integer month,
                                                         BigDecimal operatingInflow, BigDecimal operatingOutflow,
                                                         BigDecimal investingInflow, BigDecimal investingOutflow,
                                                         BigDecimal financingInflow, BigDecimal financingOutflow,
                                                         List<CashFlowItemVO> items) {
        BigDecimal operatingNetFlow = operatingInflow.subtract(operatingOutflow);
        BigDecimal investingNetFlow = investingInflow.subtract(investingOutflow);
        BigDecimal financingNetFlow = financingInflow.subtract(financingOutflow);
        BigDecimal netIncrease = operatingNetFlow.add(investingNetFlow).add(financingNetFlow);

        CashFlowStatementVO vo = new CashFlowStatementVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setOperatingInflow(operatingInflow);
        vo.setOperatingOutflow(operatingOutflow);
        vo.setOperatingNetFlow(operatingNetFlow);
        vo.setInvestingInflow(investingInflow);
        vo.setInvestingOutflow(investingOutflow);
        vo.setInvestingNetFlow(investingNetFlow);
        vo.setFinancingInflow(financingInflow);
        vo.setFinancingOutflow(financingOutflow);
        vo.setFinancingNetFlow(financingNetFlow);
        vo.setNetIncrease(netIncrease);
        vo.setItems(items);
        return vo;
    }

    /**
     * 判断是否为现金科目（1001库存现金、1002银行存款）
     */
    private boolean isCashSubject(String code) {
        return code != null && (code.startsWith("1001") || code.startsWith("1002"));
    }

    /**
     * 根据非现金科目编码判断现金流类别
     * 经营：6001主营收入/6051其他收入/6601销售费用/6602管理费用
     * 投资：1601固定资产/1604在建工程/1606固定资产清理
     * 筹资：2001短期借款/2002长期借款/2241其他应付款
     */
    private String determineCashFlowCategory(List<VoucherDetail> nonCashLines) {
        for (VoucherDetail d : nonCashLines) {
            String code = d.getSubjectCode();
            if (code == null) {
                continue;
            }
            if (code.startsWith("6001") || code.startsWith("6051")
                    || code.startsWith("6601") || code.startsWith("6602")) {
                return "经营";
            }
            if (code.startsWith("1601") || code.startsWith("1604") || code.startsWith("1606")) {
                return "投资";
            }
            if (code.startsWith("2001") || code.startsWith("2002") || code.startsWith("2241")) {
                return "筹资";
            }
        }
        return null;
    }

    /**
     * 构建现金流量明细项名称
     */
    private String buildCashFlowItemName(List<VoucherDetail> nonCashLines) {
        if (nonCashLines.isEmpty()) {
            return "现金流量项目";
        }
        String name = nonCashLines.stream()
                .map(VoucherDetail::getSubjectName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));
        return name.isEmpty() ? "现金流量项目" : name;
    }

    @Override
    public void exportBalanceSheet(ReportQueryRequest request, HttpServletResponse response) {
        BalanceSheetVO data = balanceSheet(request);
        reportExcelUtil.exportBalanceSheet(data, request.getYear(), request.getMonth(), response);
    }

    @Override
    public void exportCashFlowStatement(Long accountSetId, Integer year, Integer month, HttpServletResponse response) {
        CashFlowStatementVO data = cashFlowStatement(accountSetId, year, month);
        reportExcelUtil.exportCashFlowStatement(data, year, month, response);
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

    // ==================== 现金流量表手动调整 ====================

    @Override
    @Transactional(readOnly = true)
    public List<CashFlowAdjustmentVO> listAdjustments(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<CashFlowAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CashFlowAdjustment::getAccountSetId, accountSetId)
                .eq(CashFlowAdjustment::getYear, year)
                .eq(CashFlowAdjustment::getMonth, month)
                .orderByAsc(CashFlowAdjustment::getId);
        List<CashFlowAdjustment> list = cashFlowAdjustmentMapper.selectList(wrapper);
        return list.stream()
                .map(this::convertAdjustmentToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAdjustment(CashFlowAdjustmentVO request) {
        if (request.getAccountSetId() == null) {
            throw new BusinessException("账套ID不能为空");
        }
        if (request.getYear() == null || request.getMonth() == null) {
            throw new BusinessException("年度和月份不能为空");
        }
        if (request.getItemName() == null || request.getItemName().trim().isEmpty()) {
            throw new BusinessException("调整项名称不能为空");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new BusinessException("调整类别不能为空");
        }

        CashFlowAdjustment entity = new CashFlowAdjustment();
        BeanUtil.copyProperties(request, entity);
        if (entity.getOriginalAmount() == null) {
            entity.setOriginalAmount(BigDecimal.ZERO);
        }
        if (entity.getAdjustedAmount() == null) {
            entity.setAdjustedAmount(BigDecimal.ZERO);
        }

        if (request.getId() != null) {
            // 更新
            entity.setId(request.getId());
            cashFlowAdjustmentMapper.updateById(entity);
            log.info("更新现金流量调整项成功，ID: {}", request.getId());
        } else {
            // 新增
            cashFlowAdjustmentMapper.insert(entity);
            log.info("新增现金流量调整项成功，ID: {}", entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdjustment(Long id) {
        CashFlowAdjustment entity = cashFlowAdjustmentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("现金流量调整项不存在");
        }
        cashFlowAdjustmentMapper.deleteById(id);
        log.info("删除现金流量调整项成功，ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CashFlowStatementVO cashFlowStatementWithAdjustment(Long accountSetId, Integer year, Integer month) {
        // 1. 获取原始现金流量表
        CashFlowStatementVO vo = cashFlowStatement(accountSetId, year, month);

        // 2. 查询该期间所有调整项
        LambdaQueryWrapper<CashFlowAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CashFlowAdjustment::getAccountSetId, accountSetId)
                .eq(CashFlowAdjustment::getYear, year)
                .eq(CashFlowAdjustment::getMonth, month)
                .orderByAsc(CashFlowAdjustment::getId);
        List<CashFlowAdjustment> adjustments = cashFlowAdjustmentMapper.selectList(wrapper);

        if (adjustments.isEmpty()) {
            return vo;
        }

        // 3. 按类别应用调整（用调整后金额替换原始金额）
        BigDecimal operatingInflow = vo.getOperatingInflow() != null ? vo.getOperatingInflow() : BigDecimal.ZERO;
        BigDecimal operatingOutflow = vo.getOperatingOutflow() != null ? vo.getOperatingOutflow() : BigDecimal.ZERO;
        BigDecimal investingInflow = vo.getInvestingInflow() != null ? vo.getInvestingInflow() : BigDecimal.ZERO;
        BigDecimal investingOutflow = vo.getInvestingOutflow() != null ? vo.getInvestingOutflow() : BigDecimal.ZERO;
        BigDecimal financingInflow = vo.getFinancingInflow() != null ? vo.getFinancingInflow() : BigDecimal.ZERO;
        BigDecimal financingOutflow = vo.getFinancingOutflow() != null ? vo.getFinancingOutflow() : BigDecimal.ZERO;

        List<CashFlowItemVO> items = vo.getItems() != null ? new ArrayList<>(vo.getItems()) : new ArrayList<>();

        for (CashFlowAdjustment adj : adjustments) {
            BigDecimal original = adj.getOriginalAmount() != null ? adj.getOriginalAmount() : BigDecimal.ZERO;
            BigDecimal adjusted = adj.getAdjustedAmount() != null ? adj.getAdjustedAmount() : BigDecimal.ZERO;
            BigDecimal diff = adjusted.subtract(original);
            String category = adj.getCategory();

            CashFlowItemVO item = new CashFlowItemVO();
            item.setCategory(category);
            item.setItemName("[调整]" + adj.getItemName());
            item.setAmount(diff);
            items.add(item);

            // 根据类别和调整方向（正数为流入增加/流出增加，负数为减少）调整对应类别合计
            if ("经营".equals(category)) {
                if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                    operatingInflow = operatingInflow.add(diff);
                } else {
                    operatingOutflow = operatingOutflow.add(diff.negate());
                }
            } else if ("投资".equals(category)) {
                if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                    investingInflow = investingInflow.add(diff);
                } else {
                    investingOutflow = investingOutflow.add(diff.negate());
                }
            } else if ("筹资".equals(category)) {
                if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                    financingInflow = financingInflow.add(diff);
                } else {
                    financingOutflow = financingOutflow.add(diff.negate());
                }
            }
        }

        // 4. 重新计算净额
        BigDecimal operatingNetFlow = operatingInflow.subtract(operatingOutflow);
        BigDecimal investingNetFlow = investingInflow.subtract(investingOutflow);
        BigDecimal financingNetFlow = financingInflow.subtract(financingOutflow);
        BigDecimal netIncrease = operatingNetFlow.add(investingNetFlow).add(financingNetFlow);

        vo.setOperatingInflow(operatingInflow);
        vo.setOperatingOutflow(operatingOutflow);
        vo.setOperatingNetFlow(operatingNetFlow);
        vo.setInvestingInflow(investingInflow);
        vo.setInvestingOutflow(investingOutflow);
        vo.setInvestingNetFlow(investingNetFlow);
        vo.setFinancingInflow(financingInflow);
        vo.setFinancingOutflow(financingOutflow);
        vo.setFinancingNetFlow(financingNetFlow);
        vo.setNetIncrease(netIncrease);
        vo.setItems(items);

        return vo;
    }

    /**
     * 现金流量调整实体转VO
     */
    private CashFlowAdjustmentVO convertAdjustmentToVO(CashFlowAdjustment entity) {
        CashFlowAdjustmentVO vo = new CashFlowAdjustmentVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    // ==================== 同比环比分析 ====================

    @Override
    @Transactional(readOnly = true)
    public List<YearOnYearVO> yearOnYearAnalysis(Long accountSetId, Integer year, Integer month) {
        List<YearOnYearVO> result = new ArrayList<>();

        // 计算同期（去年同期）
        int prevYear = year - 1;
        int prevMonthYear = year;
        int prevMonth = month - 1;
        if (prevMonth < 1) {
            prevMonth = 12;
            prevMonthYear = year - 1;
        }

        // 本期指标
        Map<String, BigDecimal> currentIndicators = calculateIndicators(accountSetId, year, month);
        // 同期指标（去年同期）
        Map<String, BigDecimal> yoyIndicators = calculateIndicators(accountSetId, prevYear, month);
        // 上月指标
        Map<String, BigDecimal> momIndicators = calculateIndicators(accountSetId, prevMonthYear, prevMonth);

        // 指标顺序
        String[] indicatorNames = {"营业收入", "营业成本", "利润总额", "资产总额", "负债总额", "所有者权益"};

        for (String name : indicatorNames) {
            BigDecimal currentValue = currentIndicators.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal yoyValue = yoyIndicators.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal momValue = momIndicators.getOrDefault(name, BigDecimal.ZERO);

            YearOnYearVO vo = new YearOnYearVO();
            vo.setIndicatorName(name);
            vo.setCurrentValue(currentValue);
            vo.setPreviousValue(yoyValue);
            vo.setPreviousMonthValue(momValue);
            vo.setYoyGrowthRate(calcGrowthRate(currentValue, yoyValue));
            vo.setMomGrowthRate(calcGrowthRate(currentValue, momValue));
            result.add(vo);
        }

        return result;
    }

    /**
     * 计算指定期间的关键财务指标
     */
    private Map<String, BigDecimal> calculateIndicators(Long accountSetId, Integer year, Integer month) {
        Map<String, BigDecimal> indicators = new LinkedHashMap<>();

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
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b, (a, b) -> a));

        BigDecimal operatingRevenue = BigDecimal.ZERO;
        BigDecimal operatingCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        for (Subject subject : subjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance == null) {
                continue;
            }
            String code = subject.getCode();
            if (code == null) {
                continue;
            }

            // 收入类（损益-贷方）
            if (code.startsWith("5001") || code.startsWith("5051") || code.startsWith("5301")
                    || code.startsWith("5111")) {
                BigDecimal amt = balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO;
                operatingRevenue = operatingRevenue.add(amt);
            }
            // 成本费用类（损益-借方）
            if (code.startsWith("5401") || code.startsWith("5402") || code.startsWith("5403")
                    || code.startsWith("5601") || code.startsWith("5602") || code.startsWith("5603")
                    || code.startsWith("5711") || code.startsWith("5801")) {
                BigDecimal amt = balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO;
                operatingCost = operatingCost.add(amt);
            }
            // 资产类
            if ("资产".equals(subject.getCategory())) {
                BigDecimal amt = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                totalAssets = totalAssets.add(amt);
            }
            // 负债类
            if ("负债".equals(subject.getCategory())) {
                BigDecimal amt = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                totalLiabilities = totalLiabilities.add(amt);
            }
            // 所有者权益类
            if ("所有者权益".equals(subject.getCategory())) {
                BigDecimal amt = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                totalEquity = totalEquity.add(amt);
            }
        }

        // 利润总额 = 收入 - 成本费用
        totalProfit = operatingRevenue.subtract(operatingCost);

        indicators.put("营业收入", operatingRevenue);
        indicators.put("营业成本", operatingCost);
        indicators.put("利润总额", totalProfit);
        indicators.put("资产总额", totalAssets);
        indicators.put("负债总额", totalLiabilities);
        indicators.put("所有者权益", totalEquity);

        return indicators;
    }

    /**
     * 计算增长率 = (本期 - 同期) / |同期| * 100
     */
    private BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal diff = current.subtract(previous);
        return diff.divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 报表打印 ====================

    @Override
    @Transactional(readOnly = true)
    public String generatePrintHtml(Long accountSetId, Integer year, Integer month, String reportType) {
        ReportQueryRequest request = new ReportQueryRequest();
        request.setAccountSetId(accountSetId);
        request.setYear(year);
        request.setMonth(month);

        String periodTitle = year + "年" + (month < 10 ? "0" + month : month) + "月";
        String printDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        html.append("<title>财务报表打印</title>");
        html.append("<style>");
        html.append("body{font-family:'SimSun','宋体',serif;margin:20px;color:#000;}");
        html.append(".report-title{text-align:center;font-size:22px;font-weight:bold;margin:10px 0;}");
        html.append(".report-period{text-align:center;font-size:14px;margin:5px 0 20px;}");
        html.append("table{width:100%;border-collapse:collapse;font-size:12px;}");
        html.append("th,td{border:1px solid #000;padding:4px 6px;text-align:left;}");
        html.append("th{background-color:#f0f0f0;font-weight:bold;}");
        html.append(".num{text-align:right;}");
        html.append(".total{font-weight:bold;background-color:#fafafa;}");
        html.append(".footer{margin-top:20px;text-align:right;font-size:12px;color:#666;}");
        html.append("@media print{.no-print{display:none;}}");
        html.append("</style></head><body>");

        if ("balance-sheet".equals(reportType)) {
            html.append(buildBalanceSheetHtml(request, periodTitle, printDate));
        } else if ("income-statement".equals(reportType)) {
            html.append(buildIncomeStatementHtml(request, periodTitle, printDate));
        } else if ("cash-flow-statement".equals(reportType)) {
            html.append(buildCashFlowStatementHtml(accountSetId, year, month, periodTitle, printDate));
        } else if ("subject-balance".equals(reportType)) {
            html.append(buildSubjectBalanceHtml(request, periodTitle, printDate));
        } else {
            html.append("<div class=\"report-title\">不支持的报表类型: ").append(reportType).append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * 构建资产负债表打印HTML
     */
    private String buildBalanceSheetHtml(ReportQueryRequest request, String periodTitle, String printDate) {
        BalanceSheetVO vo = balanceSheet(request);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"report-title\">资产负债表</div>");
        sb.append("<div class=\"report-period\">会计期间：").append(periodTitle).append("</div>");
        sb.append("<table>");
        sb.append("<thead><tr><th>行次</th><th>项目</th><th>编码</th><th class=\"num\">期初余额</th><th class=\"num\">期末余额</th></tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td colspan=\"5\"><b>资产</b></td></tr>");
        if (vo.getAssets() != null) {
            for (BalanceSheetItem item : vo.getAssets()) {
                sb.append("<tr>");
                sb.append("<td>").append(item.getRowNo()).append("</td>");
                sb.append("<td>").append(item.getName() != null ? item.getName() : "").append("</td>");
                sb.append("<td>").append(item.getCode() != null ? item.getCode() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getBeginningBalance())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getEndingBalance())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td></td><td>资产合计</td><td></td>");
        sb.append("<td class=\"num\"></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalAssets())).append("</td></tr>");

        sb.append("<tr><td colspan=\"5\"><b>负债</b></td></tr>");
        if (vo.getLiabilities() != null) {
            for (BalanceSheetItem item : vo.getLiabilities()) {
                sb.append("<tr>");
                sb.append("<td>").append(item.getRowNo()).append("</td>");
                sb.append("<td>").append(item.getName() != null ? item.getName() : "").append("</td>");
                sb.append("<td>").append(item.getCode() != null ? item.getCode() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getBeginningBalance())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getEndingBalance())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td></td><td>负债合计</td><td></td>");
        sb.append("<td class=\"num\"></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalLiabilities())).append("</td></tr>");

        sb.append("<tr><td colspan=\"5\"><b>所有者权益</b></td></tr>");
        if (vo.getEquity() != null) {
            for (BalanceSheetItem item : vo.getEquity()) {
                sb.append("<tr>");
                sb.append("<td>").append(item.getRowNo()).append("</td>");
                sb.append("<td>").append(item.getName() != null ? item.getName() : "").append("</td>");
                sb.append("<td>").append(item.getCode() != null ? item.getCode() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getBeginningBalance())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getEndingBalance())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td></td><td>所有者权益合计</td><td></td>");
        sb.append("<td class=\"num\"></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalEquity())).append("</td></tr>");

        sb.append("<tr class=\"total\"><td></td><td>负债和所有者权益总计</td><td></td>");
        sb.append("<td class=\"num\"></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalLiabilitiesAndEquity())).append("</td></tr>");
        sb.append("</tbody></table>");
        sb.append("<div class=\"footer\">打印日期：").append(printDate).append("</div>");
        return sb.toString();
    }

    /**
     * 构建利润表打印HTML
     */
    private String buildIncomeStatementHtml(ReportQueryRequest request, String periodTitle, String printDate) {
        IncomeStatementVO vo = incomeStatement(request);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"report-title\">利润表</div>");
        sb.append("<div class=\"report-period\">会计期间：").append(periodTitle).append("</div>");
        sb.append("<table>");
        sb.append("<thead><tr><th>行次</th><th>项目</th><th>编码</th><th class=\"num\">本期金额</th><th class=\"num\">本年累计金额</th></tr></thead>");
        sb.append("<tbody>");
        if (vo.getItems() != null) {
            for (IncomeStatementItem item : vo.getItems()) {
                sb.append("<tr>");
                sb.append("<td>").append(item.getRowNo()).append("</td>");
                sb.append("<td>").append(item.getName() != null ? item.getName() : "").append("</td>");
                sb.append("<td>").append(item.getCode() != null ? item.getCode() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getCurrentAmount())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getYearAmount())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td></td><td>收入合计</td><td></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalRevenue())).append("</td>");
        sb.append("<td class=\"num\"></td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>费用合计</td><td></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalExpense())).append("</td>");
        sb.append("<td class=\"num\"></td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>净利润</td><td></td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getNetProfit())).append("</td>");
        sb.append("<td class=\"num\"></td></tr>");
        sb.append("</tbody></table>");
        sb.append("<div class=\"footer\">打印日期：").append(printDate).append("</div>");
        return sb.toString();
    }

    /**
     * 构建现金流量表打印HTML
     */
    private String buildCashFlowStatementHtml(Long accountSetId, Integer year, Integer month, String periodTitle, String printDate) {
        CashFlowStatementVO vo = cashFlowStatement(accountSetId, year, month);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"report-title\">现金流量表</div>");
        sb.append("<div class=\"report-period\">会计期间：").append(periodTitle).append("</div>");
        sb.append("<table>");
        sb.append("<thead><tr><th>类别</th><th>项目</th><th class=\"num\">金额</th></tr></thead>");
        sb.append("<tbody>");
        if (vo.getItems() != null) {
            for (CashFlowItemVO item : vo.getItems()) {
                sb.append("<tr>");
                sb.append("<td>").append(item.getCategory() != null ? item.getCategory() : "").append("</td>");
                sb.append("<td>").append(item.getItemName() != null ? item.getItemName() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(item.getAmount())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td>经营活动</td><td>现金流入小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getOperatingInflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金流出小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getOperatingOutflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金净额</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getOperatingNetFlow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td>投资活动</td><td>现金流入小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getInvestingInflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金流出小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getInvestingOutflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金净额</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getInvestingNetFlow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td>筹资活动</td><td>现金流入小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getFinancingInflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金流出小计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getFinancingOutflow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金净额</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getFinancingNetFlow())).append("</td></tr>");
        sb.append("<tr class=\"total\"><td></td><td>现金净增加额</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getNetIncrease())).append("</td></tr>");
        sb.append("</tbody></table>");
        sb.append("<div class=\"footer\">打印日期：").append(printDate).append("</div>");
        return sb.toString();
    }

    /**
     * 构建科目余额表打印HTML
     */
    private String buildSubjectBalanceHtml(ReportQueryRequest request, String periodTitle, String printDate) {
        SubjectBalanceTableVO vo = subjectBalanceTable(request);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"report-title\">科目余额表</div>");
        sb.append("<div class=\"report-period\">会计期间：").append(periodTitle).append("</div>");
        sb.append("<table>");
        sb.append("<thead><tr><th>科目编码</th><th>科目名称</th>");
        sb.append("<th class=\"num\">期初借方</th><th class=\"num\">期初贷方</th>");
        sb.append("<th class=\"num\">本期借方</th><th class=\"num\">本期贷方</th>");
        sb.append("<th class=\"num\">期末借方</th><th class=\"num\">期末贷方</th></tr></thead>");
        sb.append("<tbody>");
        if (vo.getRows() != null) {
            for (SubjectBalanceRow row : vo.getRows()) {
                sb.append("<tr>");
                sb.append("<td>").append(row.getSubjectCode() != null ? row.getSubjectCode() : "").append("</td>");
                sb.append("<td>").append(row.getSubjectName() != null ? row.getSubjectName() : "").append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getBeginDebit())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getBeginCredit())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getPeriodDebit())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getPeriodCredit())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getEndDebit())).append("</td>");
                sb.append("<td class=\"num\">").append(formatAmount(row.getEndCredit())).append("</td>");
                sb.append("</tr>");
            }
        }
        sb.append("<tr class=\"total\"><td colspan=\"2\">合计</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalBeginDebit())).append("</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalBeginCredit())).append("</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalPeriodDebit())).append("</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalPeriodCredit())).append("</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalEndDebit())).append("</td>");
        sb.append("<td class=\"num\">").append(formatAmount(vo.getTotalEndCredit())).append("</td>");
        sb.append("</tr>");
        sb.append("</tbody></table>");
        sb.append("<div class=\"footer\">打印日期：").append(printDate).append("</div>");
        return sb.toString();
    }

    /**
     * 格式化金额（保留两位小数）
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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

    @Override
    @Transactional(readOnly = true)
    public EquityChangeStatementVO equityChangeStatement(Long accountSetId, Integer year, Integer month) {
        // 查询所有者权益类科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1)
                .eq(Subject::getCategory, "所有者权益");
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 查询期末余额（month月）
        LambdaQueryWrapper<AccountBalance> endWrapper = new LambdaQueryWrapper<>();
        endWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> endBalances = accountBalanceMapper.selectList(endWrapper);
        Map<Long, AccountBalance> endMap = endBalances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 查询年初余额（1月份）
        LambdaQueryWrapper<AccountBalance> beginWrapper = new LambdaQueryWrapper<>();
        beginWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, 1);
        List<AccountBalance> beginBalances = accountBalanceMapper.selectList(beginWrapper);
        Map<Long, AccountBalance> beginMap = beginBalances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        List<EquityChangeItem> items = new ArrayList<>();
        BigDecimal totalBegin = BigDecimal.ZERO;
        BigDecimal totalEnd = BigDecimal.ZERO;
        int rowNo = 1;

        for (Subject subject : subjects.stream().sorted(Comparator.comparing(Subject::getCode)).collect(Collectors.toList())) {
            AccountBalance beginBal = beginMap.get(subject.getId());
            AccountBalance endBal = endMap.get(subject.getId());

            // 所有者权益科目余额在贷方
            BigDecimal beginAmount = beginBal != null && beginBal.getEndCredit() != null
                    ? beginBal.getEndCredit() : BigDecimal.ZERO;
            BigDecimal endAmount = endBal != null && endBal.getEndCredit() != null
                    ? endBal.getEndCredit() : BigDecimal.ZERO;
            BigDecimal increase = endAmount.subtract(beginAmount);

            EquityChangeItem item = new EquityChangeItem();
            item.setItemName(subject.getName());
            item.setRowNo(rowNo++);
            item.setBeginningBalance(beginAmount);
            item.setIncreaseAmount(increase.compareTo(BigDecimal.ZERO) > 0 ? increase : BigDecimal.ZERO);
            item.setDecreaseAmount(increase.compareTo(BigDecimal.ZERO) < 0 ? increase.negate() : BigDecimal.ZERO);
            item.setEndingBalance(endAmount);
            items.add(item);

            totalBegin = totalBegin.add(beginAmount);
            totalEnd = totalEnd.add(endAmount);
        }

        EquityChangeStatementVO vo = new EquityChangeStatementVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setItems(items);
        vo.setTotalBeginningBalance(totalBegin);
        vo.setTotalIncrease(totalEnd.subtract(totalBegin).compareTo(BigDecimal.ZERO) > 0
                ? totalEnd.subtract(totalBegin) : BigDecimal.ZERO);
        vo.setTotalDecrease(totalEnd.subtract(totalBegin).compareTo(BigDecimal.ZERO) < 0
                ? totalEnd.subtract(totalBegin).negate() : BigDecimal.ZERO);
        vo.setTotalEndingBalance(totalEnd);

        log.info("生成所有者权益变动表：accountSetId={}, year={}年{}月, 项目数={}", accountSetId, year, month, items.size());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentExpenseReportVO departmentExpenseReport(Long accountSetId, Integer year, Integer month) {
        DepartmentExpenseReportVO vo = new DepartmentExpenseReportVO();
        vo.setAccountSetId(accountSetId);
        vo.setYear(year);
        vo.setMonth(month);

        // 1. 查询部门辅助核算类别
        LambdaQueryWrapper<AuxiliaryCategory> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(AuxiliaryCategory::getAccountSetId, accountSetId)
                .eq(AuxiliaryCategory::getCategoryType, "部门");
        List<AuxiliaryCategory> deptCategories = auxiliaryCategoryMapper.selectList(catWrapper);

        if (deptCategories.isEmpty()) {
            vo.setItems(new ArrayList<>());
            vo.setTotalExpense(BigDecimal.ZERO);
            return vo;
        }

        // 2. 查询所有部门辅助核算项目
        List<Long> categoryIds = deptCategories.stream()
                .map(AuxiliaryCategory::getId).collect(Collectors.toList());
        LambdaQueryWrapper<AuxiliaryItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(AuxiliaryItem::getCategoryId, categoryIds)
                .eq(AuxiliaryItem::getStatus, 1);
        List<AuxiliaryItem> deptItems = auxiliaryItemMapper.selectList(itemWrapper);

        if (deptItems.isEmpty()) {
            vo.setItems(new ArrayList<>());
            vo.setTotalExpense(BigDecimal.ZERO);
            return vo;
        }

        Map<Long, AuxiliaryItem> deptItemMap = deptItems.stream()
                .collect(Collectors.toMap(AuxiliaryItem::getId, i -> i, (x, y) -> x));

        // 3. 查询费用类科目（损益类，借方余额）
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCategory, "损益")
                .eq(Subject::getBalanceDirection, 1)
                .eq(Subject::getStatus, 1);
        List<Subject> expenseSubjects = subjectMapper.selectList(subjectWrapper);
        Set<Long> expenseSubjectIds = expenseSubjects.stream()
                .map(Subject::getId).collect(Collectors.toSet());

        // 4. 查询本期凭证明细，筛选费用类科目且有部门辅助核算
        // 先找该期间所有凭证ID
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .select(Voucher::getId);
        List<Long> voucherIds = voucherMapper.selectList(voucherWrapper).stream()
                .map(Voucher::getId).collect(Collectors.toList());

        Map<Long, BigDecimal> deptAmountMap = new HashMap<>(); // 部门辅助核算ID -> 借方合计
        BigDecimal totalExpense = BigDecimal.ZERO;

        if (!voucherIds.isEmpty() && !deptItemMap.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .in(VoucherDetail::getAuxiliaryId, deptItemMap.keySet());
            List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

            for (VoucherDetail d : details) {
                if (!expenseSubjectIds.contains(d.getSubjectId())) {
                    continue; // 仅统计费用类科目
                }
                BigDecimal debit = d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO;
                deptAmountMap.merge(d.getAuxiliaryId(), debit, BigDecimal::add);
                totalExpense = totalExpense.add(debit);
            }
        }

        // 5. 构建返回列表
        List<DepartmentExpenseItem> items = new ArrayList<>();
        for (AuxiliaryItem dept : deptItems) {
            DepartmentExpenseItem item = new DepartmentExpenseItem();
            item.setDepartmentId(dept.getId());
            item.setDepartmentCode(dept.getItemCode());
            item.setDepartmentName(dept.getItemName());
            BigDecimal periodAmount = deptAmountMap.getOrDefault(dept.getId(), BigDecimal.ZERO);
            item.setPeriodAmount(periodAmount);
            item.setYearAmount(periodAmount); // 简化：年累计=本期（实际应累计1~month）
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = periodAmount.multiply(new BigDecimal("100"))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP);
                item.setPercentage(pct);
            } else {
                item.setPercentage(BigDecimal.ZERO);
            }
            items.add(item);
        }

        // 按金额降序
        items.sort((a, b) -> b.getPeriodAmount().compareTo(a.getPeriodAmount()));

        vo.setItems(items);
        vo.setTotalExpense(totalExpense);

        log.info("生成部门费用分析报表：accountSetId={}, year={}年{}月, 部门数={}, 费用合计={}",
                accountSetId, year, month, items.size(), totalExpense);
        return vo;
    }
}
