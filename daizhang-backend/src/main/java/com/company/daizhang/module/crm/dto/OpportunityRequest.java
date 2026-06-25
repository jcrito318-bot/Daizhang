package com.company.daizhang.module.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商机创建/更新请求
 */
@Data
public class OpportunityRequest {

    @NotBlank(message = "商机名称不能为空")
    private String opportunityName;

    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String source;

    private String stage;

    @NotNull(message = "预计金额不能为空")
    private BigDecimal expectedAmount;

    private LocalDate expectedCloseDate;

    private Long assigneeId;

    private String assigneeName;

    private String remark;
}
