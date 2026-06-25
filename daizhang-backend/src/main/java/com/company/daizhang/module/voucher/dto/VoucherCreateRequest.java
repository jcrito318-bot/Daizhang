package com.company.daizhang.module.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 凭证创建请求
 */
@Data
public class VoucherCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Long voucherWordId;

    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    private Integer attachmentCount;

    /**
     * 草稿状态 0-正常 1-草稿
     */
    private Integer draftStatus;

    @NotEmpty(message = "凭证明细不能为空")
    @Valid
    private List<VoucherDetailRequest> details;
}
