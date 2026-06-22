package com.company.daizhang.module.document.dto;

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
