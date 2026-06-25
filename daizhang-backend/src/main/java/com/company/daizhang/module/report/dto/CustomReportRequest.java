package com.company.daizhang.module.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 自定义报表请求
 */
@Data
public class CustomReportRequest {

    @NotBlank(message = "报表名称不能为空")
    private String reportName;

    @NotBlank(message = "报表编码不能为空")
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

    @NotEmpty(message = "报表项目不能为空")
    @Valid
    private List<CustomReportItemRequest> items;

    /**
     * 自定义报表项目请求
     */
    @Data
    public static class CustomReportItemRequest {

        /**
         * 行号
         */
        private Integer rowNo;

        @NotBlank(message = "项目名称不能为空")
        private String itemName;

        /**
         * 取数公式(科目编码组合，如"1001+1002")
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
    }
}
