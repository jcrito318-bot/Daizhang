package com.company.daizhang.module.system.totp.vo;

import lombok.Data;

import java.util.List;

/**
 * TOTP 启用响应 (P4.2)
 * <p>
 * 启用 2FA 成功后返回备用恢复码,仅此一次展示,用户需自行保存。
 */
@Data
public class TotpEnableResponse {

    /** 是否启用成功 */
    private Boolean enabled;

    /** 备用恢复码列表(10 个,每个一次性使用) */
    private List<String> backupCodes;
}
