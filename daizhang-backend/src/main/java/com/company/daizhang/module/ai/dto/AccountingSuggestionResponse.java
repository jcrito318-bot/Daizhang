package com.company.daizhang.module.ai.dto;

import lombok.Data;

/**
 * 智能记账建议响应 DTO
 * <p>
 * source 取值:
 * - rule: 规则库命中(置信度固定 1.0,无需调用 AI)
 * - ai:   规则未命中,由 GLM 推理得出(置信度由模型给出)
 */
@Data
public class AccountingSuggestionResponse {

    /**
     * 建议来源: rule / ai
     */
    private String source;

    /**
     * 借方科目编码
     */
    private String debitSubjectCode;

    /**
     * 借方科目名称
     */
    private String debitSubjectName;

    /**
     * 贷方科目编码
     */
    private String creditSubjectCode;

    /**
     * 贷方科目名称
     */
    private String creditSubjectName;

    /**
     * 建议摘要
     */
    private String summary;

    /**
     * 置信度 0-1,rule 命中固定 1.0,ai 推理由模型给出
     */
    private Double confidence;

    /**
     * 命中的规则ID(仅 source=rule 时有值),用于命中计数
     */
    private Long ruleId;

    /**
     * 原始 AI 返回内容(仅 source=ai 时有值,便于前端调试/透传)
     */
    private String rawAiResponse;
}
