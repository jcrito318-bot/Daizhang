package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 税务风险预警视图对象
 */
@Data
public class TaxRiskWarningVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
