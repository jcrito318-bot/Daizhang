package com.company.daizhang.module.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量审核凭证请求(跨多个账套)
 * <p>
 * 代账会计一人代多家账,通过本接口一次性审核多个账套指定期间的凭证。
 * 每个 item 可指定具体的 voucherIds,也可省略 voucherIds 审核该期间所有未审核凭证。
 */
@Data
public class BatchVoucherAuditRequest {

    /**
     * 批量审核项列表
     */
    @NotEmpty(message = "审核项列表不能为空")
    @Valid
    private List<VoucherAuditItem> items;

    /**
     * 单个账套的审核项
     */
    @Data
    public static class VoucherAuditItem {

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
         * 凭证ID列表。
         * <p>
         * 为空(或 null)时审核该期间(accountSetId+year+month)下所有未审核凭证;
         * 非空时仅审核指定的凭证ID。
         */
        private List<Long> voucherIds;
    }
}
