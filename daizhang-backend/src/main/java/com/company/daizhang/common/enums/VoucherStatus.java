package com.company.daizhang.common.enums;

import lombok.Getter;

/**
 * 凭证状态枚举
 */
@Getter
public enum VoucherStatus {

    UNAUDITED(0, "未审核"),
    AUDITED(1, "已审核"),
    POSTED(2, "已过账"),
    CANCELED(3, "已作废");

    private final Integer code;
    private final String description;

    VoucherStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
