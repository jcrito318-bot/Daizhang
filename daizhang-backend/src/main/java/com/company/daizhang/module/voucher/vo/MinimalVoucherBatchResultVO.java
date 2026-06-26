package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.util.List;

/**
 * 极简记账批量结果VO
 */
@Data
public class MinimalVoucherBatchResultVO {

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 处理的账套数
     */
    private Integer totalCount;

    /**
     * 成功生成凭证数
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 生成的凭证总数
     */
    private Integer voucherCount;

    /**
     * 失败详情
     */
    private List<MinimalVoucherFailItem> failItems;

    @Data
    public static class MinimalVoucherFailItem {
        private Long accountSetId;
        private String accountSetName;
        private String failReason;
    }
}
