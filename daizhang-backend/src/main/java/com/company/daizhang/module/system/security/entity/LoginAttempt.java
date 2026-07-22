package com.company.daizhang.module.system.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录尝试记录实体 (P4.3)
 * <p>
 * 对应 login_attempt 表,用于登录锁定判定。
 */
@Data
@TableName("login_attempt")
public class LoginAttempt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 尝试时间 */
    private LocalDateTime attemptTime;

    /** 客户端IP */
    private String ip;

    /** 是否成功 0-失败 1-成功 */
    private Integer success;
}
