package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量导出报表请求(跨多个账套, zip 打包)
 * <p>
 * 一次性为多个账套导出指定期间的财务报表 Excel,所有 Excel 文件打包为 zip 流式下载。
 * 单个账套或单个报表类型失败不影响其他文件,失败项跳过并在日志中告警。
 */
@Data
public class BatchReportExportRequest {

    /**
     * 批量导出报表项列表
     */
    @NotEmpty(message = "导出报表项列表不能为空")
    @Valid
    private List<ReportExportItem> items;

    /**
     * 单个账套的报表导出项
     */
    @Data
    public static class ReportExportItem {

        /**
         * 账套ID
         */
        @NotNull(message = "账套ID不能为空")
        private Long accountSetId;

        /**
         * 年度
         */
        @NotNull(message = "年度不能为空")
        private Integer year;

        /**
         * 月份
         */
        @NotNull(message = "月份不能为空")
        private Integer month;

        /**
         * 报表类型列表,可选值:
         * <ul>
         *   <li>balance-sheet 资产负债表</li>
         *   <li>income-statement 利润表</li>
         *   <li>cash-flow-statement 现金流量表</li>
         *   <li>subject-balance 科目余额表</li>
         * </ul>
         */
        @NotEmpty(message = "报表类型列表不能为空")
        private List<String> reportTypes;
    }
}
