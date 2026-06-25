package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进项发票视图对象
 */
@Data
public class InputInvoiceVO {

    private Long id;

    private Long accountSetId;

    private String invoiceCode;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private String invoiceType;

    private String sellerName;

    private String sellerTaxNumber;

    private String buyerName;

    private String buyerTaxNumber;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private BigDecimal taxRate;

    private Integer authStatus;

    private LocalDate authDate;

    private Long voucherId;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
