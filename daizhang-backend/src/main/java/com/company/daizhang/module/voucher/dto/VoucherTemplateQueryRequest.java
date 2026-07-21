package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 凭证模板分页查询请求
 */
@Data
public class VoucherTemplateQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    /**
     * 模板名称(模糊匹配)
     */
    private String templateName;

    /**
     * 模板分类
     */
    private String templateCategory;

    /**
     * 模板编码(模糊匹配)
     */
    private String templateCode;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
