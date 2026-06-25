package com.company.daizhang.module.customer.dto;

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

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
