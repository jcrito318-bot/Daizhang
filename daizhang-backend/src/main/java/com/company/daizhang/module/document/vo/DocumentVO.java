package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 票据视图对象
 */
@Data
public class DocumentVO {

    private Long id;

    private Long accountSetId;

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

    private Integer status;

    private Long voucherId;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
