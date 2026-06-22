package com.company.daizhang.module.bank.enums;

import lombok.Getter;

/**
 * 对账状态枚举
 */
@Getter
public enum ReconciliationStatus {

    UNRECONCILED(0, "未对账"),
    RECONCILED(1, "已对账");

    private final Integer code;
    private final String description;

    ReconciliationStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
