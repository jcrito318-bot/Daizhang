package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 常用摘要请求(新增/更新)
 */
@Data
public class AbstractLibraryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "摘要文本不能为空")
    @Size(max = 200, message = "摘要文本长度不能超过200")
    private String abstractText;

    /**
     * 分类: 工资/折旧/社保/税金/报销/采购/销售/其他
     */
    @Size(max = 50, message = "分类长度不能超过50")
    private String abstractCategory;
}
