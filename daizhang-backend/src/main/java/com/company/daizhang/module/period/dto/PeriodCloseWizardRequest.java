package com.company.daizhang.module.period.dto;

import lombok.Data;

/**
 * 期末结账向导请求体
 * <p>
 * 通过 POST body 传入向导执行参数。两个字段均可省略,缺省值均为 true。
 * <pre>
 * {"skipOptionalSteps": true, "autoCloseIfNoErrors": true}
 * </pre>
 */
@Data
public class PeriodCloseWizardRequest {

    /**
     * 是否跳过可选步骤(默认 true)。
     * <p>
     * true: 跳过步骤 2(期末调汇)、4(结转成本)、5(计提折旧)等可选项,适合商业账套快速结账。
     * false: 保留可选步骤执行(本项目可选步骤均未实现,会以 skipped 状态跳过)。
     */
    private Boolean skipOptionalSteps = Boolean.TRUE;

    /**
     * 数据完整性检查失败时是否中止后续步骤(默认 true)。
     * <p>
     * true: 步骤 1(数据完整性检查)发现未审核凭证/借贷不平凭证时,中止后续步骤并回滚事务。
     * false: 即使步骤 1 发现问题,仍继续执行后续步骤(强制结账场景,慎用)。
     */
    private Boolean autoCloseIfNoErrors = Boolean.TRUE;
}
