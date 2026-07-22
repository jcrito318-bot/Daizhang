package com.company.daizhang.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import com.company.daizhang.common.annotation.FieldEncrypt;
import com.company.daizhang.common.crypto.annotation.EncryptedField;
import com.company.daizhang.common.crypto.enums.MaskType;
import com.company.daizhang.common.crypto.mybatis.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * P4.2: 新增 2FA 相关字段(twoFactorEnabled / twoFactorSecret / twoFactorBackupCodes)。
 * P4.3: 新增密码策略与登录锁定相关字段(passwordChangedAt / loginFailCount / lockedUntil)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser extends BaseEntity {

    private String username;

    private String password;

    private String realName;

    /**
     * 手机号 (P4.1: AES-GCM 加密存储,读库自动解密;对外展示脱敏)
     */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @EncryptedField("用户手机号")
    @FieldEncrypt(maskType = MaskType.PHONE)
    private String phone;

    private String email;

    private String avatar;

    private Integer status;

    /**
     * 是否启用 2FA (P4.2):0-未启用 1-已启用
     */
    private Integer twoFactorEnabled;

    /**
     * TOTP 密钥 (P4.2):加密存储,仅在用户启用 2FA 时非空。
     */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @EncryptedField("2FA TOTP 密钥")
    private String twoFactorSecret;

    /**
     * 2FA 备用恢复码 (P4.2):JSON 数组字符串,加密存储。
     * 用户无法访问 TOTP 设备时可用备用码登录,每枚一次性使用。
     */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @EncryptedField("2FA 备用恢复码")
    private String twoFactorBackupCodes;

    /**
     * 最后改密时间 (P4.3):用于强制改密策略(默认 90 天过期)。
     */
    private LocalDateTime passwordChangedAt;

    /**
     * 登录失败次数 (P4.3):连续失败累计,达到阈值(默认 5)后锁定账户。
     */
    private Integer loginFailCount;

    /**
     * 锁定截止时间 (P4.3):非空且大于当前时间则账户被锁定,登录返回 423。
     */
    private LocalDateTime lockedUntil;
}
