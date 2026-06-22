package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 薪资项目创建请求
 */
@Data
public class SalaryItemCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "项目类型不能为空")
    private String itemType;

    private String calculationMethod;

    private String remark;
}
