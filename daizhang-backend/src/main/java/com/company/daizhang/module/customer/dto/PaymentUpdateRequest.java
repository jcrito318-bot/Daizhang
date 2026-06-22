package com.company.daizhang.module.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录更新请求
 */
@Data
public class PaymentUpdateRequest {

    private Long contractId;

    private LocalDate paymentDate;

    private BigDecimal amount;

    private String paymentMethod;

    private String paymentType;

    private String voucherNo;

    private String remark;
}
