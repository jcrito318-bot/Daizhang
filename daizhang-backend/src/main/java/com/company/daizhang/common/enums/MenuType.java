package com.company.daizhang.common.enums;

import lombok.Getter;

/**
 * 菜单类型枚举
 */
@Getter
public enum MenuType {

    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final Integer code;
    private final String description;

    MenuType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
