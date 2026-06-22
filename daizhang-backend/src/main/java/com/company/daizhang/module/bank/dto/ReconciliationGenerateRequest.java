package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 生成对账单请求
 */
@Data
public class ReconciliationGenerateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "银行账号不能为空")
    private String bankAccount;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    private String remark;
}
