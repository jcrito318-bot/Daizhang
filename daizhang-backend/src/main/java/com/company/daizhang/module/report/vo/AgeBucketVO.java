package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账龄分桶VO
 * <p>
 * 按凭证日期与截止日期(asOfDate)的差值(天)分桶:
 * <ul>
 *   <li>0-30 天: 正常</li>
 *   <li>31-60 天: 关注</li>
 *   <li>61-90 天: 预警</li>
 *   <li>91-180 天: 逾期</li>
 *   <li>180+ 天: 坏账风险</li>
 * </ul>
 */
@Data
public class AgeBucketVO {

    /**
     * 0-30 天(正常)
     */
    private BigDecimal within30Days;

    /**
     * 31-60 天(关注)
     */
    private BigDecimal days31To60;

    /**
     * 61-90 天(预警)
     */
    private BigDecimal days61To90;

    /**
     * 91-180 天(逾期)
     */
    private BigDecimal days91To180;

    /**
     * 180 天以上(坏账风险)
     */
    private BigDecimal over180Days;
}
