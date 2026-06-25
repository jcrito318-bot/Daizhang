package com.company.daizhang.module.amortization.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 长期待摊费用视图对象
 */
@Data
public class AmortizationVO {

    private Long id;

    private Long accountSetId;

    private String amortizationName;

    private Long subjectId;

    private BigDecimal totalAmount;

    private BigDecimal amortizedAmount;

    private BigDecimal remainingAmount;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer totalMonths;

    private BigDecimal monthlyAmount;

    private Integer status;

    private String statusName;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
