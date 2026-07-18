package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 登出请求 DTO
 * <p>
 * S-007 修复:登出时前端将本地存储的 refresh token 通过 body 传入,后端一并吊销,
 * 防止攻击者窃取 refresh token 后绕过登出换取新 access token。
 */
@Data
public class LogoutRequest {

    /**
     * refresh token (Bearer xxx 或裸 token 均可,后端会统一处理)
     */
    private String refreshToken;
}
