package com.company.daizhang.module.industrycommerce.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 工商外勤任务更新请求
 */
@Data
public class IndustryCommerceTaskUpdateRequest {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 外勤日期
     */
    private LocalDate fieldDate;

    /**
     * 地点
     */
    private String location;

    /**
     * 备注
     */
    private String remark;
}
