package com.company.daizhang.module.customer.dto;

import lombok.Data;

/**
 * 客户更新请求
 */
@Data
public class CustomerUpdateRequest {

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

    private String remark;
}
