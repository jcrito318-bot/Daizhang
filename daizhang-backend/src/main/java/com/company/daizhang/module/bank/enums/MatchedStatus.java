package com.company.daizhang.module.bank.enums;

import lombok.Getter;

/**
 * 匹配状态枚举
 */
@Getter
public enum MatchedStatus {

    UNMATCHED(0, "未匹配"),
    MATCHED(1, "已匹配");

    private final Integer code;
    private final String description;

    MatchedStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
