package com.company.daizhang.module.accountset.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目期初余额录入请求
 */
@Data
public class SubjectBalanceRequest {

    private Long accountSetId;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Integer year;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private Long auxiliaryId;
}
