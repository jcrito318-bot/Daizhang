package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求 (P4.3)
 * <p>
 * 用户自己修改密码(body: {oldPassword, newPassword}),需校验原密码 + 密码策略 + 密码历史。
 */
@Data
public class ChangePasswordRequest {

    /** 原密码 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
