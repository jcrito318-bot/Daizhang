package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 凭证模板视图对象
 */
@Data
public class VoucherTemplateVO {

    private Long id;

    private Long accountSetId;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板分类: 工资/折旧/社保/税金/结转/其他
     */
    private String templateCategory;

    /**
     * 凭证摘要
     */
    private String summary;

    /**
     * 备注
     */
    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    /**
     * 分录明细列表(由 detail_json 字段反序列化得到)
     */
    private List<VoucherTemplateDetailVO> details;

    /**
     * 凭证模板明细视图对象
     */
    @Data
    public static class VoucherTemplateDetailVO {

        /**
         * 科目编码
         */
        private String subjectCode;

        /**
         * 科目名称
         */
        private String subjectName;

        /**
         * 借方金额
         */
        private BigDecimal debitAmount;

        /**
         * 贷方金额
         */
        private BigDecimal creditAmount;

        /**
         * 摘要
         */
        private String summary;
    }
}
