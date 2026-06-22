package com.company.daizhang.module.bank.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 银行对账结果视图对象
 */
@Data
public class BankReconciliationVO {

    private Long id;

    private Long accountSetId;

    private String bankAccount;

    private Integer year;

    private Integer month;

    private BigDecimal bankBalance;

    private BigDecimal bookBalance;

    private BigDecimal difference;

    private Integer unreconciledItems;

    private LocalDate reconciledDate;

    private Long reconciledBy;

    private String reconciledByName;

    private Integer status;

    private String statusName;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    /**
     * 未达账项列表（银行已收/付，企业未收/付）
     */
    private List<BankTransactionVO> unreconciledTransactions;
}
