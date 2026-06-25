package com.company.daizhang.module.crm.dto;

import lombok.Data;

/**
 * 商机查询请求
 */
@Data
public class OpportunityQueryRequest {

    private String opportunityName;

    private String stage;

    private Long assigneeId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
