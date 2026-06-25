package com.company.daizhang.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现金流量表调整实体
 */
@Data
@TableName("rpt_cash_flow_adjustment")
public class CashFlowAdjustment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountSetId;

    @TableField("`year`")
    private Integer year;

    @TableField("`month`")
    private Integer month;

    /**
     * 调整项名称
     */
    private String itemName;

    /**
     * 经营/投资/筹资
     */
    private String category;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 调整后金额
     */
    private BigDecimal adjustedAmount;

    /**
     * 调整原因
     */
    private String adjustmentReason;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
