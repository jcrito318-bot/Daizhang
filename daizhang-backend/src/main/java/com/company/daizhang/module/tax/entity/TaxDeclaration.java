package com.company.daizhang.module.tax.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税务申报实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tax_declaration")
public class TaxDeclaration extends BaseEntity {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    /**
     * 税种：增值税/企业所得税/个人所得税等
     */
    private String taxType;

    /**
     * 应纳税所得额
     */
    private BigDecimal taxableAmount;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 应纳税额
     */
    private BigDecimal taxAmount;

    /**
     * 已申报金额
     */
    private BigDecimal declaredAmount;

    /**
     * 实际缴纳金额
     */
    private BigDecimal actualAmount;

    /**
     * 状态 0-未申报 1-已申报 2-已缴纳
     */
    private Integer status;

    /**
     * 申报日期
     */
    private LocalDate declarationDate;

    /**
     * 缴纳日期
     */
    private LocalDate paymentDate;

    /**
     * 备注
     */
    private String remark;
}
