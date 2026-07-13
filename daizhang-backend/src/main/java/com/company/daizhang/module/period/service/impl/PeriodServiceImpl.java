package com.company.daizhang.module.period.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.enums.PeriodStatus;
import com.company.daizhang.common.enums.VoucherStatus;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.period.dto.TrialBalanceRequest;
import com.company.daizhang.module.period.service.PeriodService;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceVO;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 期末处理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements PeriodService {

    private final AccountPeriodMapper accountPeriodMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final AccountSetMapper accountSetMapper;

    /**
     * 生成结转凭证号：格式 year-month-sequence，基于本期最大序号+1。
     * 使用max+1而非count+1，避免期间存在断号时重号冲突（唯一索引报错导致结转失败）。
     */
    private String generateCarryVoucherNo(Long accountSetId, int year, int month) {
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

    @Override
    public TrialBalanceResultVO trialBalance(TrialBalanceRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        log.info("试算平衡，账套ID：{}，期间：{}-{}", accountSetId, year, month);

        // 查询该账套的所有科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        // 查询该期间的科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                     .eq(AccountBalance::getYear, year)
                     .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 转换为Map便于查找
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 构建试算平衡项
        List<TrialBalanceVO> items = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Subject subject : subjects) {
            AccountBalance balance = balanceMap.get(subject.getId());

            TrialBalanceVO item = new TrialBalanceVO();
            item.setSubjectCode(subject.getCode());
            item.setSubjectName(subject.getName());

            if (balance != null) {
                item.setDebitBalance(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
                item.setCreditBalance(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);
            } else {
                item.setDebitBalance(BigDecimal.ZERO);
                item.setCreditBalance(BigDecimal.ZERO);
            }

            totalDebit = totalDebit.add(item.getDebitBalance());
            totalCredit = totalCredit.add(item.getCreditBalance());

            items.add(item);
        }

        // 构建结果
        TrialBalanceResultVO result = new TrialBalanceResultVO();
        result.setItems(items);
        result.setTotalDebit(totalDebit);
        result.setTotalCredit(totalCredit);
        result.setBalanced(totalDebit.compareTo(totalCredit) == 0);

        log.info("试算平衡结果：借方合计={}，贷方合计={}，是否平衡={}", totalDebit, totalCredit, result.isBalanced());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClosePeriodResultVO closePeriod(Long accountSetId, int year, int month) {
        log.info("期末结账，账套ID：{}，期间：{}-{}", accountSetId, year, month);
        ClosePeriodResultVO result = new ClosePeriodResultVO();

        // 1. 查询会计期间
        AccountPeriod period = getPeriod(accountSetId, year, month);
        if (PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
            throw new BusinessException(ErrorCode.PERIOD_ALREADY_CLOSED);
        }

        // 2. 检查本期凭证是否全部审核
        LambdaQueryWrapper<Voucher> unauditWrapper = new LambdaQueryWrapper<>();
        unauditWrapper.eq(Voucher::getAccountSetId, accountSetId)
                     .eq(Voucher::getYear, year)
                     .eq(Voucher::getMonth, month)
                     .eq(Voucher::getStatus, VoucherStatus.UNAUDITED.getCode());
        long unauditedCount = voucherMapper.selectCount(unauditWrapper);
        if (unauditedCount > 0) {
            result.setSuccess(false);
            result.setMessage("存在" + unauditedCount + "张未审核的凭证，无法结账");
            result.setUncheckedVouchers((int) unauditedCount);
            return result;
        }

        // 3. 检查本期凭证是否全部过账
        // 仅统计"未审核(0)+已审核(1)"的凭证,作废凭证(3)不应阻塞结账,
        // 否则用户作废一张凭证后该期间永远无法结账(原 .ne(POSTED) 会把作废凭证计入未过账数)
        LambdaQueryWrapper<Voucher> unpostWrapper = new LambdaQueryWrapper<>();
        unpostWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .in(Voucher::getStatus, VoucherStatus.UNAUDITED.getCode(), VoucherStatus.AUDITED.getCode());
        long unpostedCount = voucherMapper.selectCount(unpostWrapper);
        if (unpostedCount > 0) {
            result.setSuccess(false);
            result.setMessage("存在" + unpostedCount + "张未过账的凭证，无法结账");
            result.setUncheckedVouchers((int) unpostedCount);
            return result;
        }

        // 4. 检查试算平衡是否通过
        TrialBalanceRequest tbRequest = new TrialBalanceRequest();
        tbRequest.setAccountSetId(accountSetId);
        tbRequest.setYear(year);
        tbRequest.setMonth(month);
        TrialBalanceResultVO trialBalance = trialBalance(tbRequest);
        if (!trialBalance.isBalanced()) {
            result.setSuccess(false);
            result.setMessage("试算不平衡（借方合计：" + trialBalance.getTotalDebit() + "，贷方合计：" + trialBalance.getTotalCredit() + "），无法结账");
            result.setUncheckedVouchers(0);
            return result;
        }

        // 5. 校验本期损益类科目是否已结转
        // 试算平衡在损益类科目有余额时仍成立(借贷必等),但资产负债表"资产=负债+所有者权益"会不平衡(差额=未结转的净利润)
        // 结账前需校验损益类科目(收入类/费用类)期末余额已清零(已结转至本年利润)
        LambdaQueryWrapper<Subject> profitLossSubjectWrapper = new LambdaQueryWrapper<>();
        profitLossSubjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                                .eq(Subject::getCategory, "损益");
        List<Subject> profitLossSubjects = subjectMapper.selectList(profitLossSubjectWrapper);
        if (!profitLossSubjects.isEmpty()) {
            List<Long> profitLossSubjectIds = profitLossSubjects.stream()
                    .map(Subject::getId).collect(Collectors.toList());
            LambdaQueryWrapper<AccountBalance> profitLossBalanceWrapper = new LambdaQueryWrapper<>();
            profitLossBalanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                                    .eq(AccountBalance::getYear, year)
                                    .eq(AccountBalance::getMonth, month)
                                    .in(AccountBalance::getSubjectId, profitLossSubjectIds);
            List<AccountBalance> profitLossBalances = accountBalanceMapper.selectList(profitLossBalanceWrapper);
            for (AccountBalance balance : profitLossBalances) {
                BigDecimal endDebit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                BigDecimal endCredit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                if (endDebit.compareTo(BigDecimal.ZERO) != 0 || endCredit.compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "本期损益类科目未结转，请先执行损益结转后再结账");
                }
            }
        }

        // 6. 更新会计期间状态为已结账
        // 使用条件update(status=原值)模拟乐观锁,防止双用户并发结账同一期间时同时通过校验
        period.setStatus(PeriodStatus.CLOSED.getCode());
        period.setCloseBy(SecurityUtils.getCurrentUserId());
        period.setCloseTime(LocalDateTime.now());
        int affected = accountPeriodMapper.update(period,
                new LambdaUpdateWrapper<AccountPeriod>()
                        .eq(AccountPeriod::getId, period.getId())
                        .eq(AccountPeriod::getStatus, PeriodStatus.OPEN.getCode()));
        if (affected == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "期间状态已变更(并发冲突)，请刷新后重试");
        }

        log.info("期末结账成功，账套ID：{}，期间：{}-{}", accountSetId, year, month);

        result.setSuccess(true);
        result.setMessage("结账成功");
        result.setUncheckedVouchers(0);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenPeriod(Long accountSetId, int year, int month) {
        log.info("期末反结账，账套ID：{}，期间：{}-{}", accountSetId, year, month);

        // 1. 查询当前期间
        AccountPeriod period = getPeriod(accountSetId, year, month);
        if (!PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_CLOSED);
        }

        // 2. 检查是否有已结账的后续期间
        LambdaQueryWrapper<AccountPeriod> subsequentWrapper = new LambdaQueryWrapper<>();
        subsequentWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                        .eq(AccountPeriod::getStatus, PeriodStatus.CLOSED.getCode())
                        .and(w -> w
                                .gt(AccountPeriod::getYear, year)
                                .or(w2 -> w2.eq(AccountPeriod::getYear, year)
                                            .gt(AccountPeriod::getMonth, month)));
        long subsequentClosedCount = accountPeriodMapper.selectCount(subsequentWrapper);
        if (subsequentClosedCount > 0) {
            throw new BusinessException(ErrorCode.PERIOD_HAS_SUBSEQUENT_CLOSED);
        }

        // 3. 更新会计期间状态为未结账，解锁本期数据
        // 使用LambdaUpdateWrapper显式set null，避免MyBatis-Plus默认NOT_NULL策略不更新null字段
        // 带条件(status=已结账)模拟乐观锁,防止双用户并发反结账同一期间时同时通过校验
        LambdaUpdateWrapper<AccountPeriod> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AccountPeriod::getId, period.getId())
                     .eq(AccountPeriod::getStatus, PeriodStatus.CLOSED.getCode())
                     .set(AccountPeriod::getStatus, PeriodStatus.OPEN.getCode())
                     .set(AccountPeriod::getCloseBy, null)
                     .set(AccountPeriod::getCloseTime, null);
        int affected = accountPeriodMapper.update(null, updateWrapper);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "期间状态已变更(并发冲突)，请刷新后重试");
        }

        // 已知限制：反结账未自动清理本期系统生成的结转凭证（source=1，含损益结转/成本结转）。
        // 这些凭证已过账（status=2）并更新了科目余额（损益科目清零、本年利润累加等），
        // 直接删除会导致余额混乱；安全删除需先按结转逻辑反向回滚余额，逻辑复杂且风险较高，故未自动清理。
        // 如需修改本期凭证请先手动删除结转凭证并调整余额，否则重新结转会撞幂等校验。
        LambdaQueryWrapper<Voucher> carryVoucherWrapper = new LambdaQueryWrapper<>();
        carryVoucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getSource, 1);
        Long carryVoucherCount = voucherMapper.selectCount(carryVoucherWrapper);
        if (carryVoucherCount != null && carryVoucherCount > 0) {
            log.warn("反结账告警：账套ID={}，期间={}-{} 存在{}张系统结转凭证（source=1），反结账未自动清理。"
                            + "如需修改本期凭证请先手动删除结转凭证并调整余额，否则重新结转会撞幂等校验",
                    accountSetId, year, month, carryVoucherCount);
        }

        log.info("期末反结账成功，账套ID：{}，期间：{}-{}", accountSetId, year, month);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void carryForward(Long accountSetId, int year, int month) {
        log.info("损益结转，账套ID：{}，期间：{}-{}", accountSetId, year, month);

        // 1. 查询该期间
        AccountPeriod period = getPeriod(accountSetId, year, month);

        // 1.0 期间状态校验:已结账期间余额已固化,不允许再执行损益结转
        if (PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
            throw new BusinessException(ErrorCode.PERIOD_ALREADY_CLOSED);
        }

        // 1.1 幂等校验:检查本期是否已存在系统生成的损益结转凭证,避免重复结转
        LambdaQueryWrapper<Voucher> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Voucher::getAccountSetId, accountSetId)
                   .eq(Voucher::getYear, year)
                   .eq(Voucher::getMonth, month)
                   .eq(Voucher::getSource, 1);
        List<Voucher> existVouchers = voucherMapper.selectList(existWrapper);
        if (!existVouchers.isEmpty()) {
            List<Long> voucherIds = existVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> carryDetailWrapper = new LambdaQueryWrapper<>();
            carryDetailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .eq(VoucherDetail::getSummary, "结转本年利润");
            Long carryCount = voucherDetailMapper.selectCount(carryDetailWrapper);
            if (carryCount != null && carryCount > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "本期" + year + "年" + month + "月已执行损益结转,不能重复结转");
            }
        }

        // 2. 查询收入类科目（损益类且贷方余额，balance_direction=2）
        LambdaQueryWrapper<Subject> revenueWrapper = new LambdaQueryWrapper<>();
        revenueWrapper.eq(Subject::getAccountSetId, accountSetId)
                     .eq(Subject::getCategory, "损益")
                     .eq(Subject::getBalanceDirection, 2);
        List<Subject> revenueSubjects = subjectMapper.selectList(revenueWrapper);

        // 3. 查询费用类科目（损益类且借方余额，balance_direction=1）
        LambdaQueryWrapper<Subject> expenseWrapper = new LambdaQueryWrapper<>();
        expenseWrapper.eq(Subject::getAccountSetId, accountSetId)
                     .eq(Subject::getCategory, "损益")
                     .eq(Subject::getBalanceDirection, 1);
        List<Subject> expenseSubjects = subjectMapper.selectList(expenseWrapper);

        // 4. 查询本年利润科目（标准科目代码3103）
        LambdaQueryWrapper<Subject> profitWrapper = new LambdaQueryWrapper<>();
        profitWrapper.eq(Subject::getAccountSetId, accountSetId)
                    .eq(Subject::getCode, "3103");
        Subject profitSubject = subjectMapper.selectOne(profitWrapper);
        if (profitSubject == null) {
            throw new BusinessException(ErrorCode.PROFIT_SUBJECT_NOT_FOUND);
        }

        // 5. 查询该期间的所有科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                     .eq(AccountBalance::getYear, year)
                     .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        // 6. 计算收入总额（按净额: endCredit - endDebit,处理销售退回等借方余额）
        // 收入净额可能为正(正常收入)或负(销售退回),totalRevenue累加全部净额保证借贷平衡
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<Long, BigDecimal> revenueNetMap = new HashMap<>();
        for (Subject subject : revenueSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                BigDecimal credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                BigDecimal debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                BigDecimal net = credit.subtract(debit);
                if (net.compareTo(BigDecimal.ZERO) != 0) {
                    revenueNetMap.put(subject.getId(), net);
                    totalRevenue = totalRevenue.add(net);
                }
            }
        }

        // 7. 计算费用总额（按净额: endDebit - endCredit,处理费用红字冲销等贷方余额）
        // 费用净额可能为正(正常费用)或负(红字冲销),totalExpense累加全部净额保证借贷平衡
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<Long, BigDecimal> expenseNetMap = new HashMap<>();
        for (Subject subject : expenseSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                BigDecimal debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                BigDecimal credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                BigDecimal net = debit.subtract(credit);
                if (net.compareTo(BigDecimal.ZERO) != 0) {
                    expenseNetMap.put(subject.getId(), net);
                    totalExpense = totalExpense.add(net);
                }
            }
        }

        // 如果没有损益发生，无需结转
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0 && totalExpense.compareTo(BigDecimal.ZERO) == 0
                && revenueNetMap.isEmpty() && expenseNetMap.isEmpty()) {
            log.info("本期无损益发生，无需结转");
            return;
        }

        // 8. 生成结转凭证
        // 利润 = 收入净额合计 - 费用净额合计(含正负值,保证借贷平衡)
        BigDecimal profit = totalRevenue.subtract(totalExpense);
        // 凭证借贷合计:借方=正向收入净额+正向(负费用净额abs)+亏损abs;贷方=正向费用净额+负向收入净额abs+盈利
        // 简化:totalAmount取借方合计=正向收入净额合计 + 负向费用净额abs合计 + (亏损?亏损abs:0)
        // 由于复式记账借贷必等,直接用借方合计作为totalDebit和totalCredit
        BigDecimal positiveRevenue = BigDecimal.ZERO;
        BigDecimal negativeRevenueAbs = BigDecimal.ZERO;
        for (BigDecimal net : revenueNetMap.values()) {
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                positiveRevenue = positiveRevenue.add(net);
            } else {
                negativeRevenueAbs = negativeRevenueAbs.add(net.abs());
            }
        }
        BigDecimal positiveExpense = BigDecimal.ZERO;
        BigDecimal negativeExpenseAbs = BigDecimal.ZERO;
        for (BigDecimal net : expenseNetMap.values()) {
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                positiveExpense = positiveExpense.add(net);
            } else {
                negativeExpenseAbs = negativeExpenseAbs.add(net.abs());
            }
        }
        // 借方合计 = 正向收入(借记) + 负向费用abs(借记) + 亏损abs(借记本年利润)
        // 贷方合计 = 正向费用(贷记) + 负向收入abs(贷记) + 盈利(贷记本年利润)
        // 两者必然相等(复式记账平衡)
        BigDecimal totalAmount = positiveRevenue.add(negativeExpenseAbs);
        if (profit.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = totalAmount.add(profit.abs());
        } else {
            // 盈利时贷方多出profit,借方需包含其他项使两者相等
            // 实际借方=正向收入+负向费用abs, 贷方=正向费用+负向收入abs+profit
            // 验证: 借-贷 = (正向收入+负向费用abs) - (正向费用+负向收入abs+profit)
            //       = (正向收入-负向收入abs) - (正向费用-负向费用abs) - profit
            //       = totalRevenue - totalExpense - profit = 0 (因profit=totalRevenue-totalExpense)
            // 所以totalAmount=借方合计=正向收入+负向费用abs,贷方=正向费用+负向收入abs+profit,两者相等
        }

        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherDate(period.getEndDate());
        voucher.setYear(year);
        voucher.setMonth(month);
        voucher.setStatus(VoucherStatus.POSTED.getCode());
        voucher.setSource(1); // 系统生成
        voucher.setTotalDebit(totalAmount);
        voucher.setTotalCredit(totalAmount);
        voucher.setAttachmentCount(0);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        voucher.setAuditBy(currentUserId);
        voucher.setAuditTime(LocalDateTime.now());
        voucher.setPostBy(currentUserId);
        voucher.setPostTime(LocalDateTime.now());

        // 生成凭证号（基于本期最大序号+1，避免断号时count+1导致重号冲突）
        voucher.setVoucherNo(generateCarryVoucherNo(accountSetId, year, month));

        voucherMapper.insert(voucher);

        // 9. 凭证明细：收入科目结转（借记收入科目净额,将其余额转为0;反向余额则贷记）
        int lineNo = 1;
        for (Subject subject : revenueSubjects) {
            BigDecimal netAmount = revenueNetMap.get(subject.getId());
            if (netAmount == null) {
                continue;
            }
            VoucherDetail detail = new VoucherDetail();
            detail.setVoucherId(voucher.getId());
            detail.setLineNo(lineNo);
            detail.setSummary("结转收入");
            detail.setSubjectId(subject.getId());
            detail.setSubjectCode(subject.getCode());
            detail.setSubjectName(subject.getName());
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 贷方净余额:借记结转
                detail.setDebit(netAmount);
                detail.setCredit(BigDecimal.ZERO);
            } else {
                // 借方净余额(销售退回等):贷记结转
                detail.setDebit(BigDecimal.ZERO);
                detail.setCredit(netAmount.abs());
            }
            detail.setSortOrder(lineNo);
            voucherDetailMapper.insert(detail);
            lineNo++;
        }

        // 10. 凭证明细：费用科目结转（贷记费用科目净额,将其余额转为0;反向余额则借记）
        for (Subject subject : expenseSubjects) {
            BigDecimal netAmount = expenseNetMap.get(subject.getId());
            if (netAmount == null) {
                continue;
            }
            VoucherDetail detail = new VoucherDetail();
            detail.setVoucherId(voucher.getId());
            detail.setLineNo(lineNo);
            detail.setSummary("结转费用");
            detail.setSubjectId(subject.getId());
            detail.setSubjectCode(subject.getCode());
            detail.setSubjectName(subject.getName());
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 借方净余额:贷记结转
                detail.setDebit(BigDecimal.ZERO);
                detail.setCredit(netAmount);
            } else {
                // 贷方净余额(红字冲销等):借记结转
                detail.setDebit(netAmount.abs());
                detail.setCredit(BigDecimal.ZERO);
            }
            detail.setSortOrder(lineNo);
            voucherDetailMapper.insert(detail);
            lineNo++;
        }

        // 11. 凭证明细：本年利润
        VoucherDetail profitDetail = new VoucherDetail();
        profitDetail.setVoucherId(voucher.getId());
        profitDetail.setLineNo(lineNo);
        profitDetail.setSummary("结转本年利润");
        profitDetail.setSubjectId(profitSubject.getId());
        profitDetail.setSubjectCode(profitSubject.getCode());
        profitDetail.setSubjectName(profitSubject.getName());
        if (profit.compareTo(BigDecimal.ZERO) >= 0) {
            // 盈利：贷记本年利润
            profitDetail.setDebit(BigDecimal.ZERO);
            profitDetail.setCredit(profit);
        } else {
            // 亏损：借记本年利润
            profitDetail.setDebit(profit.abs());
            profitDetail.setCredit(BigDecimal.ZERO);
        }
        profitDetail.setSortOrder(lineNo);
        voucherDetailMapper.insert(profitDetail);

        // 12. 更新损益类科目余额为0（按净额结转后,期末借贷余额清零）
        for (Subject subject : revenueSubjects) {
            BigDecimal netAmount = revenueNetMap.get(subject.getId());
            if (netAmount == null) {
                continue;
            }
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                BigDecimal carryAmount = netAmount.abs();
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 贷方净余额:结转凭证借记,累加本期借方发生额+本年累计借方
                    BigDecimal newPeriodDebit = (balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setPeriodDebit(newPeriodDebit);
                    BigDecimal newYearDebit = (balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setYearDebit(newYearDebit);
                } else {
                    // 借方净余额:结转凭证贷记,累加本期贷方发生额+本年累计贷方
                    BigDecimal newPeriodCredit = (balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setPeriodCredit(newPeriodCredit);
                    BigDecimal newYearCredit = (balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setYearCredit(newYearCredit);
                }
                balance.setEndDebit(BigDecimal.ZERO);
                balance.setEndCredit(BigDecimal.ZERO);
                accountBalanceMapper.updateById(balance);
            }
        }
        for (Subject subject : expenseSubjects) {
            BigDecimal netAmount = expenseNetMap.get(subject.getId());
            if (netAmount == null) {
                continue;
            }
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                BigDecimal carryAmount = netAmount.abs();
                if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 借方净余额:结转凭证贷记,累加本期贷方发生额+本年累计贷方
                    BigDecimal newPeriodCredit = (balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setPeriodCredit(newPeriodCredit);
                    BigDecimal newYearCredit = (balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setYearCredit(newYearCredit);
                } else {
                    // 贷方净余额:结转凭证借记,累加本期借方发生额+本年累计借方
                    BigDecimal newPeriodDebit = (balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setPeriodDebit(newPeriodDebit);
                    BigDecimal newYearDebit = (balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO).add(carryAmount);
                    balance.setYearDebit(newYearDebit);
                }
                balance.setEndDebit(BigDecimal.ZERO);
                balance.setEndCredit(BigDecimal.ZERO);
                accountBalanceMapper.updateById(balance);
            }
        }

        // 13. 更新本年利润科目余额（若不存在则创建）
        AccountBalance profitBalance = balanceMap.get(profitSubject.getId());
        if (profitBalance == null) {
            profitBalance = new AccountBalance();
            profitBalance.setAccountSetId(accountSetId);
            profitBalance.setSubjectId(profitSubject.getId());
            profitBalance.setYear(year);
            profitBalance.setMonth(month);
            // 期初从上一期间期末结转,同年内结转本年累计
            // 原实现begin/year全置零,丢失累计利润,
            // 导致3103(本年利润)余额每月从0开始,资产负债表"本年利润"只显示当月利润而非本年累计,试算不平衡
            int lastYear = (month == 1) ? year - 1 : year;
            int lastMonth = (month == 1) ? 12 : month - 1;
            LambdaQueryWrapper<AccountBalance> lastProfitWrapper = new LambdaQueryWrapper<>();
            lastProfitWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                       .eq(AccountBalance::getSubjectId, profitSubject.getId())
                       .eq(AccountBalance::getYear, lastYear)
                       .eq(AccountBalance::getMonth, lastMonth);
            AccountBalance lastProfitBalance = accountBalanceMapper.selectOne(lastProfitWrapper);
            BigDecimal carriedBeginDebit = BigDecimal.ZERO;
            BigDecimal carriedBeginCredit = BigDecimal.ZERO;
            BigDecimal carriedYearDebit = BigDecimal.ZERO;
            BigDecimal carriedYearCredit = BigDecimal.ZERO;
            if (lastProfitBalance != null) {
                carriedBeginDebit = lastProfitBalance.getEndDebit() != null ? lastProfitBalance.getEndDebit() : BigDecimal.ZERO;
                carriedBeginCredit = lastProfitBalance.getEndCredit() != null ? lastProfitBalance.getEndCredit() : BigDecimal.ZERO;
                if (lastYear == year) {
                    carriedYearDebit = lastProfitBalance.getYearDebit() != null ? lastProfitBalance.getYearDebit() : BigDecimal.ZERO;
                    carriedYearCredit = lastProfitBalance.getYearCredit() != null ? lastProfitBalance.getYearCredit() : BigDecimal.ZERO;
                }
            }
            profitBalance.setBeginDebit(carriedBeginDebit);
            profitBalance.setBeginCredit(carriedBeginCredit);
            profitBalance.setPeriodDebit(BigDecimal.ZERO);
            profitBalance.setPeriodCredit(BigDecimal.ZERO);
            // 期末初值=期初(后续会累加本期利润)
            profitBalance.setEndDebit(carriedBeginDebit);
            profitBalance.setEndCredit(carriedBeginCredit);
            profitBalance.setYearDebit(carriedYearDebit);
            profitBalance.setYearCredit(carriedYearCredit);
            accountBalanceMapper.insert(profitBalance);
        }
        if (profit.compareTo(BigDecimal.ZERO) >= 0) {
            // 盈利：贷记本年利润，累加本期贷方发生额+本年累计贷方
            BigDecimal newPeriodCredit = (profitBalance.getPeriodCredit() != null ? profitBalance.getPeriodCredit() : BigDecimal.ZERO).add(profit);
            profitBalance.setPeriodCredit(newPeriodCredit);
            BigDecimal newYearCredit = (profitBalance.getYearCredit() != null ? profitBalance.getYearCredit() : BigDecimal.ZERO).add(profit);
            profitBalance.setYearCredit(newYearCredit);
            BigDecimal currentCredit = profitBalance.getEndCredit() != null ? profitBalance.getEndCredit() : BigDecimal.ZERO;
            profitBalance.setEndCredit(currentCredit.add(profit));
        } else {
            // 亏损：借记本年利润，累加本期借方发生额+本年累计借方
            BigDecimal newPeriodDebit = (profitBalance.getPeriodDebit() != null ? profitBalance.getPeriodDebit() : BigDecimal.ZERO).add(profit.abs());
            profitBalance.setPeriodDebit(newPeriodDebit);
            BigDecimal newYearDebit = (profitBalance.getYearDebit() != null ? profitBalance.getYearDebit() : BigDecimal.ZERO).add(profit.abs());
            profitBalance.setYearDebit(newYearDebit);
            BigDecimal currentDebit = profitBalance.getEndDebit() != null ? profitBalance.getEndDebit() : BigDecimal.ZERO;
            profitBalance.setEndDebit(currentDebit.add(profit.abs()));
        }
        accountBalanceMapper.updateById(profitBalance);

        log.info("损益结转完成，账套ID：{}，期间：{}-{}，收入总额：{}，费用总额：{}，净利润：{}",
                accountSetId, year, month, totalRevenue, totalExpense, profit);
    }

    /**
     * 获取会计期间
     */
    private AccountPeriod getPeriod(Long accountSetId, int year, int month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getYear, year)
               .eq(AccountPeriod::getMonth, month);
        AccountPeriod period = accountPeriodMapper.selectOne(wrapper);
        if (period == null) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_FOUND);
        }
        return period;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void carryForwardYear(Long accountSetId, Integer fromYear) {
        log.info("年度结转，账套ID：{}，来源年度：{}", accountSetId, fromYear);

        // 参数校验
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        if (fromYear == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源年度不能为空");
        }
        if (fromYear < 1900 || fromYear > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 校验账套存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        int toYear = fromYear + 1;

        // 1. 检查fromYear所有月份是否已结账
        LambdaQueryWrapper<AccountPeriod> fromYearWrapper = new LambdaQueryWrapper<>();
        fromYearWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                        .eq(AccountPeriod::getYear, fromYear);
        List<AccountPeriod> fromYearPeriods = accountPeriodMapper.selectList(fromYearWrapper);

        if (fromYearPeriods.size() < 12) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "来源年度会计期间不完整，无法结转");
        }

        for (AccountPeriod period : fromYearPeriods) {
            if (!PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "来源年度" + period.getMonth() + "月未结账，无法进行年度结转");
            }
        }

        // 2. 检查下一年度会计期间是否已存在
        LambdaQueryWrapper<AccountPeriod> toYearWrapper = new LambdaQueryWrapper<>();
        toYearWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                     .eq(AccountPeriod::getYear, toYear);
        Long toYearCount = accountPeriodMapper.selectCount(toYearWrapper);
        if (toYearCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "下一年度会计期间已存在，无法重复结转");
        }

        // 3. 创建下一年的12个会计期间
        for (int month = 1; month <= 12; month++) {
            AccountPeriod newPeriod = new AccountPeriod();
            newPeriod.setAccountSetId(accountSetId);
            newPeriod.setYear(toYear);
            newPeriod.setMonth(month);
            newPeriod.setStartDate(LocalDate.of(toYear, month, 1));
            newPeriod.setEndDate(LocalDate.of(toYear, month, 1)
                    .plusMonths(1).minusDays(1));
            newPeriod.setStatus(PeriodStatus.OPEN.getCode());
            accountPeriodMapper.insert(newPeriod);
        }
        log.info("创建{}年度12个会计期间完成", toYear);

        // 4. 复制科目体系到下一年（科目无年度字段，全账套共享，验证科目存在即可）
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        log.info("科目体系共{}个科目，已共享至{}年度", subjects.size(), toYear);

        // 5. 将fromYear年末余额结转为下一年年初余额
        // 查询fromYear 12月的科目余额（年末余额）
        LambdaQueryWrapper<AccountBalance> decBalanceWrapper = new LambdaQueryWrapper<>();
        decBalanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                         .eq(AccountBalance::getYear, fromYear)
                         .eq(AccountBalance::getMonth, 12);
        List<AccountBalance> decBalances = accountBalanceMapper.selectList(decBalanceWrapper);

        // 4.1 年末结转"本年利润(3103)"至"利润分配-未分配利润(3104)"
        // 按会计准则,本年利润科目年末应结转到"利润分配-未分配利润",本年利润年末无余额
        // 否则下年度1月本年利润科目有期初余额(上年累计净利润),资产负债表"本年利润"项目反映累计利润而非当年利润
        Subject profitSubject = subjects.stream()
                .filter(s -> "3103".equals(s.getCode()))
                .findFirst().orElse(null);
        Subject retainedEarningsSubject = subjects.stream()
                .filter(s -> "3104".equals(s.getCode()))
                .findFirst().orElse(null);
        if (profitSubject != null && retainedEarningsSubject != null) {
            AccountBalance profitDecBalance = decBalances.stream()
                    .filter(b -> profitSubject.getId().equals(b.getSubjectId()))
                    .findFirst().orElse(null);
            if (profitDecBalance != null) {
                BigDecimal profitEndCredit = profitDecBalance.getEndCredit() != null ? profitDecBalance.getEndCredit() : BigDecimal.ZERO;
                BigDecimal profitEndDebit = profitDecBalance.getEndDebit() != null ? profitDecBalance.getEndDebit() : BigDecimal.ZERO;
                // 本年利润年末余额(贷方为正表示盈利)
                BigDecimal profitNet = profitEndCredit.subtract(profitEndDebit);
                if (profitNet.compareTo(BigDecimal.ZERO) != 0) {
                    // 获取或创建利润分配3104的年末余额
                    AccountBalance retainedDecBalance = decBalances.stream()
                            .filter(b -> retainedEarningsSubject.getId().equals(b.getSubjectId()))
                            .findFirst().orElse(null);
                    if (retainedDecBalance == null) {
                        retainedDecBalance = new AccountBalance();
                        retainedDecBalance.setAccountSetId(accountSetId);
                        retainedDecBalance.setSubjectId(retainedEarningsSubject.getId());
                        retainedDecBalance.setYear(fromYear);
                        retainedDecBalance.setMonth(12);
                        retainedDecBalance.setBeginDebit(BigDecimal.ZERO);
                        retainedDecBalance.setBeginCredit(BigDecimal.ZERO);
                        retainedDecBalance.setPeriodDebit(BigDecimal.ZERO);
                        retainedDecBalance.setPeriodCredit(BigDecimal.ZERO);
                        retainedDecBalance.setEndDebit(BigDecimal.ZERO);
                        retainedDecBalance.setEndCredit(BigDecimal.ZERO);
                        retainedDecBalance.setYearDebit(BigDecimal.ZERO);
                        retainedDecBalance.setYearCredit(BigDecimal.ZERO);
                        accountBalanceMapper.insert(retainedDecBalance);
                        decBalances.add(retainedDecBalance);
                    }
                    BigDecimal absAmount = profitNet.abs();
                    if (profitNet.compareTo(BigDecimal.ZERO) > 0) {
                        // 盈利:借3103贷3104,利润分配年末贷方余额相应增加
                        retainedDecBalance.setEndCredit(
                                (retainedDecBalance.getEndCredit() != null ? retainedDecBalance.getEndCredit() : BigDecimal.ZERO).add(profitNet));
                        // 同步本期/本年发生额:贷方记3104的贷方发生额
                        retainedDecBalance.setPeriodCredit(
                                (retainedDecBalance.getPeriodCredit() != null ? retainedDecBalance.getPeriodCredit() : BigDecimal.ZERO).add(profitNet));
                        retainedDecBalance.setYearCredit(
                                (retainedDecBalance.getYearCredit() != null ? retainedDecBalance.getYearCredit() : BigDecimal.ZERO).add(profitNet));
                        // 3103本年利润:借方记结转出金额
                        profitDecBalance.setPeriodDebit(
                                (profitDecBalance.getPeriodDebit() != null ? profitDecBalance.getPeriodDebit() : BigDecimal.ZERO).add(profitNet));
                        profitDecBalance.setYearDebit(
                                (profitDecBalance.getYearDebit() != null ? profitDecBalance.getYearDebit() : BigDecimal.ZERO).add(profitNet));
                    } else {
                        // 亏损:借3104贷3103,利润分配年末借方余额相应增加(代表减少)
                        retainedDecBalance.setEndDebit(
                                (retainedDecBalance.getEndDebit() != null ? retainedDecBalance.getEndDebit() : BigDecimal.ZERO).add(absAmount));
                        // 同步本期/本年发生额:借方记3104的借方发生额
                        retainedDecBalance.setPeriodDebit(
                                (retainedDecBalance.getPeriodDebit() != null ? retainedDecBalance.getPeriodDebit() : BigDecimal.ZERO).add(absAmount));
                        retainedDecBalance.setYearDebit(
                                (retainedDecBalance.getYearDebit() != null ? retainedDecBalance.getYearDebit() : BigDecimal.ZERO).add(absAmount));
                        // 3103本年利润:贷方记结转出金额
                        profitDecBalance.setPeriodCredit(
                                (profitDecBalance.getPeriodCredit() != null ? profitDecBalance.getPeriodCredit() : BigDecimal.ZERO).add(absAmount));
                        profitDecBalance.setYearCredit(
                                (profitDecBalance.getYearCredit() != null ? profitDecBalance.getYearCredit() : BigDecimal.ZERO).add(absAmount));
                    }
                    accountBalanceMapper.updateById(retainedDecBalance);
                    // 3103本年利润年末余额清零
                    profitDecBalance.setEndDebit(BigDecimal.ZERO);
                    profitDecBalance.setEndCredit(BigDecimal.ZERO);
                    accountBalanceMapper.updateById(profitDecBalance);
                    log.info("年末结转本年利润至利润分配完成，账套ID：{}，年度：{}，净利润：{}", accountSetId, fromYear, profitNet);
                }
            }
        }

        // 为下一年创建各月份的科目余额记录
        for (Subject subject : subjects) {
            // 找到该科目fromYear 12月的期末余额
            AccountBalance decBalance = decBalances.stream()
                    .filter(b -> subject.getId().equals(b.getSubjectId()))
                    .findFirst()
                    .orElse(null);

            BigDecimal beginDebit = BigDecimal.ZERO;
            BigDecimal beginCredit = BigDecimal.ZERO;
            if (decBalance != null) {
                beginDebit = decBalance.getEndDebit() != null ? decBalance.getEndDebit() : BigDecimal.ZERO;
                beginCredit = decBalance.getEndCredit() != null ? decBalance.getEndCredit() : BigDecimal.ZERO;
            }

            // 只为下一年1月创建余额记录(年初余额=上年年末余额)
            // 2-12月余额记录由凭证过账时懒生成,避免预建破坏月度连续结转(每月期初=上月期末)
            AccountBalance newBalance = new AccountBalance();
            newBalance.setAccountSetId(accountSetId);
            newBalance.setSubjectId(subject.getId());
            newBalance.setYear(toYear);
            newBalance.setMonth(1);
            newBalance.setBeginDebit(beginDebit);
            newBalance.setBeginCredit(beginCredit);
            newBalance.setPeriodDebit(BigDecimal.ZERO);
            newBalance.setPeriodCredit(BigDecimal.ZERO);
            newBalance.setEndDebit(beginDebit);
            newBalance.setEndCredit(beginCredit);
            newBalance.setYearDebit(BigDecimal.ZERO);
            newBalance.setYearCredit(BigDecimal.ZERO);
            accountBalanceMapper.insert(newBalance);
        }
        log.info("结转{}年度年末余额至{}年度1月年初余额完成，共{}个科目", fromYear, toYear, subjects.size());

        // 5. 标记年度结转完成
        log.info("年度结转完成，账套ID：{}，从{}年度结转至{}年度", accountSetId, fromYear, toYear);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long carryForwardCost(Long accountSetId, Integer year, Integer month, BigDecimal costRate) {
        log.info("期末成本结转，账套ID：{}，期间：{}-{}，成本率：{}", accountSetId, year, month, costRate);

        if (costRate == null) {
            costRate = new BigDecimal("0.80");
        }
        if (costRate.compareTo(BigDecimal.ZERO) < 0 || costRate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "成本率必须在0-1之间");
        }

        AccountPeriod period = getPeriod(accountSetId, year, month);

        // 期间状态校验:已结账期间余额已固化,不允许再执行成本结转(与carryForward保持一致)
        if (PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
            throw new BusinessException(ErrorCode.PERIOD_ALREADY_CLOSED);
        }

        // 幂等校验:检查本期是否已存在系统生成的成本结转凭证,避免重复结转
        // 原实现无任何幂等校验,重复调用会重复借记成本/贷记库存,导致主营业务成本虚增、库存虚减
        LambdaQueryWrapper<Voucher> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Voucher::getAccountSetId, accountSetId)
                   .eq(Voucher::getYear, year)
                   .eq(Voucher::getMonth, month)
                   .eq(Voucher::getSource, 1);
        List<Voucher> existVouchers = voucherMapper.selectList(existWrapper);
        if (!existVouchers.isEmpty()) {
            List<Long> voucherIds = existVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> costDetailWrapper = new LambdaQueryWrapper<>();
            costDetailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .eq(VoucherDetail::getSummary, "结转销售成本");
            Long costCount = voucherDetailMapper.selectCount(costDetailWrapper);
            if (costCount != null && costCount > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "本期" + year + "年" + month + "月已执行成本结转,不能重复结转");
            }
        }

        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);

        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        Map<Long, AccountBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));

        BigDecimal totalSalesRevenue = BigDecimal.ZERO;
        for (Subject subject : subjects) {
            if (subject.getCode() != null && subject.getCode().startsWith("5001")) {
                AccountBalance balance = balanceMap.get(subject.getId());
                if (balance != null) {
                    // 销售收入净额 = 贷方发生额 - 借方发生额(销售退回/红字冲销)
                    // 忽略借方(退回)会导致收入虚高,进而成本虚增、库存虚减
                    BigDecimal credit = balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO;
                    BigDecimal debit = balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO;
                    BigDecimal netRevenue = credit.subtract(debit);
                    if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
                        totalSalesRevenue = totalSalesRevenue.add(netRevenue);
                    }
                }
            }
        }

        if (totalSalesRevenue.compareTo(BigDecimal.ZERO) == 0) {
            log.info("本期无销售收入，无需结转成本");
            return null;
        }

        BigDecimal costAmount = totalSalesRevenue.multiply(costRate).setScale(2, RoundingMode.HALF_UP);

        Subject costSubject = findSubjectByCode(subjects, "5401");
        if (costSubject == null) {
            costSubject = findSubjectByCodePrefix(subjects, "5401");
        }
        if (costSubject == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到主营业务成本科目(5401)");
        }

        Subject inventorySubject = findSubjectByCode(subjects, "1405");
        if (inventorySubject == null) {
            inventorySubject = findSubjectByCodePrefix(subjects, "1405");
        }
        if (inventorySubject == null) {
            inventorySubject = findSubjectByCodePrefix(subjects, "14");
        }
        if (inventorySubject == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未找到库存商品科目(1405)");
        }

        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherDate(period.getEndDate());
        voucher.setYear(year);
        voucher.setMonth(month);
        // 与 carryForward 保持一致：成本结转凭证状态为 POSTED（已过账），避免 closePeriod
        // 校验"未过账凭证"时阻断结账；UNAUDITED 会导致结账流程中断。
        voucher.setStatus(VoucherStatus.POSTED.getCode());
        voucher.setSource(1);
        voucher.setTotalDebit(costAmount);
        voucher.setTotalCredit(costAmount);
        voucher.setAttachmentCount(0);
        // 凭证字不硬编码,由账套默认凭证字决定(避免不同账套凭证字ID不一致)
        voucher.setVoucherWordId(null);
        // 与 carryForward 保持一致：填充审核/过账人与时间
        Long currentUserId = SecurityUtils.getCurrentUserId();
        voucher.setAuditBy(currentUserId);
        voucher.setAuditTime(LocalDateTime.now());
        voucher.setPostBy(currentUserId);
        voucher.setPostTime(LocalDateTime.now());

        voucher.setVoucherNo(generateCarryVoucherNo(accountSetId, year, month));

        voucherMapper.insert(voucher);

        VoucherDetail debitDetail = new VoucherDetail();
        debitDetail.setVoucherId(voucher.getId());
        debitDetail.setLineNo(1);
        debitDetail.setSummary("结转销售成本");
        debitDetail.setSubjectId(costSubject.getId());
        debitDetail.setSubjectCode(costSubject.getCode());
        debitDetail.setSubjectName(costSubject.getName());
        debitDetail.setDebit(costAmount);
        debitDetail.setCredit(BigDecimal.ZERO);
        voucherDetailMapper.insert(debitDetail);

        VoucherDetail creditDetail = new VoucherDetail();
        creditDetail.setVoucherId(voucher.getId());
        creditDetail.setLineNo(2);
        creditDetail.setSummary("结转销售成本");
        creditDetail.setSubjectId(inventorySubject.getId());
        creditDetail.setSubjectCode(inventorySubject.getCode());
        creditDetail.setSubjectName(inventorySubject.getName());
        creditDetail.setDebit(BigDecimal.ZERO);
        creditDetail.setCredit(costAmount);
        voucherDetailMapper.insert(creditDetail);

        // 同步更新科目余额（参照 carryForward 中对 acc_account_balance 的更新）
        // 借记主营业务成本：累加本期借方发生额、本年累计借方、期末借方
        AccountBalance costBalance = balanceMap.get(costSubject.getId());
        if (costBalance == null) {
            costBalance = new AccountBalance();
            costBalance.setAccountSetId(accountSetId);
            costBalance.setSubjectId(costSubject.getId());
            costBalance.setYear(year);
            costBalance.setMonth(month);
            costBalance.setBeginDebit(BigDecimal.ZERO);
            costBalance.setBeginCredit(BigDecimal.ZERO);
            costBalance.setPeriodDebit(BigDecimal.ZERO);
            costBalance.setPeriodCredit(BigDecimal.ZERO);
            costBalance.setYearDebit(BigDecimal.ZERO);
            costBalance.setYearCredit(BigDecimal.ZERO);
            costBalance.setEndDebit(BigDecimal.ZERO);
            costBalance.setEndCredit(BigDecimal.ZERO);
            accountBalanceMapper.insert(costBalance);
        }
        costBalance.setPeriodDebit((costBalance.getPeriodDebit() != null ? costBalance.getPeriodDebit() : BigDecimal.ZERO).add(costAmount));
        costBalance.setYearDebit((costBalance.getYearDebit() != null ? costBalance.getYearDebit() : BigDecimal.ZERO).add(costAmount));
        costBalance.setEndDebit((costBalance.getEndDebit() != null ? costBalance.getEndDebit() : BigDecimal.ZERO).add(costAmount));
        accountBalanceMapper.updateById(costBalance);

        // 贷记库存商品：累加本期贷方发生额、本年累计贷方、期末贷方
        AccountBalance inventoryBalance = balanceMap.get(inventorySubject.getId());
        if (inventoryBalance == null) {
            inventoryBalance = new AccountBalance();
            inventoryBalance.setAccountSetId(accountSetId);
            inventoryBalance.setSubjectId(inventorySubject.getId());
            inventoryBalance.setYear(year);
            inventoryBalance.setMonth(month);
            inventoryBalance.setBeginDebit(BigDecimal.ZERO);
            inventoryBalance.setBeginCredit(BigDecimal.ZERO);
            inventoryBalance.setPeriodDebit(BigDecimal.ZERO);
            inventoryBalance.setPeriodCredit(BigDecimal.ZERO);
            inventoryBalance.setYearDebit(BigDecimal.ZERO);
            inventoryBalance.setYearCredit(BigDecimal.ZERO);
            inventoryBalance.setEndDebit(BigDecimal.ZERO);
            inventoryBalance.setEndCredit(BigDecimal.ZERO);
            accountBalanceMapper.insert(inventoryBalance);
        }
        inventoryBalance.setPeriodCredit((inventoryBalance.getPeriodCredit() != null ? inventoryBalance.getPeriodCredit() : BigDecimal.ZERO).add(costAmount));
        inventoryBalance.setYearCredit((inventoryBalance.getYearCredit() != null ? inventoryBalance.getYearCredit() : BigDecimal.ZERO).add(costAmount));
        inventoryBalance.setEndCredit((inventoryBalance.getEndCredit() != null ? inventoryBalance.getEndCredit() : BigDecimal.ZERO).add(costAmount));
        accountBalanceMapper.updateById(inventoryBalance);

        log.info("成本结转凭证生成成功，凭证ID：{}，成本金额：{}", voucher.getId(), costAmount);
        return voucher.getId();
    }

    private Subject findSubjectByCode(List<Subject> subjects, String code) {
        for (Subject subject : subjects) {
            if (code.equals(subject.getCode())) {
                return subject;
            }
        }
        return null;
    }

    private Subject findSubjectByCodePrefix(List<Subject> subjects, String prefix) {
        for (Subject subject : subjects) {
            if (subject.getCode() != null && subject.getCode().startsWith(prefix)) {
                return subject;
            }
        }
        return null;
    }
}
