package com.company.daizhang.module.bank.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历史匹配模式视图对象
 * <p>
 * 描述某账套下同一交易对方在历史对账中已被匹配的模式(金额范围、对应科目等),
 * 用于智能匹配时给同对方、相似金额的流水加分,加速月度对账。
 */
@Data
@Schema(description = "历史匹配模式")
public class MatchHistoryPatternVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 交易对方
     */
    private String counterparty;

    /**
     * 金额范围最小值
     */
    private BigDecimal amountRangeMin;

    /**
     * 金额范围最大值
     */
    private BigDecimal amountRangeMax;

    /**
     * 对应凭证科目编码(银行存款明细科目)
     */
    private String voucherSubjectCode;

    /**
     * 历史匹配次数
     */
    private Integer matchCount;

    /**
     * 最近匹配时间
     */
    private LocalDateTime lastMatchedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
