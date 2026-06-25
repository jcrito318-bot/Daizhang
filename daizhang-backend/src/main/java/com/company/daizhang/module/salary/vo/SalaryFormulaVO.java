package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪资公式视图对象
 */
@Data
public class SalaryFormulaVO {

    private Long id;

    private Long accountSetId;

    private String formulaName;

    private String targetItem;

    private String formulaExpression;

    private Integer priority;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
