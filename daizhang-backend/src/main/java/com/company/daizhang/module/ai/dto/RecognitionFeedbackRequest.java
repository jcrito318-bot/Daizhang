package com.company.daizhang.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 识别反馈提交请求 DTO
 * <p>
 * 用户在前端选择实际借贷科目后,通过本接口反馈,用于:
 * 1. 作为 few-shot 示例注入后续 AI prompt
 * 2. 由定时任务聚合高频反馈自动生成规则(autoLearnFromFeedback)
 */
@Data
public class RecognitionFeedbackRequest {

    /**
     * 账套ID
     */
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    /**
     * 原始业务描述
     */
    @NotBlank(message = "业务描述不能为空")
    private String originalDescription;

    /**
     * AI建议借方科目编码(可能为空,表示规则命中或无建议)
     */
    private String aiSuggestedDebitCode;

    /**
     * AI建议贷方科目编码
     */
    private String aiSuggestedCreditCode;

    /**
     * 用户实际选择借方科目编码
     */
    @NotBlank(message = "实际借方科目编码不能为空")
    private String actualDebitCode;

    /**
     * 用户实际选择贷方科目编码
     */
    @NotBlank(message = "实际贷方科目编码不能为空")
    private String actualCreditCode;

    /**
     * 用户实际摘要
     */
    private String actualSummary;

    /**
     * 是否采纳AI建议 0-未采纳 1-部分采纳 2-完全采纳
     * 后端会基于 aiSuggested* 与 actual* 自动计算,前端传值仅作参考校验
     */
    private Integer accepted;
}
