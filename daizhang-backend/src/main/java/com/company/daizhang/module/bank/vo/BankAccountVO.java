package com.company.daizhang.module.bank.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行账户视图对象
 */
@Data
public class BankAccountVO {

    private Long id;

    private Long accountSetId;

    private String accountName;

    private String accountNumber;

    private String bankName;

    private String branchName;

    private String accountType;

    private String accountTypeDesc;

    private String currency;

    private Long subjectId;

    private String subjectName;

    private BigDecimal beginningBalance;

    private Integer status;

    private String statusDesc;

    private LocalDate openDate;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
