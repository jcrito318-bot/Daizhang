package com.company.daizhang.module.accountset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 会计期间实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_account_period")
public class AccountPeriod extends BaseEntity {
    
    private Long accountSetId;
    
    private Integer year;
    
    private Integer month;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer status;
    
    private Long closeBy;
    
    private java.time.LocalDateTime closeTime;
}
