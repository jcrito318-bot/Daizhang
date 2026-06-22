package com.company.daizhang.module.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 账簿查询请求
 */
@Data
public class LedgerQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Long subjectId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    private Integer month;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long auxiliaryId;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
