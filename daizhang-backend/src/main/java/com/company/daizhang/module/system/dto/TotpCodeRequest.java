package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * TOTP 验证码请求 (P4.2)
 * <p>
 * 用于启用/禁用 2FA 时提交 6 位验证码或备用码。
 */
@Data
public class TotpCodeRequest {

    /** 6 位 TOTP 验证码或备用恢复码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
