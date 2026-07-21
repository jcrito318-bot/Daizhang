package com.company.daizhang.module.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证模板请求(创建/更新)
 */
@Data
public class VoucherTemplateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过50")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;

    /**
     * 模板分类: 工资/折旧/社保/税金/结转/其他
     */
    @Size(max = 50, message = "模板分类长度不能超过50")
    private String templateCategory;

    /**
     * 凭证摘要
     */
    @Size(max = 200, message = "凭证摘要长度不能超过200")
    private String summary;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /**
     * 分录明细列表(JSON 序列化后存入 detail_json 字段)
     */
    @NotEmpty(message = "模板明细不能为空")
    @Valid
    private List<VoucherTemplateDetailRequest> details;

    /**
     * 凭证模板明细请求
     */
    @Data
    public static class VoucherTemplateDetailRequest {

        /**
         * 科目编码
         */
        @NotBlank(message = "科目编码不能为空")
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
