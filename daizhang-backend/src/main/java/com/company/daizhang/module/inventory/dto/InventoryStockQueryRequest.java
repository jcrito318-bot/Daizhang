package com.company.daizhang.module.inventory.dto;

import lombok.Data;

@Data
public class InventoryStockQueryRequest {
    private Long accountSetId;
    private Long itemId;
    private String itemName;
    private String category;
    private Integer year;
    private Integer month;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
