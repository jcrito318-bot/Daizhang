package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自定义报表数据视图对象(执行报表取数后返回)
 */
@Data
public class CustomReportDataVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表编码
     */
    private String reportCode;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 报表数据行
     */
    private List<CustomReportDataRow> rows;

    /**
     * 报表数据行
     */
    @Data
    public static class CustomReportDataRow {

        /**
         * 行号
         */
        private Integer rowNo;

        /**
         * 项目名称
         */
        private String itemName;

        /**
         * 取数公式
         */
        private String formula;

        /**
         * 显示方向 0-借方 1-贷方
         */
        private Integer displayDirection;

        /**
         * 是否合计行(0-否 1-是)
         */
        private Integer isTotal;

        /**
         * 父行号
         */
        private Integer parentRowNo;

        /**
         * 金额
         */
        private BigDecimal amount;
    }
}
