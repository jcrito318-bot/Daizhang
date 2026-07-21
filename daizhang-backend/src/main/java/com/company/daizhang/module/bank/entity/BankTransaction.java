package com.company.daizhang.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import com.company.daizhang.common.crypto.annotation.EncryptedField;
import com.company.daizhang.common.crypto.mybatis.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行流水实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "bank_transaction", autoResultMap = true)
public class BankTransaction extends BaseEntity {

    private Long accountSetId;

    /**
     * 银行账号 (P4.1: AES-GCM 加密存储,读库自动解密)
     */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @EncryptedField("银行流水-银行账号")
    private String bankAccount;

    private LocalDate transactionDate;

    /**
     * 交易类型 1-收入 2-支出
     */
    private Integer transactionType;

    private BigDecimal amount;

    private BigDecimal balance;

    private String counterparty;

    private String summary;

    private String transactionNo;

    /**
     * 匹配状态 0-未匹配 1-已匹配
     */
    private Integer matchedStatus;

    private Long voucherId;

    private String remark;
}
