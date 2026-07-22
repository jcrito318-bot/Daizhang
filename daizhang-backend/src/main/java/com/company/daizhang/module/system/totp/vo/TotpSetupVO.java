package com.company.daizhang.module.system.totp.vo;

import lombok.Data;

/**
 * TOTP 设置响应 (P4.2)
 * <p>
 * 调用 /auth/totp/setup 后返回,前端据此渲染二维码与密钥。
 * qrCodeBase64 由前端根据 otpauthUrl 自行生成(后端无 QR 库依赖)。
 */
@Data
public class TotpSetupVO {

    /** Base32 编码的 TOTP 密钥(供手动输入) */
    private String secret;

    /** otpauth:// URL(二维码内容) */
    private String otpauthUrl;

    /** QR 码 Base64(Data URL),前端无 QR 库时可使用;当前由前端生成,此字段预留为 null */
    private String qrCodeBase64;
}
