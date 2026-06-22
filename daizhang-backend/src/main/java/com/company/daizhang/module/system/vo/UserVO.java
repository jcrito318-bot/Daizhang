package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象
 */
@Data
public class UserVO {
    
    private Long id;
    
    private String username;
    
    private String realName;
    
    private String phone;
    
    private String email;
    
    private String avatar;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private List<String> roles;
    
    private List<String> permissions;
    
    private List<MenuVO> menus;
}
