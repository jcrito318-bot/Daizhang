package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 2FA 登录请求 (P4.2)
 * <p>
 * 用户启用 2FA 后,密码验证通过返回 tempToken,前端用 tempToken + code 调 /auth/login/totp 完成登录。
 */
@Data
public class TotpLoginRequest {

    /** 2FA 临时 token(由 /auth/login 密码验证通过后下发) */
    @NotBlank(message = "临时令牌不能为空")
    private String tempToken;

    /** 6 位 TOTP 验证码或备用恢复码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
