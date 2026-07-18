package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 票据创建请求
 */
@Data
public class DocumentCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private String documentNo;

    @NotNull(message = "票据类型不能为空")
    private Integer documentType;

    private LocalDate documentDate;

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

    private String sellerName;

    private String buyerName;

    private String invoiceCode;

    private String invoiceNumber;

    private String ocrContent;

    private String fileUrl;

    private String remark;
}
