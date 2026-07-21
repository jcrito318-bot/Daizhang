package com.company.daizhang.module.batch.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量操作响应
 * <p>
 * 汇总本次批量操作的整体结果,包含成功/失败统计与每个账套的详细结果。
 */
@Data
public class BatchOperationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总数(参与操作的账套数量)
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数(含 partial 与 failed)
     */
    private Integer failCount;

    /**
     * 各账套详细结果列表
     */
    private List<BatchOperationResultVO> results;
}
