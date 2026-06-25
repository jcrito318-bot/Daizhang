package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户运营摘要VO（单客户视角）
 */
@Data
public class CustomerSummaryVO {

    private Long accountSetId;

    private String accountSetCode;

    private String accountSetName;

    private String companyName;

    private String taxpayerType;

    private String taxpayerTypeDesc;

    private String industryType;

    private Integer status;

    private String statusDesc;

    /**
     * 联系人
     */
    private String contactPerson;

    private String contactPhone;

    /**
     * 进行中任务数
     */
    private Integer pendingTaskCount;

    /**
     * 未审核凭证数
     */
    private Integer unauditedVoucherCount;

    /**
     * 未申报税种数
     */
    private Integer undeclaredTaxCount;

    /**
     * 最近更新时间
     */
    private java.time.LocalDateTime lastUpdateTime;
}
