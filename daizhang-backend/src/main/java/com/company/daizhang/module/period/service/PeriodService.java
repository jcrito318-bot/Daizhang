package com.company.daizhang.module.period.service;

import com.company.daizhang.module.period.dto.TrialBalanceRequest;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceResultVO;

import java.math.BigDecimal;

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

    /**
     * 期末成本结转
     * 按销售收入的指定比例结转销售成本
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @param costRate     成本率（如0.8表示80%），为null时使用默认值
     * @return 生成的凭证ID
     */
    Long carryForwardCost(Long accountSetId, Integer year, Integer month, BigDecimal costRate);
}
