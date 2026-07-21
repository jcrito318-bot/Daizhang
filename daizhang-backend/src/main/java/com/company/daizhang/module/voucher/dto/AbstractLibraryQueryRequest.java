package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 常用摘要分页查询请求
 */
@Data
public class AbstractLibraryQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    /**
     * 摘要文本(模糊匹配)
     */
    private String abstractText;

    /**
     * 分类
     */
    private String abstractCategory;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
