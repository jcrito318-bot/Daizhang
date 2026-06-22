package com.company.daizhang.module.salary.enums;

import lombok.Getter;

/**
 * 薪资项目类型枚举
 */
@Getter
public enum ItemType {

    FIXED("FIXED", "固定"),
    FLOAT("FLOAT", "浮动"),
    DEDUCT("DEDUCT", "扣款");

    private final String code;
    private final String description;

    ItemType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
