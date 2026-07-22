package com.company.daizhang.module.system.totp.service;

import com.company.daizhang.module.system.totp.vo.TotpEnableResponse;
import com.company.daizhang.module.system.totp.vo.TotpSetupVO;
import com.company.daizhang.module.system.totp.vo.TotpStatusVO;

/**
 * TOTP 双因素认证服务 (P4.2)
 */
public interface TotpService {

    /**
     * 生成密钥(未启用时调用),返回 secret + otpauthUrl(用于二维码)。
     * 若已存在未启用的密钥则复用,避免重复生成。
     *
     * @param userId 用户ID
     * @return 设置信息(secret, otpauthUrl, qrCodeBase64)
     */
    TotpSetupVO generateSecret(Long userId);

    /**
     * 校验 code,启用 2FA,生成 10 个备用码。
     *
     * @param userId 用户ID
     * @param code   用户输入的 6 位验证码
     * @return 启用结果(enabled + backupCodes)
     */
    TotpEnableResponse enableTotp(Long userId, String code);

    /**
     * 校验 code 后禁用 2FA。
     *
     * @param userId 用户ID
     * @param code   用户输入的 6 位验证码
     */
    void disableTotp(Long userId, String code);

    /**
     * 验证 6 位 code(登录时调用)。
     *
     * @param userId 用户ID
     * @param code   用户输入的 6 位验证码
     * @return true=验证通过
     */
    boolean verifyCode(Long userId, String code);

    /**
     * 验证备用码(消耗一次,使用后从列表移除)。
     *
     * @param userId 用户ID
     * @param code   用户输入的备用码
     * @return true=验证通过
     */
    boolean verifyBackupCode(Long userId, String code);

    /**
     * 查询当前用户 2FA 状态。
     *
     * @param userId 用户ID
     * @return 状态信息
     */
    TotpStatusVO getStatus(Long userId);

    /**
     * 判断用户是否已启用 2FA。
     *
     * @param userId 用户ID
     * @return true=已启用
     */
    boolean isTwoFactorEnabled(Long userId);
}
