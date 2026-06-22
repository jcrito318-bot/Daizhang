package com.company.daizhang.module.document.dto;

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

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String sellerName;

    private String buyerName;

    private String invoiceCode;

    private String invoiceNumber;

    private String ocrContent;

    private String fileUrl;

    private String remark;
}
