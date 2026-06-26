package com.company.daizhang.module.inventory.dto;

import lombok.Data;

@Data
public class InventoryOutQueryRequest {
    private Long accountSetId;
    private String outNo;
    private Integer outType;
    private Integer status;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
