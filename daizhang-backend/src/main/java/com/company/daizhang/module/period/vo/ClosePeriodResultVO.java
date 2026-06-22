package com.company.daizhang.module.period.vo;

import lombok.Data;

/**
 * 结账结果
 */
@Data
public class ClosePeriodResultVO {
    
    private boolean success;
    
    private String message;
    
    private Integer uncheckedVouchers;
}
