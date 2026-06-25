package com.company.daizhang.module.accountset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 科目期初余额实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_subject_balance")
public class SubjectBalance extends BaseEntity {

    private Long accountSetId;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    @TableField("`year`")
    private Integer year;

    private Integer period;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private Long auxiliaryId;
}
