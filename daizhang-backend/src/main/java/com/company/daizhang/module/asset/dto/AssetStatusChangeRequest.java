package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产状态变更请求
 */
@Data
public class AssetStatusChangeRequest {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    /**
     * 目标状态：0-在用 1-闲置 2-报废
     */
    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    private String remark;
}
