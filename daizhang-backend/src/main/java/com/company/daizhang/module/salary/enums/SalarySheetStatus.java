package com.company.daizhang.module.salary.enums;

import lombok.Getter;

/**
 * 薪资表状态枚举
 */
@Getter
public enum SalarySheetStatus {

    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    PAID(2, "已发放");

    private final Integer code;
    private final String description;

    SalarySheetStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
