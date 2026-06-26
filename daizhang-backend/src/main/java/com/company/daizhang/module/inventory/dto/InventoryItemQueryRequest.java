package com.company.daizhang.module.inventory.dto;

import lombok.Data;

@Data
public class InventoryItemQueryRequest {
    private Long accountSetId;
    private String itemCode;
    private String itemName;
    private String category;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
