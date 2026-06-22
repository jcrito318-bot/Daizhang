package com.company.daizhang.common.enums;

import lombok.Getter;

/**
 * 余额方向枚举
 */
@Getter
public enum BalanceDirection {

    DEBIT(1, "借"),
    CREDIT(2, "贷");

    private final Integer code;
    private final String description;

    BalanceDirection(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
