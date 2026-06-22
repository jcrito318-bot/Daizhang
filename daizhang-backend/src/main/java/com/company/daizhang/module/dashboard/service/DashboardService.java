package com.company.daizhang.module.dashboard.service;

import com.company.daizhang.module.dashboard.vo.DashboardStatsVO;

/**
 * 仪表盘服务接口
 */
public interface DashboardService {
    
    /**
     * 获取统计数据
     */
    DashboardStatsVO getStats(Long accountSetId);
}
