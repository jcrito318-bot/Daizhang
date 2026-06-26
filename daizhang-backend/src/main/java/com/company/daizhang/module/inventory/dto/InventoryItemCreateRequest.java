package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryItemCreateRequest {
    private Long accountSetId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private String category;
    private String remark;
}
