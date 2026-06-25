package com.company.daizhang.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义报表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rpt_custom_report")
public class CustomReport extends BaseEntity {

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表编码
     */
    private String reportCode;

    /**
     * 报表类型
     */
    private String reportType;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;
}
