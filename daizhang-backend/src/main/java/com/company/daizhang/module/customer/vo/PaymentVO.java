package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录视图对象
 */
@Data
public class PaymentVO {

    private Long id;

    private Long contractId;

    private String contractNo;

    private Long customerId;

    private String customerName;

    private LocalDate paymentDate;

    private BigDecimal amount;

    private String paymentMethod;

    private String paymentType;

    private String voucherNo;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
