package com.company.daizhang.module.inventory.dto;

import lombok.Data;

@Data
public class InventoryInQueryRequest {
    private Long accountSetId;
    private String inNo;
    private Integer inType;
    private Integer status;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
