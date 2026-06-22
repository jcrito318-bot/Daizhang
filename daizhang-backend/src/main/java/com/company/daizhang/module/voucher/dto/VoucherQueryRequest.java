package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 凭证查询请求
 */
@Data
public class VoucherQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Integer year;

    private Integer month;

    private Integer status;

    private String voucherNo;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
