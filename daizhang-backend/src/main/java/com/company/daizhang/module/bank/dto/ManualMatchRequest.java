package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 手动匹配请求
 */
@Data
public class ManualMatchRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotEmpty(message = "银行流水ID列表不能为空")
    private List<Long> transactionIds;

    @NotNull(message = "凭证ID不能为空")
    private Long voucherId;
}
