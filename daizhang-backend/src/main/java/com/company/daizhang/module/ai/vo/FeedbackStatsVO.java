package com.company.daizhang.module.ai.vo;

import lombok.Data;

/**
 * AI 反馈统计 VO
 * <p>
 * 用于反映指定账套在统计区间内的 AI 建议质量:
 * - totalCount:      反馈总条数(用户实际产生记账建议交互的次数)
 * - fullyAccepted:   完全采纳次数(AI 建议借贷均正确)
 * - partiallyAccepted: 部分采纳次数(借/贷一方正确)
 * - rejected:        未采纳次数(借贷均错误)
 * - acceptanceRate:  综合采纳率 = (完全 + 部分采纳 * 0.5) / 总数
 */
@Data
public class FeedbackStatsVO {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 反馈总条数
     */
    private Long totalCount;

    /**
     * 完全采纳次数(accepted=2)
     */
    private Long fullyAccepted;

    /**
     * 部分采纳次数(accepted=1)
     */
    private Long partiallyAccepted;

    /**
     * 未采纳次数(accepted=0)
     */
    private Long rejected;

    /**
     * 综合采纳率 0-1
     */
    private Double acceptanceRate;
}
