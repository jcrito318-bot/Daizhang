package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进项发票请求
 */
@Data
public class InputInvoiceRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private String invoiceCode;

    @NotBlank(message = "发票号码不能为空")
    private String invoiceNumber;

    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @NotBlank(message = "发票类型不能为空")
    private String invoiceType;

    @NotBlank(message = "销方名称不能为空")
    private String sellerName;

    private String sellerTaxNumber;

    private String buyerName;

    private String buyerTaxNumber;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围")
    private BigDecimal amount;

    @NotNull(message = "税额不能为空")
    @DecimalMin(value = "0", message = "税额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "税额精度超出范围")
    private BigDecimal taxAmount;

    @NotNull(message = "价税合计不能为空")
    @DecimalMin(value = "0", message = "价税合计不能为负数")
    @Digits(integer = 15, fraction = 2, message = "价税合计精度超出范围")
    private BigDecimal totalAmount;

    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "税率精度超出范围")
    private BigDecimal taxRate;

    private String remark;
}
