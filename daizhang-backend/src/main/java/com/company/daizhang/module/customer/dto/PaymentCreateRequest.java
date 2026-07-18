package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录创建请求
 */
@Data
public class PaymentCreateRequest {

    /**
     * 合同ID
     */
    private Long contractId;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    /**
     * 收款日期
     */
    private LocalDate paymentDate;

    /**
     * 收款金额
     */
    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0", message = "收款金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "收款金额精度超出范围")
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
