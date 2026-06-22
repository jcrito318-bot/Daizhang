package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单视图对象
 */
@Data
public class MenuVO {
    
    private Long id;
    
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
    
    private LocalDateTime createTime;
    
    private List<MenuVO> children;
}
