package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 角色查询请求
 */
@Data
public class RoleQueryRequest {
    
    private String roleName;
    
    private String roleCode;
    
    private Integer status;
    
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
