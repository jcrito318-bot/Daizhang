package com.company.daizhang.module.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 商机查询请求
 */
@Data
public class OpportunityQueryRequest {

    private String opportunityName;

    private String stage;

    private Long assigneeId;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
