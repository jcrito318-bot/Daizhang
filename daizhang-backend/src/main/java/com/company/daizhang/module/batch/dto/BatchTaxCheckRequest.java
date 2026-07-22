package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 跨账套批量漏报检查请求
 * <p>
 * 代账公司需检查所有账套是否有漏报(某月该报未报)。每个账套独立检查,
 * 单个账套检查异常不影响其他账套。
 */
@Data
public class BatchTaxCheckRequest {

    /**
     * 批量税务检查项列表
     */
    @NotEmpty(message = "税务检查项列表不能为空")
    @Valid
    private List<TaxCheckItem> items;

    /**
     * 单个账套的税务检查项
     */
    @Data
    public static class TaxCheckItem {

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
    }
}
