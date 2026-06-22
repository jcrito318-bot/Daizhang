package com.company.daizhang.module.ai.dto;

import lombok.Data;

/**
 * 智能记账请求DTO
 */
@Data
public class AccountingSuggestRequest {
    /**
     * 业务描述
     */
    private String description;

    /**
     * 金额
     */
    private Double amount;

    /**
     * 账套ID
     */
    private Long accountSetId;
}
