package com.company.daizhang.module.bank.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 智能对账匹配建议视图对象
 * <p>
 * 描述一条银行流水与一条凭证之间的匹配建议,包含匹配分数、匹配原因、匹配类型等信息,
 * 供前端展示给用户决定是否接受。
 */
@Data
@Schema(description = "智能对账匹配建议")
public class MatchSuggestionVO {

    /**
     * 银行流水ID
     */
    private Long transactionId;

    /**
     * 凭证ID
     */
    private Long voucherId;

    /**
     * 匹配分数(0-100)
     * <p>
     * 评分维度(各维度互斥,按最高项计分):
     * <ul>
     *   <li>金额精确匹配: 50 分(BigDecimal.compareTo == 0)</li>
     *   <li>金额模糊匹配: 40 分(±0.01 容差)</li>
     *   <li>日期同日: 20 分;日期接近(±3 天): 15 分</li>
     *   <li>摘要相似度 ≥0.8: 30 分;≥0.5: 20 分</li>
     *   <li>对方单位匹配: 15 分</li>
     *   <li>历史模式匹配: 25 分</li>
     * </ul>
     * 总分上限 100。
     */
    private Integer score;

    /**
     * 匹配原因列表(每项形如 "金额精确匹配 +50",用于前端标签展示)
     */
    private List<String> reasons;

    /**
     * 匹配类型 exact/fuzzy/suggested
     * <ul>
     *   <li>exact: 总分 ≥80,强烈建议匹配</li>
     *   <li>fuzzy: 总分 60-79,建议匹配</li>
     *   <li>suggested: 总分 &lt;60,不推荐但可参考</li>
     * </ul>
     */
    private String matchType;

    /**
     * 匹配类型名称(强烈建议/建议/不推荐)
     */
    private String matchTypeName;

    // ===== 流水信息(便于前端直接展示,免去再次查询) =====

    /**
     * 流水交易日期
     */
    private LocalDate transactionDate;

    /**
     * 流水金额
     */
    private BigDecimal transactionAmount;

    /**
     * 流水交易类型 1-收入 2-支出
     */
    private Integer transactionType;

    /**
     * 流水交易对方
     */
    private String counterparty;

    /**
     * 流水摘要
     */
    private String transactionSummary;

    // ===== 凭证信息 =====

    /**
     * 凭证号
     */
    private String voucherNo;

    /**
     * 凭证日期
     */
    private LocalDate voucherDate;

    /**
     * 凭证摘要(取首条银行存款明细摘要)
     */
    private String voucherSummary;

    /**
     * 凭证金额(取银行存款科目对应借贷方金额)
     */
    private BigDecimal voucherAmount;
}
