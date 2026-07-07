package com.company.daizhang.module.accountset.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账套关系视图对象
 */
@Data
public class UserAccountSetVO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private Long accountSetId;

    private String accountSetName;

    /** 角色类型 OWNER/ACCOUNTANT/VIEWER */
    private String roleType;

    private LocalDateTime createTime;
}
