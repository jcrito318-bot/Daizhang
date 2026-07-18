package com.company.daizhang.module.accountset.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
