package com.company.daizhang.module.bank.dto;

import lombok.Data;

/**
 * 银行账户查询请求
 */
@Data
public class BankAccountQueryRequest {

    private Long accountSetId;

    private String accountName;

    private String accountNumber;

    private String bankName;

    private String accountType;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
