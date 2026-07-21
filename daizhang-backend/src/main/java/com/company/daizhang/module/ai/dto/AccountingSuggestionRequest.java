package com.company.daizhang.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能记账建议请求 DTO
 * <p>
 * 与旧版 {@link AccountingSuggestRequest} 区别:
 * - 强制 accountSetId(用于规则匹配/few-shot 注入/反馈归档)
 * - amount 允许为 null(部分纯文本咨询场景无金额)
 */
@Data
public class AccountingSuggestionRequest {

    /**
     * 业务描述
     */
    @NotBlank(message = "业务描述不能为空")
    private String description;

    /**
     * 金额(元),允许为 null
     */
    private Double amount;

    /**
     * 账套ID(必填,用于匹配账套级规则与注入科目体系)
     */
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
}
