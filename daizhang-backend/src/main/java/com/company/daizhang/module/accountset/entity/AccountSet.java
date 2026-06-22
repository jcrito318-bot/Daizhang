package com.company.daizhang.module.accountset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账套实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_account_set")
public class AccountSet extends BaseEntity {
    
    private String code;
    
    private String name;
    
    private String companyName;
    
    private String industryType;
    
    private String accountingStandard;
    
    private Integer startYear;
    
    private Integer startMonth;
    
    private String currencyCode;
    
    private String taxpayerType;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String address;
    
    private Integer status;
}
