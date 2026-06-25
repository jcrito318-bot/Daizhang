package com.company.daizhang.module.accountset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 科目余额实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_account_balance")
public class AccountBalance extends BaseEntity {

    private Long accountSetId;

    private Long subjectId;

    @TableField("`year`")
    private Integer year;

    @TableField("`month`")
    private Integer month;
    
    private BigDecimal beginDebit;
    
    private BigDecimal beginCredit;
    
    private BigDecimal periodDebit;
    
    private BigDecimal periodCredit;
    
    private BigDecimal endDebit;
    
    private BigDecimal endCredit;
    
    private BigDecimal yearDebit;
    
    private BigDecimal yearCredit;
}
