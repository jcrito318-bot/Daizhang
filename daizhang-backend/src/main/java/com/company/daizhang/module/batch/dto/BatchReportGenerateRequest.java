package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量生成报表请求(跨多个账套)
 * <p>
 * 一次性为多个账套生成(计算并校验)指定期间的财务报表。
 * 报表为按需计算,本接口用于批量校验各账套报表可正常生成。
 */
@Data
public class BatchReportGenerateRequest {

    /**
     * 批量生成报表项列表
     */
    @NotEmpty(message = "生成报表项列表不能为空")
    @Valid
    private List<ReportGenerateItem> items;

    /**
     * 单个账套的报表生成项
     */
    @Data
    public static class ReportGenerateItem {

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
