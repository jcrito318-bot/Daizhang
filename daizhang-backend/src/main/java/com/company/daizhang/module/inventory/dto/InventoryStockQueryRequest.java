package com.company.daizhang.module.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InventoryStockQueryRequest {
    private Long accountSetId;
    private Long itemId;
    private String itemName;
    private String category;
    private Integer year;
    private Integer month;
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
