package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 菜单更新请求
 */
@Data
public class MenuUpdateRequest {
    
    private Long parentId;
    
    private String name;
    
    private String path;
    
    private String component;
    
    private String icon;
    
    private Integer sortOrder;
    
    private Integer menuType;
    
    private String permission;
    
    private Integer visible;
    
    private Integer status;
}
