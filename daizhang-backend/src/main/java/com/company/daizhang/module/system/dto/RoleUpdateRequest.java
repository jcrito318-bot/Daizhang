package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 角色更新请求
 */
@Data
public class RoleUpdateRequest {
    
    private String roleName;
    
    private String roleCode;
    
    private String description;
    
    private Integer status;
}
