package com.company.daizhang.module.dashboard.service;

import com.company.daizhang.module.dashboard.vo.DashboardVO;

/**
 * 代账公司运营看板服务
 */
public interface DashboardService {

    /**
     * 获取运营看板总览（含统计、客户摘要、待办看板）
     */
    DashboardVO getDashboard();
}
