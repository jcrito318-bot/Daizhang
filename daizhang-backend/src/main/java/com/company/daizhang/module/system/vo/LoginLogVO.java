package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志视图对象
 */
@Data
public class LoginLogVO {

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
