package com.company.daizhang.module.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 票据实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_document")
public class Document extends BaseEntity {

    private Long accountSetId;

    /**
     * 票据编号
     */
    private String documentNo;

    /**
     * 票据类型：1-发票 2-银行回单 3-费用单据 4-其他
     */
    private Integer documentType;

    /**
     * 票据日期
     */
    private LocalDate documentDate;

    /**
     * 金额
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
     * 销方名称
     */
    private String sellerName;

    /**
     * 购方名称
     */
    private String buyerName;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * OCR识别内容
     */
    private String ocrContent;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 状态：0-待处理 1-已关联凭证 2-已完成
     */
    private Integer status;

    /**
     * 关联凭证ID
     */
    private Long voucherId;

    /**
     * 备注
     */
    private String remark;
}
