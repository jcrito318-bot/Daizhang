package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 个税专项附加扣除视图对象
 */
@Data
public class SpecialDeductionVO {

    private Long id;

    private Long accountSetId;

    private Long employeeId;

    private String employeeName;

    private String deductionType;

    private String deductionTypeDesc;

    private String deductionName;

    private BigDecimal monthlyAmount;

    private BigDecimal annualAmount;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Integer status;

    private String statusDesc;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
