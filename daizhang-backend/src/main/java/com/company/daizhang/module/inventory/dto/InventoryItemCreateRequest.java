package com.company.daizhang.module.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemCreateRequest {
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
    @NotBlank(message = "商品编码不能为空")
    private String itemCode;
    @NotBlank(message = "商品名称不能为空")
    private String itemName;
    private String specification;
    private String unit;
    @DecimalMin(value = "0", message = "单价不能为负数")
    @Digits(integer = 15, fraction = 4, message = "单价精度超出范围")
    private BigDecimal unitPrice;
    private String category;
    private String remark;
}
