package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 税务计算视图对象
 */
@Data
public class TaxCalculationVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private String taxType;

    private String calculationItem;

    private BigDecimal amount;

    private BigDecimal rate;

    private BigDecimal taxAmount;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String createByName;
}
