package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 饼图数据视图对象
 */
@Data
public class ChartPieVO {

    /**
     * 名称
     */
    private String name;

    /**
     * 数值
     */
    private BigDecimal value;
}
