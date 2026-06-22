package com.company.daizhang.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cst_payment_record")
public class PaymentRecord extends BaseEntity {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 收款日期
     */
    private LocalDate paymentDate;

    /**
     * 收款金额
     */
    private BigDecimal amount;

    /**
     * 收款方式
     */
    private String paymentMethod;

    /**
     * 收款类型
     */
    private String paymentType;

    /**
     * 凭证号
     */
    private String voucherNo;

    /**
     * 备注
     */
    private String remark;
}
