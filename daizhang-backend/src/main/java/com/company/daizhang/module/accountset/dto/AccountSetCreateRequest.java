package com.company.daizhang.module.accountset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 账套创建请求
 */
@Data
public class AccountSetCreateRequest {
    
    @NotBlank(message = "账套编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "账套编码只能包含字母、数字、下划线、短横线，长度2-50字符")
    private String code;
    
    @NotBlank(message = "账套名称不能为空")
    private String name;
    
    private String companyName;
    
    private String industryType;
    
    private String accountingStandard;
    
    @NotNull(message = "启用年度不能为空")
    private Integer startYear;
    
    @NotNull(message = "启用月份不能为空")
    private Integer startMonth;
    
    private String currencyCode;
    
    private String taxpayerType;
    
    private String contactPerson;
    
    private String contactPhone;
    
    private String address;
}
