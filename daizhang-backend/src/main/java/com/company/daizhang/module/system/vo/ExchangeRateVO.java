package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 汇率视图对象
 */
@Data
public class ExchangeRateVO {

    private Long id;

    private String currencyCode;

    private String currencyName;

    private BigDecimal rate;

    private LocalDate rateDate;

    private String rateType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
