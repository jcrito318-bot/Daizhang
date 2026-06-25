package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行账户创建/更新请求
 */
@Data
public class BankAccountRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "账户名称不能为空")
    private String accountName;

    @NotBlank(message = "银行账号不能为空")
    private String accountNumber;

    private String bankName;

    private String branchName;

    private String accountType;

    private String currency;

    private Long subjectId;

    private BigDecimal beginningBalance;

    private Integer status;

    private LocalDate openDate;

    private String remark;
}
