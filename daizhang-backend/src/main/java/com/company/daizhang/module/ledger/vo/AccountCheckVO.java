package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账账核对VO
 */
@Data
public class AccountCheckVO {

    /**
     * 账账核对类型
     */
    private String checkType;

    private String checkName;

    /**
     * 是否平衡
     */
    private Boolean balanced;

    /**
     * 左方金额
     */
    private BigDecimal leftAmount;

    /**
     * 右方金额
     */
    private BigDecimal rightAmount;

    private BigDecimal difference;

    private String description;
}
