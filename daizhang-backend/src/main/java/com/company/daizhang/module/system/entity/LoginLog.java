package com.company.daizhang.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("sys_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long userId;

    private Integer loginType;

    private Integer loginStatus;

    private String loginIp;

    private String loginLocation;

    private String browser;

    private String os;

    private String message;

    private LocalDateTime loginTime;
}
