package com.company.daizhang.module.customer.dto;

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

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
