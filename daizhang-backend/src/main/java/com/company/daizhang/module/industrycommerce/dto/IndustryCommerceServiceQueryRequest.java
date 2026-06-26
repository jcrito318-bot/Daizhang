package com.company.daizhang.module.industrycommerce.dto;

import lombok.Data;

/**
 * 工商服务查询请求
 */
@Data
public class IndustryCommerceServiceQueryRequest {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 服务类型:1注册 2变更 3注销
     */
    private Integer serviceType;

    /**
     * 服务状态:0待派工 1进行中 2已完成 3已取消
     */
    private Integer serviceStatus;

    /**
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
