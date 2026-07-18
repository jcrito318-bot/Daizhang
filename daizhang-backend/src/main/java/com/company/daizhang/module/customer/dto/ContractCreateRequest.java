package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务合同创建请求
 */
@Data
public class ContractCreateRequest {

    @NotBlank(message = "合同编号不能为空")
    private String contractNo;

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "合同名称不能为空")
    private String contractName;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 服务内容
     */
    private String serviceContent;

    /**
     * 合同金额
     */
    @NotNull(message = "合同金额不能为空")
    @DecimalMin(value = "0", message = "合同金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "合同金额精度超出范围")
    private BigDecimal amount;

    /**
     * 付款方式
     */
    private String paymentMethod;

    /**
     * 备注
     */
    private String remark;
}
