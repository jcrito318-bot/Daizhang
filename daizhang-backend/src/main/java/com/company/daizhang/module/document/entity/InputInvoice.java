package com.company.daizhang.module.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进项发票实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_input_invoice")
public class InputInvoice extends BaseEntity {

    private Long accountSetId;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * 开票日期
     */
    private LocalDate invoiceDate;

    /**
     * 发票类型:增值税专用发票/增值税普通发票/电子发票
     */
    private String invoiceType;

    /**
     * 销方名称
     */
    private String sellerName;

    /**
     * 销方税号
     */
    private String sellerTaxNumber;

    /**
     * 购方名称
     */
    private String buyerName;

    /**
     * 购方税号
     */
    private String buyerTaxNumber;

    /**
     * 金额(不含税)
     */
    private BigDecimal amount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    private BigDecimal totalAmount;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 认证状态 0-未认证 1-已认证 2-已作废
     */
    private Integer authStatus;

    /**
     * 认证日期
     */
    private LocalDate authDate;

    /**
     * 关联凭证ID
     */
    private Long voucherId;

    /**
     * 备注
     */
    private String remark;
}
