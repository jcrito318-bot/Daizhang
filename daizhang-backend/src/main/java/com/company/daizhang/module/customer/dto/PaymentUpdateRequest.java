package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0", message = "收款金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "收款金额精度超出范围")
    private BigDecimal amount;

    private String paymentMethod;

    private String paymentType;

    private String voucherNo;

    private String remark;
}
