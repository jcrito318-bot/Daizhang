/**
 * 期末结账向导相关类型定义
 * <p>
 * 与后端 com.company.daizhang.module.period.vo.PeriodCloseWizardVO 对应,
 * 用于一键完成"结转损益 + 结账 + 下月开启"的月末结账流程。
 */

/**
 * 向导单步执行结果(对应后端 WizardStepResult)
 */
export interface WizardStep {
  /** 步骤序号(1-7) */
  stepNo: number
  /** 步骤名称 */
  stepName: string
  /** 步骤状态: success / failed / skipped */
  status: WizardStepStatus
  /** 结果消息(成功/失败/跳过原因) */
  message: string
  /** 关联凭证ID(可选,结转损益/结转成本步骤生成凭证时回填) */
  voucherId?: number
  /** 错误详情(可选,失败步骤可展开查看) */
  errorDetail?: string
}

/** 步骤状态字面量类型 */
export type WizardStepStatus = 'success' | 'failed' | 'skipped'

/** 整体状态字面量类型 */
export type WizardOverallStatus = 'success' | 'failed' | 'partial'

/**
 * 期末结账向导请求体(对应后端 PeriodCloseWizardRequest)
 */
export interface PeriodCloseWizardRequest {
  /** 是否跳过可选步骤(期末调汇/结转成本/计提折旧),默认 true */
  skipOptionalSteps?: boolean
  /** 数据完整性检查失败时是否中止后续步骤,默认 true */
  autoCloseIfNoErrors?: boolean
}

/**
 * 期末结账向导结果 VO(对应后端 PeriodCloseWizardVO)
 */
export interface PeriodCloseWizardVO {
  /** 各步骤执行明细,按 stepNo 升序排列(1-7) */
  steps: WizardStep[]
  /** 整体状态: success / failed / partial */
  overallStatus: WizardOverallStatus
  /** 下月会计期间是否已开启(新建或已存在且状态为"开") */
  nextPeriodOpened: boolean
  /** 成功步骤数 */
  successCount: number
  /** 失败步骤数 */
  failedCount: number
  /** 跳过步骤数 */
  skippedCount: number
}
