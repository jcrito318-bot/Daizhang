package com.company.daizhang.module.period.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 试算平衡项
 */
@Data
public class TrialBalanceVO {
    
    private String subjectCode;
    
    private String subjectName;
    
    private BigDecimal debitBalance;
    
    private BigDecimal creditBalance;
}
