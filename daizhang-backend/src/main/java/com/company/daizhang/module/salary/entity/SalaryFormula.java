package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 薪资公式实体
 */
@Data
@TableName("sal_salary_formula")
public class SalaryFormula implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 公式名称
     */
    private String formulaName;

    /**
     * 目标薪资项
     */
    private String targetItem;

    /**
     * 公式表达式
     */
    private String formulaExpression;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 状态 1-启用 0-停用
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
