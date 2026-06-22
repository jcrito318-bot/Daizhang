package com.company.daizhang.module.accountset.service;

import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.vo.AccountPeriodVO;

import java.util.List;

/**
 * 会计期间服务接口
 */
public interface AccountPeriodService {
    
    /**
     * 查询账套的会计期间列表
     */
    List<AccountPeriodVO> listPeriods(Long accountSetId);
    
    /**
     * 初始化会计期间（生成一年的12个月）
     */
    void createPeriod(Long accountSetId, int year);
    
    /**
     * 结账
     */
    void closePeriod(Long accountSetId, int year, int month);
    
    /**
     * 反结账
     */
    void reopenPeriod(Long accountSetId, int year, int month);
    
    /**
     * 获取当前期间（最新未结账期间）
     */
    AccountPeriod getCurrentPeriod(Long accountSetId);
}
