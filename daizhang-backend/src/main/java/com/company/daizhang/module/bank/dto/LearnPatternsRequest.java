package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 历史模式学习触发请求
 * <p>
 * 手动触发对某账套的历史对账数据进行模式学习,从 bank_transaction 中已匹配
 * (matched_status=1 且 voucher_id 非空)的记录聚合出 (counterparty, 金额范围, 科目)
 * 模式,upsert 到 bank_match_history 表。
 */
@Data
public class LearnPatternsRequest {

    /**
     * 账套ID(IDOR 治理必填)
     */
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
}
