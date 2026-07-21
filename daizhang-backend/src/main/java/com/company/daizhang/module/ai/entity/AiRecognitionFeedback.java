package com.company.daizhang.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 识别反馈实体
 * <p>
 * 记录用户实际选择的借贷科目 vs AI/规则建议的科目,用于闭环学习:
 * 1. 短期价值:作为 few-shot 示例注入 prompt,提升 GLM 在同账套下的识别准确率
 * 2. 长期价值:定时任务 autoLearnFromFeedback 聚合高频反馈,自动生成账套级规则
 * <p>
 * accepted 取值:
 * 0=未采纳(AI建议与用户实际完全不一致)
 * 1=部分采纳(借/贷一方一致)
 * 2=完全采纳(借贷均一致)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_recognition_feedback")
public class AiRecognitionFeedback extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 原始业务描述
     */
    private String originalDescription;

    /**
     * AI建议借方科目编码
     */
    private String aiSuggestedDebitCode;

    /**
     * AI建议贷方科目编码
     */
    private String aiSuggestedCreditCode;

    /**
     * 用户实际选择借方科目编码
     */
    private String actualDebitCode;

    /**
     * 用户实际选择贷方科目编码
     */
    private String actualCreditCode;

    /**
     * 用户实际摘要
     */
    private String actualSummary;

    /**
     * 是否采纳AI建议 0-未采纳 1-部分采纳 2-完全采纳
     */
    private Integer accepted;
}
