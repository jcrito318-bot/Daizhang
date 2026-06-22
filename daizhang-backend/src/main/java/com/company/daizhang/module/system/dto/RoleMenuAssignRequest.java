package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单请求
 */
@Data
public class RoleMenuAssignRequest {
    
    @NotEmpty(message = "菜单ID列表不能为空")
    private List<Long> menuIds;
}
