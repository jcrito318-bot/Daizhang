package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemUpdateRequest {
    private Long id;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private String category;
    private Integer status;
    private String remark;
}
