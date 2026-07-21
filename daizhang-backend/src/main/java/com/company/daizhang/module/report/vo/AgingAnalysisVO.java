package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 账龄分析结果VO(应收/应付合并返回)
 * <p>
 * 用于一次性返回应收账龄、应付账龄及汇总信息,代账出经营建议时常用。
 * 单独端点(receivable/payable/summary)仅返回其中的子集。
 */
@Data
public class AgingAnalysisVO {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 截止日期(账龄基准日)
     */
    private LocalDate asOfDate;

    /**
     * 客户应收账龄列表(仅含有未核销余额的客户)
     */
    private List<AgingItemVO> customerAging;

    /**
     * 供应商应付账龄列表(仅含有未核销余额的供应商)
     */
    private List<AgingItemVO> supplierAging;

    /**
     * 汇总信息
     */
    private AgingSummaryVO summary;
}
