package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户开票记录创建请求
 */
@Data
public class BillingRecordCreateRequest {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    private Long contractId;

    @NotNull(message = "开票日期不能为空")
    private LocalDate billingDate;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 发票类型 1-专票 2-普票 3-电子普票
     */
    private Integer invoiceType;

    @NotNull(message = "开票金额不能为空")
    @DecimalMin(value = "0", message = "开票金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "开票金额精度超出范围")
    private BigDecimal amount;

    /**
     * 税率（如0.06）
     */
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "税率精度超出范围")
    private BigDecimal taxRate;

    /**
     * 开票内容
     */
    private String billingContent;

    private String remark;
}
