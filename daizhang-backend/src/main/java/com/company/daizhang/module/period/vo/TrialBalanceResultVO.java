package com.company.daizhang.module.period.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试算平衡结果
 */
@Data
public class TrialBalanceResultVO {
    
    private List<TrialBalanceVO> items;
    
    private BigDecimal totalDebit;
    
    private BigDecimal totalCredit;
    
    private boolean balanced;
}
