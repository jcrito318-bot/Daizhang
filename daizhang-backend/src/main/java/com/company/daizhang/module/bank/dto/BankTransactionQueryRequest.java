package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 银行流水查询请求
 */
@Data
public class BankTransactionQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private String bankAccount;

    private Integer transactionType;

    private Integer matchedStatus;

    private LocalDate startDate;

    private LocalDate endDate;

    private String counterparty;

    private String summary;

    private String transactionNo;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
