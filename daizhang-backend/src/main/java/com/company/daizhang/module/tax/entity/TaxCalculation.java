package com.company.daizhang.module.tax.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 税务计算实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tax_calculation")
public class TaxCalculation extends BaseEntity {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    /**
     * 税种：增值税/企业所得税/个人所得税等
     */
    private String taxType;

    /**
     * 计算项目
     */
    private String calculationItem;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 税率
     */
    private BigDecimal rate;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 备注
     */
    private String remark;
}
