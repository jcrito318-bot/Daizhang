package com.company.daizhang.module.industrycommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 工商外勤任务创建请求
 */
@Data
public class IndustryCommerceTaskCreateRequest {

    /**
     * 工商服务ID
     */
    @NotNull(message = "工商服务ID不能为空")
    private Long serviceId;

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
