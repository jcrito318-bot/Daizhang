package com.company.daizhang.module.system.totp.vo;

import lombok.Data;

/**
 * TOTP 状态响应 (P4.2)
 */
@Data
public class TotpStatusVO {

    /** 是否已启用 2FA */
    private Boolean enabled;

    /** 是否已生成密钥(未启用但已 setup) */
    private Boolean secretGenerated;
}
