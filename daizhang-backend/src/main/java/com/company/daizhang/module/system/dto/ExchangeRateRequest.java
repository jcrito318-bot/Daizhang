package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率请求
 */
@Data
public class ExchangeRateRequest {

    @NotBlank(message = "币种代码不能为空")
    private String currencyCode;

    @NotBlank(message = "币种名称不能为空")
    private String currencyName;

    @NotNull(message = "汇率不能为空")
    private BigDecimal rate;

    @NotNull(message = "汇率日期不能为空")
    private LocalDate rateDate;

    private String rateType;
}
