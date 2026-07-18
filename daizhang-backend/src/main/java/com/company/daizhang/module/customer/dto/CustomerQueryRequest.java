package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 客户查询请求
 */
@Data
public class CustomerQueryRequest {

    private String customerCode;

    private String customerName;

    private String customerType;

    private String industry;

    private String taxpayerType;

    private String contactPhone;

    private Integer status;

    private Long accountSetId;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
