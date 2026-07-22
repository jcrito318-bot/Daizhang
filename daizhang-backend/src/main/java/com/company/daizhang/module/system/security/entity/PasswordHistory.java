package com.company.daizhang.module.system.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密码历史实体 (P4.3)
 * <p>
 * 对应 password_history 表,改密时记录旧密码 hash,新密码不能与最近 N 次相同。
 */
@Data
@TableName("password_history")
public class PasswordHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 历史密码 hash(BCrypt) */
    private String passwordHash;

    /** 创建时间 */
    private LocalDateTime createTime;
}
