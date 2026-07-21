package com.company.daizhang.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行匹配历史模式实体
 * <p>
 * 记录某账套下同一交易对方历史匹配的金额范围与对应凭证科目,
 * 供智能匹配引擎在遇到同对方、相似金额的流水时给予加分,加速月度对账。
 * <p>
 * 唯一键: (account_set_id, counterparty) — 同账套下同一对方仅保留一条最新聚合记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_match_history")
public class BankMatchHistory extends BaseEntity {

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
}
