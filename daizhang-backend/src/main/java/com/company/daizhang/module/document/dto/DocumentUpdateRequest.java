package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 票据更新请求
 */
@Data
public class DocumentUpdateRequest {

    private String documentNo;

    private Integer documentType;

    private LocalDate documentDate;

    @DecimalMin(value = "0", message = "金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围")
    private BigDecimal amount;

    @DecimalMin(value = "0", message = "税额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "税额精度超出范围")
    private BigDecimal taxAmount;

    @DecimalMin(value = "0", message = "价税合计不能为负数")
    @Digits(integer = 15, fraction = 2, message = "价税合计精度超出范围")
    private BigDecimal totalAmount;

    private String sellerName;

    private String buyerName;

    private String invoiceCode;

    private String invoiceNumber;

    private String ocrContent;

    private String fileUrl;

    private String remark;
}
