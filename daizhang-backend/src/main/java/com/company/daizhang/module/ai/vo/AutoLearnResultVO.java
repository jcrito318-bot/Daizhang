package com.company.daizhang.module.ai.vo;

import lombok.Data;

/**
 * 自动学习结果 VO
 * <p>
 * 由 autoLearnFromFeedback 返回,反映本次自动学习生成/跳过的规则数,
 * 便于 ADMIN 评估学习效果并排查为何某些高频反馈未被采纳为规则。
 */
@Data
public class AutoLearnResultVO {

    /**
     * 扫描的反馈记录数
     */
    private Long scannedFeedbackCount;

    /**
     * 候选规则数(满足 ≥3 次且一致率 ≥80% 的描述分组数)
     */
    private Integer candidateCount;

    /**
     * 本次新生成的规则数
     */
    private Integer generatedCount;

    /**
     * 因 keyword+accountSetId 已存在而跳过的规则数
     */
    private Integer skippedDuplicateCount;
}
