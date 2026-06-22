package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 税务申报视图对象
 */
@Data
public class TaxDeclarationVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private String taxType;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal declaredAmount;

    private BigDecimal actualAmount;

    private Integer status;

    private LocalDate declarationDate;

    private LocalDate paymentDate;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String createByName;
}
