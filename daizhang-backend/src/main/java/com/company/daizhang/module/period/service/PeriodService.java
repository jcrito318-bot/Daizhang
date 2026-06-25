package com.company.daizhang.module.period.service;

import com.company.daizhang.module.period.dto.TrialBalanceRequest;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceResultVO;

/**
 * 期末处理服务接口
 */
public interface PeriodService {
    
    /**
     * 试算平衡
     */
    TrialBalanceResultVO trialBalance(TrialBalanceRequest request);
    
    /**
     * 结账
     */
    ClosePeriodResultVO closePeriod(Long accountSetId, int year, int month);
    
    /**
     * 反结账
     */
    void reopenPeriod(Long accountSetId, int year, int month);
    
    /**
     * 月末结转损益
     */
    void carryForward(Long accountSetId, int year, int month);

    /**
     * 年度结转
     */
    void carryForwardYear(Long accountSetId, Integer fromYear);
}
