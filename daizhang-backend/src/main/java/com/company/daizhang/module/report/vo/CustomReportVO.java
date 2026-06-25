package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义报表视图对象
 */
@Data
public class CustomReportVO {

    private Long id;

    private String reportName;

    private String reportCode;

    private String reportType;

    private String description;

    private Integer status;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<CustomReportItemVO> items;

    /**
     * 自定义报表项目视图对象
     */
    @Data
    public static class CustomReportItemVO {

        private Long id;

        private Long reportId;

        private Integer rowNo;

        private String itemName;

        private String formula;

        private Integer displayDirection;

        private Integer isTotal;

        private Integer parentRowNo;
    }
}
