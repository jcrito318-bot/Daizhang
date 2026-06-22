package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务合同视图对象
 */
@Data
public class ContractVO {

    private Long id;

    private String contractNo;

    private Long customerId;

    private String customerName;

    private String contractName;

    private String contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String serviceContent;

    private BigDecimal amount;

    private String paymentMethod;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
