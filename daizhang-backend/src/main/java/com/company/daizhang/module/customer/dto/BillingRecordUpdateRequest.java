package com.company.daizhang.module.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户开票记录更新请求
 */
@Data
public class BillingRecordUpdateRequest {

    private Long contractId;

    private LocalDate billingDate;

    private String invoiceNo;

    private Integer invoiceType;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private String billingContent;

    private Integer status;

    private Long paymentRecordId;

    private String remark;
}
