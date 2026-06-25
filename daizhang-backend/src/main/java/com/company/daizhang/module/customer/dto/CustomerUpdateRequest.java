package com.company.daizhang.module.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户更新请求
 */
@Data
public class CustomerUpdateRequest {

    private String customerName;

    private String customerType;

    private String industry;

    private String scale;

    private String taxpayerType;

    /**
     * 客户等级(VIP/重要/普通/潜在)
     */
    private String customerLevel;

    /**
     * 行业类型
     */
    private String industryType;

    /**
     * 企业规模(微型/小型/中型/大型)
     */
    private String companySize;

    /**
     * 客户状态(0-潜在 1-在服 2-流失)
     */
    private Integer customerStatus;

    /**
     * 服务开始日期
     */
    private LocalDate serviceStartDate;

    /**
     * 服务结束日期
     */
    private LocalDate serviceEndDate;

    /**
     * 信用额度
     */
    private BigDecimal creditLimit;

    /**
     * 联系人数量
     */
    private Integer contactCount;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private String address;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private Integer status;

    private String remark;
}
