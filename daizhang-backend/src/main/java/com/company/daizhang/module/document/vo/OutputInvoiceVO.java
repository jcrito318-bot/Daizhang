package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销项发票视图对象
 */
@Data
public class OutputInvoiceVO {

    private Long id;

    private Long accountSetId;

    private String invoiceCode;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private String invoiceType;

    private String buyerName;

    private String buyerTaxNumber;

    private String sellerName;

    private String sellerTaxNumber;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private BigDecimal taxRate;

    private Integer invoiceStatus;

    private Long voucherId;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
