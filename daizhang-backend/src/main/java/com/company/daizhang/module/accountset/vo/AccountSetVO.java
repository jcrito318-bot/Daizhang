package com.company.daizhang.module.accountset.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账套视图对象
 */
@Data
public class AccountSetVO {
    
    private Long id;
    
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
    
    private LocalDateTime createTime;
}
