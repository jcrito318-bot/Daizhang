package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 角色查询请求
 */
@Data
public class RoleQueryRequest {
    
    private String roleName;
    
    private String roleCode;
    
    private Integer status;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
}
