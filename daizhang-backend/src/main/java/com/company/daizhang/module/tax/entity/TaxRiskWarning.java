package com.company.daizhang.module.tax.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 税务风险预警实体
 * 注意：该表无 version/create_by/update_by 字段，不继承 BaseEntity
 */
@Data
@TableName("tax_risk_warning")
public class TaxRiskWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountSetId;

    @TableField("`year`")
    private Integer year;

    @TableField("`month`")
    private Integer month;

    /**
     * 风险类型：税负率/发票/申报
     */
    private String riskType;

    /**
     * 风险等级 1-低 2-中 3-高
     */
    private Integer riskLevel;

    /**
     * 风险描述
     */
    private String riskDescription;

    /**
     * 风险值
     */
    private String riskValue;

    /**
     * 处理建议
     */
    private String suggestion;

    /**
     * 状态 0-未处理 1-已处理 2-已忽略
     */
    private Integer status;

    /**
     * 处理备注
     */
    private String handleRemark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
