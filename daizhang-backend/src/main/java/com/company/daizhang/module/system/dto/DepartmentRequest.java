package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 部门请求
 */
@Data
public class DepartmentRequest {

    private Long parentId;

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Long managerId;

    private Integer sortOrder;

    private Integer status;
}
