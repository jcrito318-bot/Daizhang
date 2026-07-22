package com.company.daizhang.module.system.totp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 TOTP 双因素认证实体 (P4.2)
 * <p>
 * 对应 user_totp 表,每个用户最多一条记录(uk_user 唯一约束)。
 */
@Data
@TableName("user_totp")
public class UserTotp implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** TOTP 密钥(Base32 编码) */
    private String secret;

    /** 是否启用 0=未启用 1=已启用 */
    private Integer enabled;

    /** 备用恢复码 JSON 数组字符串(明文比对,简化方案) */
    private String backupCodes;

    /** 启用时间 */
    private LocalDateTime enabledAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
