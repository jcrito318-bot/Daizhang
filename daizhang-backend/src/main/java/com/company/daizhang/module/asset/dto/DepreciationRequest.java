package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 折旧计提请求
 */
@Data
public class DepreciationRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;
}
