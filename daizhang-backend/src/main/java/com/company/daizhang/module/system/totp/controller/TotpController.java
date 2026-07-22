package com.company.daizhang.module.system.totp.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.system.dto.TotpCodeRequest;
import com.company.daizhang.module.system.totp.service.TotpService;
import com.company.daizhang.module.system.totp.vo.TotpEnableResponse;
import com.company.daizhang.module.system.totp.vo.TotpSetupVO;
import com.company.daizhang.module.system.totp.vo.TotpStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * TOTP 双因素认证控制器 (P4.2)
 * <p>
 * 路径 /auth/totp/** 位于 SecurityConfig 的 /auth/** permitAll 范围内,
 * 但每个端点通过 {@link SecurityUtils#getCurrentUserIdRequired()} 强制校验登录态,
 * 与现有 /auth/info 模式一致。
 */
@Slf4j
@RestController
@RequestMapping("/auth/totp")
@RequiredArgsConstructor
@Tag(name = "双因素认证", description = "TOTP 2FA 设置与状态查询")
public class TotpController {

    private final TotpService totpService;

    @PostMapping("/setup")
    @Operation(summary = "生成 TOTP 密钥(返回二维码内容)")
    public Result<TotpSetupVO> setup() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        TotpSetupVO vo = totpService.generateSecret(userId);
        return Result.success(vo);
    }

    @PostMapping("/enable")
    @Operation(summary = "启用 2FA(校验验证码,生成备用码)")
    public Result<TotpEnableResponse> enable(@Valid @RequestBody TotpCodeRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        TotpEnableResponse resp = totpService.enableTotp(userId, request.getCode());
        return Result.success("2FA 启用成功", resp);
    }

    @PostMapping("/disable")
    @Operation(summary = "禁用 2FA(校验验证码)")
    public Result<Void> disable(@Valid @RequestBody TotpCodeRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        totpService.disableTotp(userId, request.getCode());
        return Result.success("2FA 已禁用", null);
    }

    @GetMapping("/status")
    @Operation(summary = "查询当前用户 2FA 状态")
    public Result<TotpStatusVO> status() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        TotpStatusVO vo = totpService.getStatus(userId);
        return Result.success(vo);
    }
}
