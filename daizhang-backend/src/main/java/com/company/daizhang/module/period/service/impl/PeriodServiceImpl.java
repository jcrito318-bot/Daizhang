package com.company.daizhang.module.period.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.enums.PeriodStatus;
import com.company.daizhang.common.enums.VoucherStatus;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        LambdaQueryWrapper<Voucher> unpostWrapper = new LambdaQueryWrapper<>();
        unpostWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .ne(Voucher::getStatus, VoucherStatus.POSTED.getCode());
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

        // 5. 更新会计期间状态为已结账
        period.setStatus(PeriodStatus.CLOSED.getCode());
        period.setCloseBy(SecurityUtils.getCurrentUserId());
        period.setCloseTime(LocalDateTime.now());
        accountPeriodMapper.updateById(period);

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
        period.setStatus(PeriodStatus.OPEN.getCode());
        period.setCloseBy(null);
        period.setCloseTime(null);
        accountPeriodMapper.updateById(period);

        log.info("期末反结账成功，账套ID：{}，期间：{}-{}", accountSetId, year, month);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void carryForward(Long accountSetId, int year, int month) {
        log.info("损益结转，账套ID：{}，期间：{}-{}", accountSetId, year, month);

        // 1. 查询该期间
        AccountPeriod period = getPeriod(accountSetId, year, month);

        // 2. 查询收入类科目（category = 'revenue'）
        LambdaQueryWrapper<Subject> revenueWrapper = new LambdaQueryWrapper<>();
        revenueWrapper.eq(Subject::getAccountSetId, accountSetId)
                     .eq(Subject::getCategory, "revenue");
        List<Subject> revenueSubjects = subjectMapper.selectList(revenueWrapper);

        // 3. 查询费用类科目（category = 'expense'）
        LambdaQueryWrapper<Subject> expenseWrapper = new LambdaQueryWrapper<>();
        expenseWrapper.eq(Subject::getAccountSetId, accountSetId)
                     .eq(Subject::getCategory, "expense");
        List<Subject> expenseSubjects = subjectMapper.selectList(expenseWrapper);

        // 4. 查询本年利润科目
        LambdaQueryWrapper<Subject> profitWrapper = new LambdaQueryWrapper<>();
        profitWrapper.eq(Subject::getAccountSetId, accountSetId)
                    .like(Subject::getCode, "3131");
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

        // 6. 计算收入总额（收入类科目通常为贷方余额）
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Subject subject : revenueSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null && balance.getEndCredit() != null) {
                totalRevenue = totalRevenue.add(balance.getEndCredit());
            }
        }

        // 7. 计算费用总额（费用类科目通常为借方余额）
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Subject subject : expenseSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null && balance.getEndDebit() != null) {
                totalExpense = totalExpense.add(balance.getEndDebit());
            }
        }

        // 如果没有损益发生，无需结转
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0 && totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            log.info("本期无损益发生，无需结转");
            return;
        }

        // 8. 生成结转凭证
        BigDecimal totalAmount = totalRevenue.add(totalExpense);
        BigDecimal profit = totalRevenue.subtract(totalExpense);

        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherDate(period.getEndDate());
        voucher.setYear(year);
        voucher.setMonth(month);
        voucher.setStatus(VoucherStatus.UNAUDITED.getCode());
        voucher.setSource(1); // 系统生成
        voucher.setTotalDebit(totalAmount);
        voucher.setTotalCredit(totalAmount);
        voucher.setAttachmentCount(0);

        // 生成凭证号
        LambdaQueryWrapper<Voucher> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Voucher::getAccountSetId, accountSetId)
                   .eq(Voucher::getYear, year)
                   .eq(Voucher::getMonth, month);
        long count = voucherMapper.selectCount(countWrapper);
        voucher.setVoucherNo(String.format("%04d", count + 1));

        voucherMapper.insert(voucher);

        // 9. 凭证明细：收入科目结转（借记收入科目，将其余额转为0）
        int lineNo = 1;
        for (Subject subject : revenueSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null && balance.getEndCredit() != null && balance.getEndCredit().compareTo(BigDecimal.ZERO) > 0) {
                VoucherDetail detail = new VoucherDetail();
                detail.setVoucherId(voucher.getId());
                detail.setLineNo(lineNo++);
                detail.setSummary("结转收入");
                detail.setSubjectId(subject.getId());
                detail.setSubjectCode(subject.getCode());
                detail.setSubjectName(subject.getName());
                detail.setDebit(balance.getEndCredit());
                detail.setCredit(BigDecimal.ZERO);
                detail.setSortOrder(lineNo);
                voucherDetailMapper.insert(detail);
            }
        }

        // 10. 凭证明细：费用科目结转（贷记费用科目，将其余额转为0）
        for (Subject subject : expenseSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null && balance.getEndDebit() != null && balance.getEndDebit().compareTo(BigDecimal.ZERO) > 0) {
                VoucherDetail detail = new VoucherDetail();
                detail.setVoucherId(voucher.getId());
                detail.setLineNo(lineNo++);
                detail.setSummary("结转费用");
                detail.setSubjectId(subject.getId());
                detail.setSubjectCode(subject.getCode());
                detail.setSubjectName(subject.getName());
                detail.setDebit(BigDecimal.ZERO);
                detail.setCredit(balance.getEndDebit());
                detail.setSortOrder(lineNo);
                voucherDetailMapper.insert(detail);
            }
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

        // 12. 更新损益类科目余额为0
        for (Subject subject : revenueSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                balance.setEndDebit(BigDecimal.ZERO);
                balance.setEndCredit(BigDecimal.ZERO);
                accountBalanceMapper.updateById(balance);
            }
        }
        for (Subject subject : expenseSubjects) {
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance != null) {
                balance.setEndDebit(BigDecimal.ZERO);
                balance.setEndCredit(BigDecimal.ZERO);
                accountBalanceMapper.updateById(balance);
            }
        }

        // 13. 更新本年利润科目余额
        AccountBalance profitBalance = balanceMap.get(profitSubject.getId());
        if (profitBalance != null) {
            if (profit.compareTo(BigDecimal.ZERO) >= 0) {
                // 盈利：增加贷方余额
                BigDecimal currentCredit = profitBalance.getEndCredit() != null ? profitBalance.getEndCredit() : BigDecimal.ZERO;
                profitBalance.setEndCredit(currentCredit.add(profit));
            } else {
                // 亏损：增加借方余额
                BigDecimal currentDebit = profitBalance.getEndDebit() != null ? profitBalance.getEndDebit() : BigDecimal.ZERO;
                profitBalance.setEndDebit(currentDebit.add(profit.abs()));
            }
            accountBalanceMapper.updateById(profitBalance);
        }

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
}
