package com.company.daizhang.module.batch.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量操作单条结果 VO
 * <p>
 * 描述单个账套在批量操作中的执行结果。
 */
@Data
public class BatchOperationResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态:成功 */
    public static final String STATUS_SUCCESS = "success";
    /** 状态:部分失败(同一账套内部分项成功部分失败) */
    public static final String STATUS_PARTIAL = "partial";
    /** 状态:失败 */
    public static final String STATUS_FAILED = "failed";

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 账套名称(便于前端展示)
     */
    private String accountSetName;

    /**
     * 执行状态:success / partial / failed
     */
    private String status;

    /**
     * 结果消息(成功/失败原因,失败时包含详细错误信息)
     */
    private String message;
}
