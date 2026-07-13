package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

/**
 * 运营看板总览统计VO
 */
@Data
public class DashboardSummary {

    /**
     * 客户账套总数
     */
    private Integer totalAccountSets;

    /**
     * 启用账套数
     */
    private Integer activeAccountSets;

    /**
     * 一般纳税人数量
     */
    private Integer generalTaxpayerCount;

    /**
     * 小规模纳税人数量
     */
    private Integer smallTaxpayerCount;

    /**
     * 待办服务任务数
     */
    private Integer pendingTaskCount;

    /**
     * 已完成服务任务数
     */
    private Integer completedTaskCount;

    /**
     * 未审核凭证数
     */
    private Integer unauditedVoucherCount;

    /**
     * 本月凭证总数(按当前年月统计,与未审核凭证数区分,避免首页两块卡片显示相同数字)
     */
    private Integer monthVoucherCount;

    /**
     * 未申报税种数
     */
    private Integer undeclaredTaxCount;

    /**
     * 已逾期待办数
     */
    private Integer overdueTodoCount;
}
