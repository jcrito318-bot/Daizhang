package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位请求
 */
@Data
public class PositionRequest {

    @NotBlank(message = "岗位编码不能为空")
    private String positionCode;

    @NotBlank(message = "岗位名称不能为空")
    private String positionName;

    private Long departmentId;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
