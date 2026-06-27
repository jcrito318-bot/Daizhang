package com.company.daizhang.module.industrycommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工商服务更新请求
 */
@Data
public class IndustryCommerceServiceUpdateRequest {

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
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 预计完成日期
     */
    private LocalDate expectedCompleteDate;

    /**
     * 成本金额（更新时若未传则为null,由service层判空保留原值,避免默认ZERO清零）
     */
    private BigDecimal costAmount;

    /**
     * 服务金额（更新时若未传则为null,由service层判空保留原值）
     */
    private BigDecimal serviceAmount;

    /**
     * 备注
     */
    private String remark;
}
