package com.company.daizhang.module.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 发票查询请求
 */
@Data
public class InvoiceQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private String invoiceNumber;

    private LocalDate startDate;

    private LocalDate endDate;

    private String invoiceType;

    /**
     * 进项发票认证状态 0-未认证 1-已认证 2-已作废
     */
    private Integer authStatus;

    /**
     * 销项发票状态 0-正常 1-已作废 2-已红冲
     */
    private Integer invoiceStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
