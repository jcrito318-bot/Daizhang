package com.company.daizhang.module.bank.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 银行流水导入请求
 */
@Data
public class BankTransactionImportRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "银行账号不能为空")
    private String bankAccount;

    @NotEmpty(message = "银行流水列表不能为空")
    @Valid
    private List<BankTransactionItem> transactions;

    @Data
    public static class BankTransactionItem {

        @NotNull(message = "交易日期不能为空")
        private LocalDate transactionDate;

        @NotNull(message = "交易类型不能为空")
        private Integer transactionType;

        @NotNull(message = "交易金额不能为空")
        private BigDecimal amount;

        private BigDecimal balance;

        private String counterparty;

        private String summary;

        private String transactionNo;

        private String remark;
    }
}
