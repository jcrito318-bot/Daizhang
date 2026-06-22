package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.service.AccountPeriodService;
import com.company.daizhang.module.accountset.vo.AccountPeriodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        if (period.getStatus() == 1) {
            throw new BusinessException(400, "该期间已结账");
        }
        
        period.setStatus(1);
        period.setCloseBy(SecurityUtils.getCurrentUserId());
        period.setCloseTime(LocalDateTime.now());
        
        accountPeriodMapper.updateById(period);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenPeriod(Long accountSetId, int year, int month) {
        AccountPeriod period = getPeriod(accountSetId, year, month);
        
        if (period.getStatus() == 0) {
            throw new BusinessException(400, "该期间未结账");
        }
        
        period.setStatus(0);
        period.setCloseBy(null);
        period.setCloseTime(null);
        
        accountPeriodMapper.updateById(period);
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
