package com.company.daizhang.module.customer.dto;

import lombok.Data;

/**
 * 客户开票记录查询请求
 */
@Data
public class BillingRecordQueryRequest {

    private Long customerId;

    private Long contractId;

    private Integer invoiceType;

    private Integer status;

    private String invoiceNo;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
