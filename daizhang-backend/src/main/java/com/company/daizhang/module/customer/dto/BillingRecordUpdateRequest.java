package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0", message = "开票金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "开票金额精度超出范围")
    private BigDecimal amount;

    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "税率精度超出范围")
    private BigDecimal taxRate;

    private String billingContent;

    private Integer status;

    private Long paymentRecordId;

    private String remark;
}
