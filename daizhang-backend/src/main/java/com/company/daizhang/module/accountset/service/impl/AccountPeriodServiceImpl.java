package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.service.AccountPeriodService;
import com.company.daizhang.module.accountset.vo.AccountPeriodVO;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.common.enums.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会计期间服务实现
 */
@Service
@RequiredArgsConstructor
public class AccountPeriodServiceImpl implements AccountPeriodService {

    private final AccountPeriodMapper accountPeriodMapper;
    private final VoucherMapper voucherMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    
    @Override
    public List<AccountPeriodVO> listPeriods(Long accountSetId) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .orderByDesc(AccountPeriod::getYear)
               .orderByDesc(AccountPeriod::getMonth);
        
        List<AccountPeriod> periods = accountPeriodMapper.selectList(wrapper);
        
        return periods.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPeriod(Long accountSetId, int year) {
        // 检查是否已存在该年度的期间
        LambdaQueryWrapper<AccountPeriod> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                   .eq(AccountPeriod::getYear, year);
        
        if (accountPeriodMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(400, "该年度的会计期间已存在");
        }
        
        // 生成12个月的会计期间
        for (int month = 1; month <= 12; month++) {
            AccountPeriod period = new AccountPeriod();
            period.setAccountSetId(accountSetId);
            period.setYear(year);
            period.setMonth(month);
            period.setStartDate(LocalDate.of(year, month, 1));
            period.setEndDate(period.getStartDate().plusMonths(1).minusDays(1));
            period.setStatus(0); // 未结账
            
            accountPeriodMapper.insert(period);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePeriod(Long accountSetId, int year, int month) {
        AccountPeriod period = getPeriod(accountSetId, year, month);

        if (Integer.valueOf(1).equals(period.getStatus())) {
            throw new BusinessException(400, "该期间已结账");
        }

        // 校验本期凭证是否全部审核
        LambdaQueryWrapper<Voucher> unauditWrapper = new LambdaQueryWrapper<>();
        unauditWrapper.eq(Voucher::getAccountSetId, accountSetId)
                     .eq(Voucher::getYear, year)
                     .eq(Voucher::getMonth, month)
                     .eq(Voucher::getStatus, VoucherStatus.UNAUDITED.getCode());
        long unauditedCount = voucherMapper.selectCount(unauditWrapper);
        if (unauditedCount > 0) {
            throw new BusinessException(400, "存在" + unauditedCount + "张未审核的凭证，无法结账");
        }

        // 校验本期凭证是否全部过账
        // 仅统计"未审核(0)+已审核(1)"的凭证,作废凭证(3)不应阻塞结账,
        // 否则用户作废一张凭证后该期间永远无法结账(原 .ne(POSTED) 会把作废凭证计入未过账数)
        LambdaQueryWrapper<Voucher> unpostWrapper = new LambdaQueryWrapper<>();
        unpostWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .in(Voucher::getStatus, VoucherStatus.UNAUDITED.getCode(), VoucherStatus.AUDITED.getCode());
        long unpostedCount = voucherMapper.selectCount(unpostWrapper);
        if (unpostedCount > 0) {
            throw new BusinessException(400, "存在" + unpostedCount + "张未过账的凭证，无法结账");
        }

        // 试算平衡校验:结账前必须确保借贷平衡,否则会固化错误余额。
        // 必须校验"期初+本期"合计平衡(期末累计借贷平衡),而非仅本期发生额平衡——
        // 否则当期初余额录入错误(借贷不平)时,只要本期发生额借贷相等即可通过结账,
        // 错误的期初数据被固化,资产负债表"资产=负债+所有者权益"将永远不平。
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                     .eq(AccountBalance::getYear, year)
                     .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (AccountBalance b : balances) {
            // 期初借方 + 本期借方借方发生额
            totalDebit = totalDebit.add(b.getBeginDebit() != null ? b.getBeginDebit() : BigDecimal.ZERO)
                    .add(b.getPeriodDebit() != null ? b.getPeriodDebit() : BigDecimal.ZERO);
            // 期初贷方 + 本期贷方发生额
            totalCredit = totalCredit.add(b.getBeginCredit() != null ? b.getBeginCredit() : BigDecimal.ZERO)
                    .add(b.getPeriodCredit() != null ? b.getPeriodCredit() : BigDecimal.ZERO);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(400, "试算不平衡，借方合计" + totalDebit + "≠贷方合计" + totalCredit + "，无法结账");
        }

        // 使用LambdaUpdateWrapper确保null字段能正确更新
        // 乐观锁:WHERE条件包含status=0,防止并发结账覆盖
        LambdaUpdateWrapper<AccountPeriod> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AccountPeriod::getId, period.getId())
                .eq(AccountPeriod::getStatus, 0)
                .set(AccountPeriod::getStatus, 1)
                .set(AccountPeriod::getCloseBy, SecurityUtils.getCurrentUserId())
                .set(AccountPeriod::getCloseTime, LocalDateTime.now());
        int updated = accountPeriodMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BusinessException(409, "期间状态已变更(并发冲突)，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenPeriod(Long accountSetId, int year, int month) {
        AccountPeriod period = getPeriod(accountSetId, year, month);

        if (Integer.valueOf(0).equals(period.getStatus())) {
            throw new BusinessException(400, "该期间未结账");
        }

        // 校验是否存在已结账的后续期间(必须从后往前依次反结账)
        LambdaQueryWrapper<AccountPeriod> subsequentWrapper = new LambdaQueryWrapper<>();
        subsequentWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                        .eq(AccountPeriod::getStatus, 1)
                        .and(w -> w
                                .gt(AccountPeriod::getYear, year)
                                .or()
                                .eq(AccountPeriod::getYear, year)
                                .gt(AccountPeriod::getMonth, month));
        Long subsequentCount = accountPeriodMapper.selectCount(subsequentWrapper);
        if (subsequentCount != null && subsequentCount > 0) {
            throw new BusinessException(400, "存在已结账的后续期间，请先反结账后续期间");
        }

        // 使用LambdaUpdateWrapper显式置空closeBy/closeTime,避免NOT_NULL策略导致null字段不更新
        // 乐观锁:WHERE条件包含status=1,防止并发反结账覆盖
        LambdaUpdateWrapper<AccountPeriod> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AccountPeriod::getId, period.getId())
                .eq(AccountPeriod::getStatus, 1)
                .set(AccountPeriod::getStatus, 0)
                .set(AccountPeriod::getCloseBy, null)
                .set(AccountPeriod::getCloseTime, null);
        int updated = accountPeriodMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BusinessException(409, "期间状态已变更(并发冲突)，请刷新后重试");
        }
    }
    
    @Override
    public AccountPeriod getCurrentPeriod(Long accountSetId) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getStatus, 0)
               .orderByAsc(AccountPeriod::getYear)
               .orderByAsc(AccountPeriod::getMonth)
               .last("LIMIT 1");
        
        return accountPeriodMapper.selectOne(wrapper);
    }
    
    private AccountPeriod getPeriod(Long accountSetId, int year, int month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getYear, year)
               .eq(AccountPeriod::getMonth, month);
        
        AccountPeriod period = accountPeriodMapper.selectOne(wrapper);
        if (period == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        return period;
    }
    
    private AccountPeriodVO convertToVO(AccountPeriod period) {
        AccountPeriodVO vo = new AccountPeriodVO();
        BeanUtil.copyProperties(period, vo);
        return vo;
    }
}
