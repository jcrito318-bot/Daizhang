package com.company.daizhang.module.bank.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能对账匹配结果视图对象
 * <p>
 * 一次智能匹配的完整结果,包含已匹配建议列表(含完整流水/凭证详情与匹配原因,
 * 便于前端一次性渲染建议面板)、未匹配流水列表、未匹配凭证列表及统计计数。
 */
@Data
@Schema(description = "智能对账匹配结果")
public class MatchResultVO {

    /**
     * 已匹配建议列表(按分数倒序)
     * <p>
     * 每项为完整 {@link MatchSuggestionVO},包含 transactionId/voucherId/score/matchType
     * 四个核心字段,以及流水/凭证详情与匹配原因,供前端直接展示。
     */
    private List<MatchSuggestionVO> matched;

    /**
     * 未匹配流水ID列表
     */
    private List<Long> unmatchedTransactions;

    /**
     * 未匹配凭证ID列表
     */
    private List<Long> unmatchedVouchers;

    /**
     * 已匹配建议总数
     */
    private Integer totalMatched;

    /**
     * 未匹配总数(流水未匹配数 + 凭证未匹配数)
     */
    private Integer totalUnmatched;
}
