package com.company.daizhang.module.period.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 试算平衡请求
 */
@Data
public class TrialBalanceRequest {
    
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
    
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
}
