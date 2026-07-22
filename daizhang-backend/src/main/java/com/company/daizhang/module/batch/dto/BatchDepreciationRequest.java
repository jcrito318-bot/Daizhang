package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 跨账套批量计提固定资产折旧请求
 * <p>
 * 代账会计一人代多家账,通过本接口一次性为多个账套计提指定期间的固定资产折旧。
 * 每个账套独立事务,单个账套失败不影响其他账套。
 */
@Data
public class BatchDepreciationRequest {

    /**
     * 批量计提折旧项列表
     */
    @NotEmpty(message = "计提折旧项列表不能为空")
    @Valid
    private List<DepreciationItem> items;

    /**
     * 单个账套的计提折旧项
     */
    @Data
    public static class DepreciationItem {

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
