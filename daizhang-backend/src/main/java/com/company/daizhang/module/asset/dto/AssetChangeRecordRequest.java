package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产变动记录请求
 */
@Data
public class AssetChangeRecordRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @NotBlank(message = "变动类型不能为空")
    private String changeType;

    @NotNull(message = "变动日期不能为空")
    private LocalDate changeDate;

    @NotNull(message = "变动金额不能为空")
    @DecimalMin(value = "0", message = "变动金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "变动金额精度超出范围")
    private BigDecimal changeAmount;

    private String fromDepartment;

    private String toDepartment;

    private Long voucherId;

    private String remark;
}
