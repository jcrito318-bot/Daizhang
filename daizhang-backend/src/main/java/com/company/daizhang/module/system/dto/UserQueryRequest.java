package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 用户查询请求
 */
@Data
public class UserQueryRequest {
    
    private String username;
    
    private String realName;
    
    private String phone;
    
    private Integer status;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
}
