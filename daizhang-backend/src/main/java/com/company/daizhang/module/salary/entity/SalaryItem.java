package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_salary_item")
public class SalaryItem extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 项目编码
     */
    private String itemCode;

    /**
     * 项目类型：FIXED-固定 FLOAT-浮动 DEDUCT-扣款
     */
    private String itemType;

    /**
     * 计算方法
     */
    private String calculationMethod;

    /**
     * 备注
     */
    private String remark;
}
