package com.company.daizhang.module.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InventoryInQueryRequest {
    private Long accountSetId;
    private String inNo;
    private Integer inType;
    private Integer status;
    private String startDate;
    private String endDate;
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
