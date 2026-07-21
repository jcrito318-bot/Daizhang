package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量结账请求(跨多个账套)
 * <p>
 * 一次性对多个账套执行期末结账。每个账套独立处理,单个失败不影响其他账套。
 */
@Data
public class BatchPeriodCloseRequest {

    /**
     * 批量结账项列表
     */
    @NotEmpty(message = "结账项列表不能为空")
    @Valid
    private List<PeriodCloseItem> items;

    /**
     * 单个账套的结账项
     */
    @Data
    public static class PeriodCloseItem {

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
