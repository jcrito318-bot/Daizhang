package com.company.daizhang.module.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商机实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_opportunity")
public class Opportunity extends BaseEntity {

    /**
     * 商机名称
     */
    private String opportunityName;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 商机来源
     */
    private String source;

    /**
     * 阶段:线索/跟进/报价/谈判/成交/流失
     */
    private String stage;

    /**
     * 预计金额
     */
    private BigDecimal expectedAmount;

    /**
     * 预计成交日期
     */
    private LocalDate expectedCloseDate;

    /**
     * 负责人ID
     */
    private Long assigneeId;

    /**
     * 负责人姓名
     */
    private String assigneeName;

    /**
     * 备注
     */
    private String remark;
}
