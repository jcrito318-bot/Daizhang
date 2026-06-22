package com.company.daizhang.module.accountset.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间视图对象
 */
@Data
public class AccountPeriodVO {
    
    private Long id;
    
    private Long accountSetId;
    
    private Integer year;
    
    private Integer month;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer status;
    
    private Long closeBy;
    
    private LocalDateTime closeTime;
}
