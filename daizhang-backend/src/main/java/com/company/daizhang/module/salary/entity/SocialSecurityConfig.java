package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 社保公积金配置实体
 */
@Data
@TableName("sal_social_security_config")
public class SocialSecurityConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年度
     */
    @TableField("`year`")
    private Integer year;

    /**
     * 城市
     */
    private String city;

    /**
     * 养老单位比例
     */
    private BigDecimal pensionEmployer;

    /**
     * 养老个人比例
     */
    private BigDecimal pensionEmployee;

    /**
     * 医疗单位比例
     */
    private BigDecimal medicalEmployer;

    /**
     * 医疗个人比例
     */
    private BigDecimal medicalEmployee;

    /**
     * 失业单位比例
     */
    private BigDecimal unemploymentEmployer;

    /**
     * 失业个人比例
     */
    private BigDecimal unemploymentEmployee;

    /**
     * 工伤单位比例
     */
    private BigDecimal injuryEmployer;

    /**
     * 生育单位比例
     */
    private BigDecimal maternityEmployer;

    /**
     * 公积金单位比例
     */
    private BigDecimal housingFundEmployer;

    /**
     * 公积金个人比例
     */
    private BigDecimal housingFundEmployee;

    /**
     * 缴费基数下限
     */
    private BigDecimal baseLower;

    /**
     * 缴费基数上限
     */
    private BigDecimal baseUpper;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
