package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 菜单创建请求
 */
@Data
public class MenuCreateRequest {
    
    private Long parentId;
    
    @NotBlank(message = "菜单名称不能为空")
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
