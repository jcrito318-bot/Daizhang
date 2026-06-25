package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 图表系列数据视图对象
 */
@Data
public class ChartSeriesVO {

    /**
     * x轴标签 如"2026-01"
     */
    private String name;

    /**
     * 数值
     */
    private BigDecimal value;
}
