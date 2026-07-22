package com.company.daizhang.module.system.dto;

import com.company.daizhang.module.system.vo.UserVO;
import lombok.Data;

/**
 * 登录响应
 * <p>
 * P4.2/P4.3 扩展字段:
 * <ul>
 *     <li>requiresTwoFactor:用户已启用 2FA 时为 true,前端跳转 2FA 输入页,tempToken 用于调 /auth/login/totp</li>
 *     <li>tempToken:2FA 临时 token(5 分钟有效),仅 requiresTwoFactor=true 时返回</li>
 *     <li>passwordExpired:密码已过期需强制改密时为 true,前端跳转改密页</li>
 *     <li>remainingAttempts:登录失败时的剩余尝试次数(用于提示用户)</li>
 * </ul>
 */
@Data
public class LoginResponse {

    private String token;
    private String refreshToken;
    private UserVO userInfo;

    /** 是否需要双因素认证 */
    private Boolean requiresTwoFactor;

    /** 2FA 临时 token(密码验证通过后下发,用于 /auth/login/totp) */
    private String tempToken;

    /** 密码是否已过期(需强制改密) */
    private Boolean passwordExpired;

    /** 剩余登录尝试次数(锁定前) */
    private Integer remainingAttempts;
}
