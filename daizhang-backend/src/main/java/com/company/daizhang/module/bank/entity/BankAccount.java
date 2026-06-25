package com.company.daizhang.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行账户主数据实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_account")
public class BankAccount extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 账户名称（开户全称）
     */
    private String accountName;

    /**
     * 银行账号
     */
    private String accountNumber;

    /**
     * 开户行名称
     */
    private String bankName;

    /**
     * 开户行支行
     */
    private String branchName;

    /**
     * 账户类型：CHECKING-活期 DEPOSIT-定期 OTHER-其他
     */
    private String accountType;

    /**
     * 币种（默认CNY）
     */
    private String currency;

    /**
     * 关联科目ID（银行存款明细科目）
     */
    private Long subjectId;

    /**
     * 期初余额
     */
    private BigDecimal beginningBalance;

    /**
     * 账户状态 0-停用 1-正常
     */
    private Integer status;

    /**
     * 开户日期
     */
    private LocalDate openDate;

    /**
     * 备注
     */
    private String remark;
}
