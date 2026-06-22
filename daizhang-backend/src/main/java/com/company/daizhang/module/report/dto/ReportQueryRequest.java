package com.company.daizhang.module.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报表查询请求
 */
@Data
public class ReportQueryRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;
}
