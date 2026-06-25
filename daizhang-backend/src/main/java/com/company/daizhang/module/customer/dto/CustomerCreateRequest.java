package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户创建请求
 */
@Data
public class CustomerCreateRequest {

    @NotBlank(message = "客户编码不能为空")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /**
     * 客户类型(企业/个人)
     */
    private String customerType;

    /**
     * 行业
     */
    private String industry;

    /**
     * 规模
     */
    private String scale;

    /**
     * 纳税人类型(一般纳税人/小规模纳税人)
     */
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

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 地址
     */
    private String address;

    /**
     * 税号
     */
    private String taxNo;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行账号
     */
    private String bankAccount;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 备注
     */
    private String remark;
}
