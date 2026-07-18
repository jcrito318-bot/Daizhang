package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 客户服务报告查询请求
 */
@Data
public class ServiceReportQueryRequest {

    private Long accountSetId;

    private Long customerId;

    private Integer reportYear;

    private Integer reportMonth;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
