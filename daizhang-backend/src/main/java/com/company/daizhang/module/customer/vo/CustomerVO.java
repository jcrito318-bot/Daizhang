package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户视图对象
 */
@Data
public class CustomerVO {

    private Long id;

    private String customerCode;

    private String customerName;

    private String customerType;

    private String industry;

    private String scale;

    private String taxpayerType;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private String address;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private Integer status;

    private Long accountSetId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
