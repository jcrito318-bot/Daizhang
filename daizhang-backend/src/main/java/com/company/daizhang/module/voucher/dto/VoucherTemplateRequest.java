package com.company.daizhang.module.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证模板请求
 */
@Data
public class VoucherTemplateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private String templateCategory;

    private String summary;

    private Long voucherWordId;

    private Integer attachmentCount;

    private String remark;

    @NotEmpty(message = "模板明细不能为空")
    @Valid
    private List<VoucherTemplateDetailRequest> details;

    /**
     * 凭证模板明细请求
     */
    @Data
    public static class VoucherTemplateDetailRequest {

        private Integer lineNo;

        private String summary;

        @NotNull(message = "科目ID不能为空")
        private Long subjectId;

        private String subjectCode;

        private String subjectName;

        private BigDecimal debit;

        private BigDecimal credit;
    }
}
