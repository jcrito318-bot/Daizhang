package com.company.daizhang.module.crm.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机视图对象
 */
@Data
public class OpportunityVO {

    private Long id;

    private String opportunityName;

    private String customerName;

    private String contactPerson;

    private String contactPhone;

    private String source;

    private String stage;

    private BigDecimal expectedAmount;

    private LocalDate expectedCloseDate;

    private Long assigneeId;

    private String assigneeName;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
