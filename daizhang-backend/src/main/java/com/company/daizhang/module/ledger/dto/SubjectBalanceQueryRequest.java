package com.company.daizhang.module.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 科目余额查询请求
 */
@Data
public class SubjectBalanceQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    private Integer startMonth;

    private Integer endMonth;

    private Integer level;
}
