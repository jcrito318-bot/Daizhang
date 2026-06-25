package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办事项VO
 */
@Data
public class TodoItemVO {

    /**
     * 待办类型：SERVICE_TASK-服务任务 TAX_DECLARATION-税务申报 VOUCHER_AUDIT-凭证审核
     */
    private String todoType;

    private String todoTypeDesc;

    /**
     * 关联ID
     */
    private Long refId;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 标题/摘要
     */
    private String title;

    /**
     * 年
     */
    private Integer year;

    /**
     * 月
     */
    private Integer month;

    /**
     * 负责人
     */
    private String assigneeName;

    /**
     * 状态
     */
    private Integer status;

    private String statusDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否逾期
     */
    private Boolean overdue;
}
