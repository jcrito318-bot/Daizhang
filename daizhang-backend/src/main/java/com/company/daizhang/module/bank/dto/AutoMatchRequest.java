package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自动匹配请求
 */
@Data
public class AutoMatchRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "银行账号不能为空")
    private String bankAccount;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;
}
