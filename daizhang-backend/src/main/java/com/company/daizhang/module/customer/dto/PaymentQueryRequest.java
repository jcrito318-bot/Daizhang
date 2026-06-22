package com.company.daizhang.module.customer.dto;

import lombok.Data;

/**
 * 收款记录查询请求
 */
@Data
public class PaymentQueryRequest {

    private Long contractId;

    private Long customerId;

    private String paymentMethod;

    private String paymentType;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
