package com.company.daizhang.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 服务合同实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cst_service_contract")
public class ServiceContract extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 服务内容
     */
    private String serviceContent;

    /**
     * 合同金额
     */
    private BigDecimal amount;

    /**
     * 付款方式
     */
    private String paymentMethod;

    /**
     * 状态(0-草稿 1-执行中 2-已完成 3-已终止)
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
