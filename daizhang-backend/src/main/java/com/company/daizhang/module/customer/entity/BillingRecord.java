package com.company.daizhang.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户开票记录实体（代账公司给客户开服务费发票）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cst_billing_record")
public class BillingRecord extends BaseEntity {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 开票日期
     */
    private LocalDate billingDate;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 发票类型 1-增值税专用发票 2-增值税普通发票 3-电子普通发票
     */
    private Integer invoiceType;

    /**
     * 开票金额（含税）
     */
    private BigDecimal amount;

    /**
     * 税率（如0.06）
     */
    private BigDecimal taxRate;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 不含税金额
     */
    private BigDecimal amountWithoutTax;

    /**
     * 开票内容/商品名称
     */
    private String billingContent;

    /**
     * 状态 0-已开票未收款 1-已收款 2-已作废
     */
    private Integer status;

    /**
     * 关联收款记录ID
     */
    private Long paymentRecordId;

    /**
     * 备注
     */
    private String remark;
}
