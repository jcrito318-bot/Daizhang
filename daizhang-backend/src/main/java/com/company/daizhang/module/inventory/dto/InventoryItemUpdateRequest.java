package com.company.daizhang.module.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    private String itemName;
    private String specification;
    private String unit;
    @DecimalMin(value = "0", message = "单价不能为负数")
    @Digits(integer = 15, fraction = 4, message = "单价精度超出范围")
    private BigDecimal unitPrice;
    private String category;
    private Integer status;
    private String remark;
}
