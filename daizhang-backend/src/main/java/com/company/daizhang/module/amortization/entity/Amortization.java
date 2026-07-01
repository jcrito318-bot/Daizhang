package com.company.daizhang.module.amortization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 长期待摊费用实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_amortization")
public class Amortization extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 费用名称
     */
    private String amortizationName;

    /**
     * 科目ID
     */
    private Long subjectId;

    /**
     * 待摊总额
     */
    private BigDecimal totalAmount;

    /**
     * 已摊销额
     */
    private BigDecimal amortizedAmount;

    /**
     * 剩余待摊
     */
    private BigDecimal remainingAmount;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 总月数
     */
    private Integer totalMonths;

    /**
     * 月摊销额
     */
    private BigDecimal monthlyAmount;

    /**
     * 状态 0-摊销中 1-已摊完
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 最近摊销期间(格式yyyy-MM),用于防止同一期间重复摊销
     */
    private String lastAmortizedPeriod;
}
