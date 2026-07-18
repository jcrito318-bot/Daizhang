package com.company.daizhang.module.ledger.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}
