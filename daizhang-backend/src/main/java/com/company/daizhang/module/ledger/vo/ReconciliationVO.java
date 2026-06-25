package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 往来对账VO
 */
@Data
public class ReconciliationVO {

    private String subjectCode;

    private String subjectName;

    private String auxiliaryItemName;

    /**
     * 账面余额
     */
    private BigDecimal bookBalance;

    /**
     * 对方余额
     */
    private BigDecimal counterpartBalance;

    /**
     * 差异
     */
    private BigDecimal difference;

    /**
     * 已平/未平
     */
    private String status;

    private List<ReconciliationDetailVO> details;
}
