package com.company.daizhang.module.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 辅助核算类别请求
 */
@Data
public class AuxiliaryCategoryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "类别编码不能为空")
    private String categoryCode;

    @NotBlank(message = "类别名称不能为空")
    private String categoryName;

    @NotBlank(message = "类别类型不能为空")
    private String categoryType;

    private String remark;
}
