package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 凭证实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_voucher")
public class Voucher extends BaseEntity {

    private Long accountSetId;

    private Long voucherWordId;

    private String voucherNo;

    private LocalDate voucherDate;

    @TableField("`year`")
    private Integer year;

    @TableField("`month`")
    private Integer month;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private Integer attachmentCount;

    /**
     * 0-未审核 1-已审核 2-已过账 3-已作废
     */
    private Integer status;

    private Long auditBy;

    private LocalDateTime auditTime;

    private Long postBy;

    private LocalDateTime postTime;

    /**
     * 来源 0-手工录入
     */
    private Integer source;

    /**
     * 草稿状态 0-正常 1-草稿
     */
    private Integer draftStatus;

    /** 红冲关联的原凭证ID(P5.2.1,null=非红冲凭证) */
    private Long originalVoucherId;
}
