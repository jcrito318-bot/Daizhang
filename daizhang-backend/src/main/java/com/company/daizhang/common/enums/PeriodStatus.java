package com.company.daizhang.common.enums;

import lombok.Getter;

/**
 * 会计期间状态枚举
 */
@Getter
public enum PeriodStatus {

    OPEN(0, "未结账"),
    CLOSED(1, "已结账");

    private final Integer code;
    private final String description;

    PeriodStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
