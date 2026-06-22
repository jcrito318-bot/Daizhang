package com.company.daizhang.module.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务合同更新请求
 */
@Data
public class ContractUpdateRequest {

    private String contractName;

    private String contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String serviceContent;

    private BigDecimal amount;

    private String paymentMethod;

    private Integer status;

    private String remark;
}
