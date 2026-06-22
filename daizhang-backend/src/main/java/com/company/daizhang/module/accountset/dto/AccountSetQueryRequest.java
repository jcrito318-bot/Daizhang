package com.company.daizhang.module.accountset.dto;

import lombok.Data;

/**
 * 账套查询请求
 */
@Data
public class AccountSetQueryRequest {
    
    private String code;
    
    private String name;
    
    private String companyName;
    
    private Integer status;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
}
