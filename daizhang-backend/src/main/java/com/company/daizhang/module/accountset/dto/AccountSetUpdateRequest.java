package com.company.daizhang.module.accountset.dto;

import lombok.Data;

/**
 * 账套更新请求
 */
@Data
public class AccountSetUpdateRequest {
    
    private String name;
    
    private String companyName;
    
    private String industryType;
    
    private String accountingStandard;
    
    private String taxpayerType;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String address;
    
    private Integer status;
}
