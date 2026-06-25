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

    private String templateName;

    private String templateCategory;

    private String summary;

    private Long voucherWordId;

    private Integer attachmentCount;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private List<VoucherTemplateDetailVO> details;

    /**
     * 凭证模板明细视图对象
     */
    @Data
    public static class VoucherTemplateDetailVO {

        private Long id;

        private Long templateId;

        private Integer lineNo;

        private String summary;

        private Long subjectId;

        private String subjectCode;

        private String subjectName;

        private BigDecimal debit;

        private BigDecimal credit;

        private Integer sortOrder;
    }
}
