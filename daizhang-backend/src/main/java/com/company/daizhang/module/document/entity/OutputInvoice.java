package com.company.daizhang.module.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销项发票实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_output_invoice")
public class OutputInvoice extends BaseEntity {

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
     * 发票类型
     */
    private String invoiceType;

    /**
     * 购方名称
     */
    private String buyerName;

    /**
     * 购方税号
     */
    private String buyerTaxNumber;

    /**
     * 销方名称
     */
    private String sellerName;

    /**
     * 销方税号
     */
    private String sellerTaxNumber;

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
     * 状态 0-正常 1-已作废 2-已红冲
     */
    private Integer invoiceStatus;

    /**
     * 关联凭证ID
     */
    private Long voucherId;

    /**
     * 备注
     */
    private String remark;
}
