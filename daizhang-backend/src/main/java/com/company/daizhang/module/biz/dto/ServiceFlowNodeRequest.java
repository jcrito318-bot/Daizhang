package com.company.daizhang.module.biz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代账服务流程节点请求
 */
@Data
public class ServiceFlowNodeRequest {

    @NotBlank(message = "节点编码不能为空")
    private String nodeCode;

    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    private Integer sortOrder;

    private String description;

    private Integer status;
}
