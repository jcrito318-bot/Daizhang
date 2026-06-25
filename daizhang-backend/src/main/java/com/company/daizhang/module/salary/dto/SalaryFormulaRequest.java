package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 薪资公式请求
 */
@Data
public class SalaryFormulaRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "公式名称不能为空")
    private String formulaName;

    @NotBlank(message = "目标薪资项不能为空")
    private String targetItem;

    @NotBlank(message = "公式表达式不能为空")
    private String formulaExpression;

    private Integer priority;

    private Integer status;
}
