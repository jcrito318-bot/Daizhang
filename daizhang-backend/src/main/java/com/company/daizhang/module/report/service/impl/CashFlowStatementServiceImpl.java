package com.company.daizhang.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.report.enums.CashFlowItem;
import com.company.daizhang.module.report.service.CashFlowStatementService;
import com.company.daizhang.module.report.vo.CashFlowItemVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 现金流量表服务实现（直接法）
 * <p>
 * 实现思路：
 * 1. 查询该期间所有已过账凭证（status=2，与资产负债表/利润表保持一致）
 * 2. 取所有分录，筛选对方科目为现金类科目（1001/1002/1012）的分录
 * 3. 根据对方分录的科目编码，套用映射规则归类到23项标准现金流量项目
 * 4. 现金借方=流入，现金贷方=流出，分别按方向归类
 * 5. 期初现金余额 = 当月 AccountBalance 中现金类科目 beginDebit 合计
 * 6. 期末现金余额 = 当月 AccountBalance 中现金类科目 endDebit 合计
 * 7. 校验：净增加额 = 期末 - 期初 ≈ 经营+投资+筹资净现金流（允许0.01差异）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowStatementServiceImpl implements CashFlowStatementService {

    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final AccountSetAccessService accountSetAccessService;

    /**
     * 凭证已过账状态（与资产负债表/利润表一致，确保三表勾稽基础相同）
     */
    private static final int VOUCHER_STATUS_POSTED = 2;

    /**
     * 勾稽校验允许的最大差异（0.01元，由四舍五入引起）
     */
    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.01");

    @Override
    @Transactional(readOnly = true)
    public CashFlowStatementVO generateCashFlowStatement(Long accountSetId, Integer year, Integer month) {
        accountSetAccessService.checkAccess(accountSetId);

        // 1. 查询当月已过账凭证
        LambdaQueryWrapper<Voucher> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getStatus, VOUCHER_STATUS_POSTED);
        List<Voucher> monthVouchers = voucherMapper.selectList(monthWrapper);
        CashFlowAnalysisResult monthResult = analyzeCashFlowVouchers(monthVouchers);

        // 2. 查询本年累计(1~month)已过账凭证
        LambdaQueryWrapper<Voucher> ytdWrapper = new LambdaQueryWrapper<>();
        ytdWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .le(Voucher::getMonth, month)
                .eq(Voucher::getStatus, VOUCHER_STATUS_POSTED);
        List<Voucher> ytdVouchers = voucherMapper.selectList(ytdWrapper);
        CashFlowAnalysisResult ytdResult = analyzeCashFlowVouchers(ytdVouchers);

        // 3. 计算期初/期末现金余额（从 AccountBalance 取现金类科目余额合计）
        BigDecimal beginningBalance = getCashBalance(accountSetId, year, month, true);
        BigDecimal endingBalance = getCashBalance(accountSetId, year, month, false);

        // 4. 构建 VO
        return buildCashFlowStatementVO(accountSetId, year, month, monthResult, ytdResult,
                beginningBalance, endingBalance);
    }

    // ==================== 凭证分析 ====================

    /**
     * 分析凭证列表，按23项标准现金流量项目归类汇总流入流出金额。
     *
     * @param vouchers 凭证列表
     * @return 分析结果（含各项目金额及分类小计）
     */
    private CashFlowAnalysisResult analyzeCashFlowVouchers(List<Voucher> vouchers) {
        CashFlowAnalysisResult result = new CashFlowAnalysisResult();
        if (vouchers == null || vouchers.isEmpty()) {
            return result;
        }

        List<Long> voucherIds = vouchers.stream()
                .map(Voucher::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (voucherIds.isEmpty()) {
            return result;
        }

        // 查询这些凭证的明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
        List<VoucherDetail> allDetails = voucherDetailMapper.selectList(detailWrapper);

        // 按凭证ID分组
        Map<Long, List<VoucherDetail>> detailsByVoucher = allDetails.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getVoucherId));

        // 逐张凭证分析
        for (Voucher voucher : vouchers) {
            List<VoucherDetail> details = detailsByVoucher.get(voucher.getId());
            if (details == null || details.isEmpty()) {
                continue;
            }
            analyzeSingleVoucher(details, result);
        }
        return result;
    }

    /**
     * 分析单张凭证：分离现金科目行与非现金科目行，按借贷方向归类到现金流量项目。
     * <p>
     * 现金借方发生额 = 流入，看对方贷方科目确定归类；
     * 现金贷方发生额 = 流出，看对方借方科目确定归类。
     * 若凭证全部为现金科目行（如银行转库存现金），则为内部调拨，跳过。
     */
    private void analyzeSingleVoucher(List<VoucherDetail> details, CashFlowAnalysisResult result) {
        List<VoucherDetail> cashLines = new ArrayList<>();
        List<VoucherDetail> nonCashLines = new ArrayList<>();
        for (VoucherDetail d : details) {
            if (isCashSubject(d.getSubjectCode())) {
                cashLines.add(d);
            } else {
                nonCashLines.add(d);
            }
        }

        // 没有现金科目行或没有非现金科目行（纯内部调拨），跳过
        if (cashLines.isEmpty() || nonCashLines.isEmpty()) {
            return;
        }

        // 计算现金流入（现金科目借方合计）和流出（现金科目贷方合计）
        BigDecimal cashInflow = BigDecimal.ZERO;
        BigDecimal cashOutflow = BigDecimal.ZERO;
        for (VoucherDetail cashLine : cashLines) {
            BigDecimal debit = cashLine.getDebit() != null ? cashLine.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = cashLine.getCredit() != null ? cashLine.getCredit() : BigDecimal.ZERO;
            cashInflow = cashInflow.add(debit);
            cashOutflow = cashOutflow.add(credit);
        }

        // 分离对方科目的借方行和贷方行
        List<VoucherDetail> nonCashDebitLines = new ArrayList<>();
        List<VoucherDetail> nonCashCreditLines = new ArrayList<>();
        for (VoucherDetail d : nonCashLines) {
            BigDecimal debit = d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO;
            if (debit.compareTo(BigDecimal.ZERO) > 0) {
                nonCashDebitLines.add(d);
            }
            if (credit.compareTo(BigDecimal.ZERO) > 0) {
                nonCashCreditLines.add(d);
            }
        }

        // 现金流入：根据对方贷方科目归类
        if (cashInflow.compareTo(BigDecimal.ZERO) > 0) {
            CashFlowItem item = determineInflowItem(nonCashCreditLines, nonCashLines);
            if (item != null) {
                result.addAmount(item, cashInflow);
                result.addInflow(item.getCategory(), cashInflow);
            }
        }

        // 现金流出：根据对方借方科目归类
        if (cashOutflow.compareTo(BigDecimal.ZERO) > 0) {
            CashFlowItem item = determineOutflowItem(nonCashDebitLines, nonCashLines);
            if (item != null) {
                result.addAmount(item, cashOutflow);
                result.addOutflow(item.getCategory(), cashOutflow);
            }
        }
    }

    /**
     * 确定现金流入项目：优先从对方贷方科目匹配，无匹配时从全部对方科目匹配，仍无匹配返回默认经营流入项。
     *
     * @param creditLines  对方贷方科目行（优先匹配）
     * @param allNonCash   全部对方科目行（回退匹配）
     * @return 匹配到的现金流量项目，若全部为现金科目则返回 null
     */
    private CashFlowItem determineInflowItem(List<VoucherDetail> creditLines, List<VoucherDetail> allNonCash) {
        // 优先从贷方科目匹配
        CashFlowItem item = matchInflowFromLines(creditLines);
        if (item != null) {
            return item;
        }
        // 回退：从全部对方科目匹配
        item = matchInflowFromLines(allNonCash);
        if (item != null) {
            return item;
        }
        // 默认归入经营活动其他流入
        return CashFlowItem.OTHER_OPERATING_RECEIPTS;
    }

    /**
     * 逐行匹配现金流入项目，返回第一个非默认匹配项。
     */
    private CashFlowItem matchInflowFromLines(List<VoucherDetail> lines) {
        if (lines == null) {
            return null;
        }
        for (VoucherDetail d : lines) {
            CashFlowItem item = mapInflowItem(d.getSubjectCode());
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * 确定现金流出项目：优先从对方借方科目匹配，无匹配时从全部对方科目匹配，仍无匹配返回默认经营流出项。
     */
    private CashFlowItem determineOutflowItem(List<VoucherDetail> debitLines, List<VoucherDetail> allNonCash) {
        CashFlowItem item = matchOutflowFromLines(debitLines);
        if (item != null) {
            return item;
        }
        item = matchOutflowFromLines(allNonCash);
        if (item != null) {
            return item;
        }
        return CashFlowItem.OTHER_OPERATING_PAYMENTS;
    }

    /**
     * 逐行匹配现金流出项目，返回第一个非默认匹配项。
     */
    private CashFlowItem matchOutflowFromLines(List<VoucherDetail> lines) {
        if (lines == null) {
            return null;
        }
        for (VoucherDetail d : lines) {
            CashFlowItem item = mapOutflowItem(d.getSubjectCode());
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    // ==================== 科目→现金流量项目映射规则 ====================

    /**
     * 现金流入时，根据对方科目编码映射到现金流量项目。
     * 对方科目在贷方（现金借方=流入），如收回应收、确认收入、取得借款等。
     *
     * @param code 对方科目编码
     * @return 对应的现金流量项目，无匹配返回 null（由调用方处理默认值）
     */
    private CashFlowItem mapInflowItem(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        // 经营活动 - 销售商品、提供劳务收到的现金
        // 1121 应收账款、1122 应收票据、1123 预付账款（贷方=收回应收）
        // 5001 主营业务收入、5051 其他业务收入（贷方=确认收入）
        if (code.startsWith("1121") || code.startsWith("1122") || code.startsWith("1123")
                || code.startsWith("5001") || code.startsWith("5051")) {
            return CashFlowItem.SALES_RECEIPTS;
        }
        // 经营活动 - 收到的税费返还
        // 2221 应交税费（贷方=收到退税）
        if (code.startsWith("2221")) {
            return CashFlowItem.TAX_REFUNDS;
        }
        // 投资活动 - 收回投资收到的现金
        // 1101 交易性金融资产、1501 持有至到期投资、1511 长期股权投资（贷方=收回投资）
        if (code.startsWith("1101") || code.startsWith("1501") || code.startsWith("1511")) {
            return CashFlowItem.INVESTMENT_RECEIPTS;
        }
        // 投资活动 - 取得投资收益收到的现金
        // 6111 投资收益（贷方=收到投资收益）
        if (code.startsWith("6111")) {
            return CashFlowItem.INVESTMENT_INCOME;
        }
        // 投资活动 - 处置固定资产、无形资产和其他长期资产收回的现金净额
        // 1601 固定资产、1602 累计折旧、1606 固定资产清理、1701 无形资产、1702 累计摊销（贷方=处置资产）
        if (code.startsWith("1601") || code.startsWith("1602") || code.startsWith("1606")
                || code.startsWith("1701") || code.startsWith("1702")) {
            return CashFlowItem.ASSET_DISPOSAL;
        }
        // 筹资活动 - 吸收投资收到的现金
        // 4001 实收资本、4002 资本公积（贷方=收到投资）
        if (code.startsWith("4001") || code.startsWith("4002")) {
            return CashFlowItem.FINANCING_RECEIPTS;
        }
        // 筹资活动 - 取得借款收到的现金
        // 2001 短期借款、2501 长期借款（贷方=取得借款）
        if (code.startsWith("2001") || code.startsWith("2501")) {
            return CashFlowItem.LOAN_RECEIPTS;
        }
        // 无匹配前缀，返回 null 由调用方归入默认项
        return null;
    }

    /**
     * 现金流出时，根据对方科目编码映射到现金流量项目。
     * 对方科目在借方（现金贷方=流出），如支付应付、购建资产、偿还借款等。
     *
     * @param code 对方科目编码
     * @return 对应的现金流量项目，无匹配返回 null（由调用方处理默认值）
     */
    private CashFlowItem mapOutflowItem(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        // 经营活动 - 购买商品、接受劳务支付的现金
        // 2202 应付账款、2203 应付票据（借方=支付货款）
        // 1401 材料采购、1403 原材料、1405 库存商品（借方=购买存货）
        if (code.startsWith("2202") || code.startsWith("2203")
                || code.startsWith("1401") || code.startsWith("1403") || code.startsWith("1405")) {
            return CashFlowItem.PURCHASE_PAYMENTS;
        }
        // 经营活动 - 支付给职工以及为职工支付的现金
        // 2211 应付职工薪酬（借方=支付薪酬）
        if (code.startsWith("2211")) {
            return CashFlowItem.EMPLOYEE_PAYMENTS;
        }
        // 经营活动 - 支付的各项税费
        // 2221 应交税费（借方=支付税费）
        if (code.startsWith("2221")) {
            return CashFlowItem.TAX_PAYMENTS;
        }
        // 投资活动 - 购建固定资产、无形资产和其他长期资产支付的现金
        // 1601 固定资产、1604 在建工程、1701 无形资产、1605 工程物资（借方=购建资产）
        if (code.startsWith("1601") || code.startsWith("1604") || code.startsWith("1605")
                || code.startsWith("1701")) {
            return CashFlowItem.ASSET_PURCHASE;
        }
        // 投资活动 - 投资支付的现金
        // 1101 交易性金融资产、1501 持有至到期投资、1511 长期股权投资（借方=支付投资）
        if (code.startsWith("1101") || code.startsWith("1501") || code.startsWith("1511")) {
            return CashFlowItem.INVESTMENT_PAYMENTS;
        }
        // 筹资活动 - 偿还债务支付的现金
        // 2001 短期借款、2501 长期借款（借方=偿还借款）
        if (code.startsWith("2001") || code.startsWith("2501")) {
            return CashFlowItem.DEBT_REPAYMENT;
        }
        // 筹资活动 - 分配股利、利润或偿付利息支付的现金
        // 2231 应付利息、2232 应付股利、4103 利润分配（借方=分配股利/偿付利息）
        // 5603 财务费用（借方=支付利息）
        if (code.startsWith("2231") || code.startsWith("2232")
                || code.startsWith("4103") || code.startsWith("5603")) {
            return CashFlowItem.DISTRIBUTION_PAYMENTS;
        }
        // 无匹配前缀，返回 null 由调用方归入默认项
        return null;
    }

    // ==================== 现金余额计算 ====================

    /**
     * 获取现金类科目余额合计。
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @param isBeginning  true=期初余额(beginDebit)，false=期末余额(endDebit)
     * @return 现金类科目余额合计
     */
    private BigDecimal getCashBalance(Long accountSetId, Integer year, Integer month, boolean isBeginning) {
        // 查询当月科目余额
        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(wrapper);

        // 批量查询该账套的科目，筛选现金类科目ID集合
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        // 筛选现金类科目ID集合
        Set<Long> cashSubjectIds = subjects.stream()
                .filter(s -> isCashSubject(s.getCode()))
                .map(Subject::getId)
                .collect(Collectors.toSet());

        if (cashSubjectIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 汇总现金类科目余额
        BigDecimal total = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            if (balance.getSubjectId() != null && cashSubjectIds.contains(balance.getSubjectId())) {
                BigDecimal debit;
                BigDecimal credit;
                if (isBeginning) {
                    debit = balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO;
                    credit = balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO;
                } else {
                    debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                    credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                }
                // 现金类科目为资产，正常余额在借方；净余额 = 借方 - 贷方
                total = total.add(debit).subtract(credit);
            }
        }
        return total;
    }

    // ==================== VO 构建 ====================

    /**
     * 构建现金流量表 VO（当月 + 本年累计 + 余额 + 校验）。
     */
    private CashFlowStatementVO buildCashFlowStatementVO(Long accountSetId, Integer year, Integer month,
                                                          CashFlowAnalysisResult monthResult,
                                                          CashFlowAnalysisResult ytdResult,
                                                          BigDecimal beginningBalance,
                                                          BigDecimal endingBalance) {
        // 当月各活动净现金流
        BigDecimal operatingNetCashFlow = monthResult.operatingInflow.subtract(monthResult.operatingOutflow);
        BigDecimal investingNetCashFlow = monthResult.investingInflow.subtract(monthResult.investingOutflow);
        BigDecimal financingNetCashFlow = monthResult.financingInflow.subtract(monthResult.financingOutflow);
        // 汇率变动影响（暂不支持外币，固定为0）
        BigDecimal exchangeEffect = BigDecimal.ZERO;
        // 现金及现金等价物净增加额 = 经营 + 投资 + 筹资 + 汇率变动
        BigDecimal netIncreaseInCash = operatingNetCashFlow
                .add(investingNetCashFlow)
                .add(financingNetCashFlow)
                .add(exchangeEffect);

        // 本年累计各活动净现金流
        BigDecimal operatingNetCashFlowYear = ytdResult.operatingInflow.subtract(ytdResult.operatingOutflow);
        BigDecimal investingNetCashFlowYear = ytdResult.investingInflow.subtract(ytdResult.investingOutflow);
        BigDecimal financingNetCashFlowYear = ytdResult.financingInflow.subtract(ytdResult.financingOutflow);
        BigDecimal netIncreaseInCashYear = operatingNetCashFlowYear
                .add(investingNetCashFlowYear)
                .add(financingNetCashFlowYear)
                .add(exchangeEffect);

        // 勾稽校验：净增加额 ≈ 期末 - 期初（允许0.01差异）
        BigDecimal balanceDiff = endingBalance.subtract(beginningBalance);
        BigDecimal checkDiff = netIncreaseInCash.subtract(balanceDiff).abs();
        boolean balanceCheck = checkDiff.compareTo(BALANCE_TOLERANCE) <= 0;
        if (!balanceCheck) {
            log.warn("现金流量表勾稽不平。accountSetId={}, {}年{}月, 净增加额={}, 期末-期初={}, 差异={}",
                    accountSetId, year, month, netIncreaseInCash, balanceDiff, checkDiff);
        }

        // 构建23项明细列表（按枚举声明顺序）
        List<CashFlowItemVO> items = new ArrayList<>();
        for (CashFlowItem item : CashFlowItem.values()) {
            CashFlowItemVO vo = new CashFlowItemVO();
            vo.setItemCode(item.name());
            vo.setItemName(item.getItemName());
            vo.setCategory(item.getCategory());
            vo.setDirection(item.getDirection());
            BigDecimal amount;
            switch (item) {
                case EXCHANGE_EFFECT:
                    amount = exchangeEffect;
                    break;
                case CASH_BEGINNING:
                    amount = beginningBalance;
                    break;
                case CASH_ENDING:
                    amount = endingBalance;
                    break;
                default:
                    amount = monthResult.getAmount(item);
                    break;
            }
            vo.setAmount(amount);
            items.add(vo);
        }

        // 组装 VO
        CashFlowStatementVO vo = new CashFlowStatementVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setAccountSetId(accountSetId);
        vo.setItems(items);

        // 当月金额（兼容旧字段 + 新字段）
        vo.setOperatingInflow(monthResult.operatingInflow);
        vo.setOperatingOutflow(monthResult.operatingOutflow);
        vo.setOperatingNetFlow(operatingNetCashFlow);
        vo.setInvestingInflow(monthResult.investingInflow);
        vo.setInvestingOutflow(monthResult.investingOutflow);
        vo.setInvestingNetFlow(investingNetCashFlow);
        vo.setFinancingInflow(monthResult.financingInflow);
        vo.setFinancingOutflow(monthResult.financingOutflow);
        vo.setFinancingNetFlow(financingNetCashFlow);
        vo.setNetIncrease(netIncreaseInCash);

        // 新增准则术语字段
        vo.setOperatingNetCashFlow(operatingNetCashFlow);
        vo.setInvestingNetCashFlow(investingNetCashFlow);
        vo.setFinancingNetCashFlow(financingNetCashFlow);
        vo.setNetIncreaseInCash(netIncreaseInCash);
        vo.setBeginningCashBalance(beginningBalance);
        vo.setEndingCashBalance(endingBalance);
        vo.setExchangeEffect(exchangeEffect);
        vo.setBalanceCheck(balanceCheck);

        // 本年累计金额
        vo.setOperatingInflowYear(ytdResult.operatingInflow);
        vo.setOperatingOutflowYear(ytdResult.operatingOutflow);
        vo.setOperatingNetFlowYear(operatingNetCashFlowYear);
        vo.setInvestingInflowYear(ytdResult.investingInflow);
        vo.setInvestingOutflowYear(ytdResult.investingOutflow);
        vo.setInvestingNetFlowYear(investingNetCashFlowYear);
        vo.setFinancingInflowYear(ytdResult.financingInflow);
        vo.setFinancingOutflowYear(ytdResult.financingOutflow);
        vo.setFinancingNetFlowYear(financingNetCashFlowYear);
        vo.setNetIncreaseYear(netIncreaseInCashYear);

        return vo;
    }

    // ==================== 工具方法 ====================

    /**
     * 判断是否为现金类科目（1001库存现金、1002银行存款、1012其他货币资金）。
     * <p>
     * 注：1101交易性金融资产中的短期理财部分理论上属于现金等价物，
     * 但因无法通过科目编码区分短期理财与短期投资，暂不纳入现金类科目范围，
     * 避免与投资活动分类冲突。如需支持可通过辅助核算或子科目区分后扩展。
     */
    private boolean isCashSubject(String code) {
        return code != null && !code.isEmpty()
                && (code.startsWith("1001") || code.startsWith("1002") || code.startsWith("1012"));
    }

    // ==================== 分析结果中间结构 ====================

    /**
     * 现金流量分析中间结果：承载某一区间凭证分析后的各项目金额及分类小计。
     * 当月与本年累计各持一份实例。
     */
    private static class CashFlowAnalysisResult {
        /** 各现金流量项目金额（23项） */
        final Map<CashFlowItem, BigDecimal> itemAmounts = new EnumMap<>(CashFlowItem.class);

        BigDecimal operatingInflow = BigDecimal.ZERO;
        BigDecimal operatingOutflow = BigDecimal.ZERO;
        BigDecimal investingInflow = BigDecimal.ZERO;
        BigDecimal investingOutflow = BigDecimal.ZERO;
        BigDecimal financingInflow = BigDecimal.ZERO;
        BigDecimal financingOutflow = BigDecimal.ZERO;

        /** 累加项目金额 */
        void addAmount(CashFlowItem item, BigDecimal amount) {
            itemAmounts.merge(item, amount, BigDecimal::add);
        }

        /** 累加分类流入 */
        void addInflow(String category, BigDecimal amount) {
            switch (category) {
                case "operating":
                    operatingInflow = operatingInflow.add(amount);
                    break;
                case "investing":
                    investingInflow = investingInflow.add(amount);
                    break;
                case "financing":
                    financingInflow = financingInflow.add(amount);
                    break;
                default:
                    break;
            }
        }

        /** 累加分类流出 */
        void addOutflow(String category, BigDecimal amount) {
            switch (category) {
                case "operating":
                    operatingOutflow = operatingOutflow.add(amount);
                    break;
                case "investing":
                    investingOutflow = investingOutflow.add(amount);
                    break;
                case "financing":
                    financingOutflow = financingOutflow.add(amount);
                    break;
                default:
                    break;
            }
        }

        /** 获取项目金额，未发生返回0 */
        BigDecimal getAmount(CashFlowItem item) {
            return itemAmounts.getOrDefault(item, BigDecimal.ZERO);
        }
    }
}
