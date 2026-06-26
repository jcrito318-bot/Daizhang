package com.company.daizhang.module.industrycommerce.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工商服务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_service")
public class IndustryCommerceService extends BaseEntity {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 合同ID（可空）
     */
    private Long contractId;

    /**
     * 服务类型:1注册 2变更 3注销
     */
    private Integer serviceType;

    /**
     * 服务项目名称
     */
    private String serviceName;

    /**
     * 服务状态:0待派工 1进行中 2已完成 3已取消
     */
    private Integer serviceStatus;

    /**
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 预计完成日期
     */
    private LocalDate expectedCompleteDate;

    /**
     * 实际完成日期
     */
    private LocalDate actualCompleteDate;

    /**
     * 成本金额
     */
    private BigDecimal costAmount = BigDecimal.ZERO;

    /**
     * 服务金额
     */
    private BigDecimal serviceAmount = BigDecimal.ZERO;

    /**
     * 备注
     */
    private String remark;
}
