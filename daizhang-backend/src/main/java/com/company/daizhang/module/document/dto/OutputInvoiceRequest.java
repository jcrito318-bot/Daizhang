package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销项发票请求
 */
@Data
public class OutputInvoiceRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private String invoiceCode;

    @NotBlank(message = "发票号码不能为空")
    private String invoiceNumber;

    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;

    @NotBlank(message = "发票类型不能为空")
    private String invoiceType;

    @NotBlank(message = "购方名称不能为空")
    private String buyerName;

    private String buyerTaxNumber;

    private String sellerName;

    private String sellerTaxNumber;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private BigDecimal taxRate;

    private String remark;
}
