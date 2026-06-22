package com.company.daizhang.module.document.dto;

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

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
