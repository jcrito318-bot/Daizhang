package com.company.daizhang.module.period.vo;

import lombok.Data;

import java.util.List;

/**
 * 期末结账向导结果 VO
 * <p>
 * 后端一次返回所有步骤结果,前端通过 setTimeout 模拟逐步执行展示动画。
 * 包含 7 个步骤的执行明细、整体状态汇总以及下月期间是否已开启标志。
 */
@Data
public class PeriodCloseWizardVO {

    /** 整体状态: 成功(所有必选步骤均成功) */
    public static final String OVERALL_SUCCESS = "success";
    /** 整体状态: 失败(有必选步骤失败,事务已回滚) */
    public static final String OVERALL_FAILED = "failed";
    /** 整体状态: 部分成功(无失败但有跳过) */
    public static final String OVERALL_PARTIAL = "partial";

    /** 步骤状态: 成功(与 WizardStepResult.STATUS_SUCCESS 保持一致) */
    public static final String STEP_STATUS_SUCCESS = "success";
    /** 步骤状态: 失败 */
    public static final String STEP_STATUS_FAILED = "failed";
    /** 步骤状态: 跳过 */
    public static final String STEP_STATUS_SKIPPED = "skipped";

    /**
     * 各步骤执行明细,按 stepNo 升序排列(1-7)。
     */
    private List<WizardStepResult> steps;

    /**
     * 整体状态: success / failed / partial。
     * <ul>
     *   <li>success: 所有必选步骤成功(跳过的可选步骤不影响)</li>
     *   <li>failed:  任一必选步骤失败,整个向导事务已回滚</li>
     *   <li>partial: 无失败但有跳过步骤(如本期已存在结转损益凭证)</li>
     * </ul>
     */
    private String overallStatus;

    /**
     * 下月会计期间是否已开启(新建或已存在且状态为"开")。
     * 仅当步骤 7 成功时为 true。
     */
    private boolean nextPeriodOpened;

    /** 成功步骤数 */
    private Integer successCount;

    /** 失败步骤数 */
    private Integer failedCount;

    /** 跳过步骤数 */
    private Integer skippedCount;
}
