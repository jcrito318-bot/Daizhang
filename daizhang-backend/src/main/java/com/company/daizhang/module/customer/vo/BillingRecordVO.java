package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户开票记录VO
 */
@Data
public class BillingRecordVO {

    private Long id;

    private Long customerId;

    private Long contractId;

    private LocalDate billingDate;

    private String invoiceNo;

    private Integer invoiceType;

    private String invoiceTypeDesc;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal amountWithoutTax;

    private String billingContent;

    private Integer status;

    private String statusDesc;

    private Long paymentRecordId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
