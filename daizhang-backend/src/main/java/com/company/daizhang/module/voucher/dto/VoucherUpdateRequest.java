package com.company.daizhang.module.voucher.dto;

import jakarta.validation.Valid;
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

    private Integer year;

    private Integer month;

    private Integer attachmentCount;

    @NotEmpty(message = "凭证明细不能为空")
    @Valid
    private List<VoucherDetailRequest> details;
}
