package com.company.daizhang.module.period.vo;

import lombok.Data;

/**
 * 期末结账向导单步执行结果
 * <p>
 * 用于在向导界面逐步展示每个步骤的执行状态(success/failed/skipped)及结果消息。
 * status 取值参考 {@link PeriodCloseWizardVO#STEP_STATUS_SUCCESS} 等常量。
 */
@Data
public class WizardStepResult {

    /** 步骤状态: 成功 */
    public static final String STATUS_SUCCESS = "success";
    /** 步骤状态: 失败 */
    public static final String STATUS_FAILED = "failed";
    /** 步骤状态: 跳过 */
    public static final String STATUS_SKIPPED = "skipped";

    /** 步骤序号(1-7) */
    private Integer stepNo;

    /** 步骤名称 */
    private String stepName;

    /**
     * 步骤状态: success / failed / skipped
     */
    private String status;

    /** 结果消息(成功/失败/跳过原因) */
    private String message;

    /**
     * 关联凭证ID(可选)。
     * 结转损益/结转成本步骤生成凭证时回填,便于前端跳转查看凭证详情。
     */
    private Long voucherId;

    /**
     * 错误详情(可选)。
     * 失败步骤可展开查看完整错误堆栈/业务原因,前端"查看详情"按钮使用。
     */
    private String errorDetail;

    public static WizardStepResult success(int stepNo, String stepName, String message) {
        WizardStepResult r = new WizardStepResult();
        r.setStepNo(stepNo);
        r.setStepName(stepName);
        r.setStatus(STATUS_SUCCESS);
        r.setMessage(message);
        return r;
    }

    public static WizardStepResult success(int stepNo, String stepName, String message, Long voucherId) {
        WizardStepResult r = success(stepNo, stepName, message);
        r.setVoucherId(voucherId);
        return r;
    }

    public static WizardStepResult failed(int stepNo, String stepName, String message, String errorDetail) {
        WizardStepResult r = new WizardStepResult();
        r.setStepNo(stepNo);
        r.setStepName(stepName);
        r.setStatus(STATUS_FAILED);
        r.setMessage(message);
        r.setErrorDetail(errorDetail);
        return r;
    }

    public static WizardStepResult skipped(int stepNo, String stepName, String message) {
        WizardStepResult r = new WizardStepResult();
        r.setStepNo(stepNo);
        r.setStepName(stepName);
        r.setStatus(STATUS_SKIPPED);
        r.setMessage(message);
        return r;
    }
}
