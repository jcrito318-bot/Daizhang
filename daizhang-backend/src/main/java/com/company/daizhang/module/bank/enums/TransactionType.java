package com.company.daizhang.module.bank.enums;

import lombok.Getter;

/**
 * 交易类型枚举
 */
@Getter
public enum TransactionType {

    INCOME(1, "收入"),
    EXPENSE(2, "支出");

    private final Integer code;
    private final String description;

    TransactionType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
