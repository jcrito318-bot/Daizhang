package com.company.daizhang.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户更新请求
 */
@Data
public class UserUpdateRequest {
    
    private String realName;
    
    private String phone;
    
    private String email;
    
    private String avatar;
    
    private Integer status;
    
    private List<Long> roleIds;
}
