package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 零申报批量自动记账+结账请求(跨多个账套)
 * <p>
 * 面向代账公司的零申报客户(当月无业务发生),一次性完成:结转损益(无数据则跳过)→ 结账。
 * 每个账套独立处理,单个失败不影响其他账套。
 */
@Data
public class BatchZeroDeclarationRequest {

    /**
     * 零申报批量处理项列表
     */
    @NotEmpty(message = "零申报处理项列表不能为空")
    @Valid
    private List<ZeroDeclarationItem> items;

    /**
     * 单个账套的零申报处理项
     */
    @Data
    public static class ZeroDeclarationItem {

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
