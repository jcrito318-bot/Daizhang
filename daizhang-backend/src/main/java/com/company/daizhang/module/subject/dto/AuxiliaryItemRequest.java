package com.company.daizhang.module.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 辅助核算项目请求
 */
@Data
public class AuxiliaryItemRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "类别ID不能为空")
    private Long categoryId;

    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    private Long parentId;

    private Integer status;

    private String remark;
}
