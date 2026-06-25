package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 代账公司运营看板VO
 */
@Data
public class DashboardVO {

    /**
     * 总览统计
     */
    private DashboardSummary summary;

    /**
     * 客户列表摘要
     */
    private List<CustomerSummaryVO> customers;

    /**
     * 待办看板（含各类型待办事项）
     */
    private List<TodoItemVO> todoItems;
}
