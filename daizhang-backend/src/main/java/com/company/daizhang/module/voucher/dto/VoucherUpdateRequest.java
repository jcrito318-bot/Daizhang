package com.company.daizhang.module.voucher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 凭证更新请求
 */
@Data
public class VoucherUpdateRequest {

    private Long voucherWordId;

    private LocalDate voucherDate;

    @Min(value = 1900, message = "年度不合法")
    @Max(value = 2099, message = "年度不合法")
    private Integer year;

    @Min(value = 1, message = "月份必须在1-12之间")
    @Max(value = 12, message = "月份必须在1-12之间")
    private Integer month;

    @Min(value = 0, message = "附件数不能为负数")
    private Integer attachmentCount;

    @NotEmpty(message = "凭证明细不能为空")
    @Valid
    private List<VoucherDetailRequest> details;
}
