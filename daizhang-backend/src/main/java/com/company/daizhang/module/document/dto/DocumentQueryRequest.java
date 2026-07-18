package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 票据查询请求
 */
@Data
public class DocumentQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Integer documentType;

    private Integer status;

    private String documentNo;

    private String sellerName;

    private String buyerName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long voucherId;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
