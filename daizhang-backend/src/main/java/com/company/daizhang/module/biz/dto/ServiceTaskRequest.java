package com.company.daizhang.module.biz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代账服务任务请求
 */
@Data
public class ServiceTaskRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Long customerId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "流程节点ID不能为空")
    private Long nodeId;

    private Long assigneeId;

    private String assigneeName;

    private Integer taskStatus;

    private String remark;
}
