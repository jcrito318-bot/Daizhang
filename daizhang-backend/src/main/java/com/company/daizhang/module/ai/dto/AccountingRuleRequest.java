package com.company.daizhang.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 记账规则 CRUD 请求 DTO
 * <p>
 * accountSetId 由后端按当前用户权限自动注入或校验,前端无需(也不应)直接指定,
 * 防止越权为他人账套创建规则。accountSetId=0 表示全局规则,仅 ADMIN 可写。
 */
@Data
public class AccountingRuleRequest {

    /**
     * 账套ID(0=全局规则,仅 ADMIN 可写;其他值须为当前用户有权限的账套)
     */
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    /**
     * 业务关键词
     */
    @NotBlank(message = "关键词不能为空")
    private String keyword;

    /**
     * 借方科目编码
     */
    @NotBlank(message = "借方科目编码不能为空")
    private String debitSubjectCode;

    /**
     * 借方科目名称
     */
    private String debitSubjectName;

    /**
     * 贷方科目编码
     */
    @NotBlank(message = "贷方科目编码不能为空")
    private String creditSubjectCode;

    /**
     * 贷方科目名称
     */
    private String creditSubjectName;

    /**
     * 建议摘要
     */
    private String voucherSummary;

    /**
     * 优先级,数字越小越优先(默认 100)
     */
    private Integer priority;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;
}
