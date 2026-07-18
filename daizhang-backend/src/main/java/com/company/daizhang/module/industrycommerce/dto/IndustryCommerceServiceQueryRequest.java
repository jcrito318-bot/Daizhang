package com.company.daizhang.module.industrycommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
