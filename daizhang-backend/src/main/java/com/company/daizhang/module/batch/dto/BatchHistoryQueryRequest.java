package com.company.daizhang.module.batch.dto;

import lombok.Data;

/**
 * 批量操作历史查询请求(分页)
 */
@Data
public class BatchHistoryQueryRequest {

    /**
     * 操作类型(可选),用于筛选特定批量操作:
     * voucher-audit / period-close / report-generate,为空时查询全部批量操作
     */
    private String operationType;

    /**
     * 起始日期(yyyy-MM-dd)
     */
    private String startDate;

    /**
     * 结束日期(yyyy-MM-dd)
     */
    private String endDate;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
