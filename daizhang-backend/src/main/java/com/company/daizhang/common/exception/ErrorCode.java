package com.company.daizhang.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 账套相关 2001-2099
    ACCOUNT_SET_NOT_FOUND(2001, "账套不存在"),
    ACCOUNT_SET_CODE_DUPLICATE(2002, "账套编码已存在"),
    ACCOUNT_SET_HAS_VOUCHERS(2003, "账套下存在凭证，无法删除"),
    ACCOUNT_SET_INITIALIZED(2004, "账套已初始化，不能重复初始化"),
    ACCOUNT_SET_PERIOD_NOT_FOUND(2005, "会计期间不存在"),
    ACCOUNT_SET_PERIOD_ALREADY_EXISTS(2006, "该年度的会计期间已存在"),
    ACCOUNT_SET_PERIOD_ALREADY_CLOSED(2007, "该期间已结账"),
    ACCOUNT_SET_PERIOD_NOT_CLOSED(2008, "该期间未结账"),

    // 科目相关 2101-2199
    SUBJECT_NOT_FOUND(2101, "科目不存在"),
    SUBJECT_ALREADY_EXISTS(2102, "科目已存在"),
    SUBJECT_CODE_DUPLICATE(2103, "科目编码已存在"),
    SUBJECT_CODE_INVALID(2104, "科目编码格式不正确"),
    SUBJECT_HAS_CHILDREN(2105, "该科目存在下级科目，无法删除"),
    SUBJECT_HAS_VOUCHERS(2106, "该科目已被凭证使用，无法删除"),
    SUBJECT_PARENT_NOT_FOUND(2107, "上级科目不存在"),
    SUBJECT_PARENT_IS_LEAF(2108, "上级科目为末级科目，不能添加下级科目"),
    SUBJECT_LEVEL_EXCEED(2109, "科目层级不能超过4级"),
    SUBJECT_CATEGORY_MISMATCH(2110, "科目类别与上级科目不一致"),
    SUBJECT_BALANCE_DIRECTION_INVALID(2111, "科目余额方向不正确"),
    SUBJECT_CODE_BLANK(2112, "科目编码不能为空"),
    SUBJECT_NAME_BLANK(2113, "科目名称不能为空"),
    SUBJECT_CATEGORY_BLANK(2114, "科目类别不能为空"),
    SUBJECT_BALANCE_DIRECTION_BLANK(2115, "科目余额方向不能为空"),

    // 凭证相关 3001-3099
    VOUCHER_NOT_FOUND(3001, "凭证不存在"),
    VOUCHER_BALANCE_ERROR(3002, "凭证借贷不平衡"),
    VOUCHER_ALREADY_AUDITED(3003, "凭证已审核"),
    VOUCHER_ALREADY_POSTED(3004, "凭证已过账"),
    PERIOD_CLOSED(3005, "会计期间已结账"),
    VOUCHER_DATE_INVALID(3006, "凭证日期不在当前会计期间范围内"),
    VOUCHER_DETAIL_EMPTY(3007, "凭证明细不能为空"),
    VOUCHER_SUBJECT_INVALID(3008, "凭证科目不存在或已停用"),
    VOUCHER_DEBIT_CREDIT_BOTH_ZERO(3009, "借贷方金额不能同时为零"),
    VOUCHER_DEBIT_CREDIT_BOTH_NONZERO(3010, "同一行借贷方金额不能同时有值"),
    VOUCHER_AMOUNT_NEGATIVE(3011, "金额不能为负数"),
    VOUCHER_NOT_AUDITED(3012, "凭证未审核，不能过账"),
    VOUCHER_DATE_OUT_OF_RANGE(3013, "凭证日期超出会计期间范围"),
    VOUCHER_SUMMARY_BLANK(3014, "凭证摘要不能为空"),
    VOUCHER_SUBJECT_ID_BLANK(3015, "科目ID不能为空"),
    VOUCHER_DEBIT_BLANK(3016, "借方金额不能为空"),
    VOUCHER_CREDIT_BLANK(3017, "贷方金额不能为空"),
    VOUCHER_DATE_BLANK(3018, "凭证日期不能为空"),
    VOUCHER_YEAR_BLANK(3019, "年度不能为空"),
    VOUCHER_MONTH_BLANK(3020, "月份不能为空"),
    VOUCHER_YEAR_INVALID(3021, "年度格式不正确"),
    VOUCHER_MONTH_INVALID(3022, "月份必须在1-12之间"),
    VOUCHER_ALREADY_CANCELED(3023, "凭证已作废"),
    VOUCHER_TEMPLATE_NOT_FOUND(3031, "凭证模板不存在"),
    VOUCHER_TEMPLATE_DETAIL_EMPTY(3032, "凭证模板明细不能为空"),
    VOUCHER_TEMPLATE_CODE_DUPLICATE(3033, "模板编码已存在"),
    ABSTRACT_NOT_FOUND(3034, "常用摘要不存在"),
    ABSTRACT_TEXT_BLANK(3035, "摘要文本不能为空"),

    // 票据相关 3101-3199
    DOCUMENT_NOT_FOUND(3101, "票据不存在"),
    DOCUMENT_ALREADY_LINKED(3102, "票据已关联凭证"),
    DOCUMENT_NOT_LINKED(3103, "票据未关联凭证"),

    // 发票相关 3201-3299
    INPUT_INVOICE_NOT_FOUND(3201, "进项发票不存在"),
    INPUT_INVOICE_ALREADY_AUTHENTICATED(3202, "进项发票已认证"),
    INPUT_INVOICE_ALREADY_VOID(3203, "进项发票已作废"),
    OUTPUT_INVOICE_NOT_FOUND(3204, "销项发票不存在"),
    OUTPUT_INVOICE_ALREADY_VOID(3205, "销项发票已作废"),
    OUTPUT_INVOICE_ALREADY_RED(3206, "销项发票已红冲"),
    INVOICE_NUMBER_DUPLICATE(3207, "发票号码已存在"),

    // 系统管理相关 4001-4099
    USER_NOT_FOUND(4001, "用户不存在"),
    USER_ALREADY_EXISTS(4002, "用户名已存在"),
    USER_CANNOT_DELETE_ADMIN(4003, "不能删除管理员账户"),
    USER_STATUS_INVALID(4004, "用户状态值不正确"),
    USER_PHONE_INVALID(4005, "手机号格式不正确"),
    USER_EMAIL_INVALID(4006, "邮箱格式不正确"),
    USER_PASSWORD_TOO_SHORT(4007, "密码长度不能少于6位"),
    ROLE_NOT_FOUND(4010, "角色不存在"),
    ROLE_CODE_DUPLICATE(4011, "角色编码已存在"),
    ROLE_HAS_USERS(4012, "该角色已被分配给用户，无法删除"),
    MENU_NOT_FOUND(4020, "菜单不存在"),
    MENU_HAS_CHILDREN(4021, "存在子菜单，无法删除"),
    MENU_PARENT_NOT_FOUND(4022, "上级菜单不存在"),
    MENU_SELF_REFERENCE(4023, "上级菜单不能选择自身"),

    // 期末处理相关 4101-4199
    PERIOD_NOT_FOUND(4101, "会计期间不存在"),
    PERIOD_ALREADY_CLOSED(4102, "会计期间已结账"),
    PERIOD_NOT_CLOSED(4103, "会计期间未结账"),
    PERIOD_HAS_SUBSEQUENT_CLOSED(4104, "存在已结账的后续期间，无法反结账"),
    TRIAL_BALANCE_NOT_BALANCED(4105, "试算不平衡，无法结账"),
    VOUCHER_NOT_ALL_AUDITED(4106, "存在未审核的凭证，无法结账"),
    VOUCHER_NOT_ALL_POSTED(4107, "存在未过账的凭证，无法结账"),
    PROFIT_SUBJECT_NOT_FOUND(4108, "未找到本年利润科目"),

    // 账簿查询相关 5001-5099
    LEDGER_ACCOUNT_SET_NOT_FOUND(5001, "账套不存在"),
    LEDGER_SUBJECT_NOT_FOUND(5002, "科目不存在"),
    LEDGER_YEAR_INVALID(5003, "年度格式不正确"),
    LEDGER_MONTH_INVALID(5004, "月份必须在1-12之间"),
    LEDGER_DATE_RANGE_INVALID(5005, "开始日期不能大于结束日期"),
    LEDGER_SUBJECT_ID_BLANK(5006, "科目ID不能为空"),

    // 并发控制相关 6001-6099
    CONCURRENT_UPDATE_FAILED(6001, "数据已被修改，请刷新后重试"),

    // AI/OCR 相关 7001-7099
    AI_OCR_PARSE_ERROR(7001, "OCR识别结果解析失败"),
    AI_OCR_FIELD_PARSE_ERROR(7002, "OCR字段解析失败"),
    AI_NOT_ENABLED(7003, "AI功能未启用"),
    AI_API_CALL_ERROR(7004, "AI服务调用失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
