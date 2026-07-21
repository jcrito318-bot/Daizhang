package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表钻取结果VO
 * <p>
 * 用于代账会计在报表金额上双击后,根据"科目+期间+金额"反查符合条件的相关凭证分录。
 * 返回的 vouchers 列表对应同一科目范围内、同一期间、借贷金额与目标金额匹配的凭证。
 */
@Data
public class DrillDownResultVO {

    /**
     * 命中的凭证分录列表(按凭证日期、凭证号排序)
     */
    private List<DrillDownVoucher> vouchers;

    /**
     * 钻取使用的科目编码(便于前端回显)
     */
    private String subjectCode;

    /**
     * 钻取使用的金额
     */
    private BigDecimal amount;

    /**
     * 钻取方向:debit/credit
     */
    private String direction;

    /**
     * 是否模糊匹配
     */
    private Boolean fuzzy;

    /**
     * 钻取命中的凭证分录
     */
    @Data
    public static class DrillDownVoucher {

        /**
         * 凭证ID(用于跳转凭证详情)
         */
        private Long voucherId;

        /**
         * 凭证号
         */
        private String voucherNo;

        /**
         * 凭证日期
         */
        private LocalDate voucherDate;

        /**
         * 凭证整体摘要(取首条明细摘要作为代表)
         */
        private String summary;

        /**
         * 命中分录的借方金额
         */
        private BigDecimal debitAmount;

        /**
         * 命中分录的贷方金额
         */
        private BigDecimal creditAmount;

        /**
         * 该凭证下命中条件的分录摘要列表(便于用户在弹窗中快速浏览)
         */
        private List<String> abstracts;
    }
}
