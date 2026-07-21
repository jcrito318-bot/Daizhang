import request from '@/utils/request'
import { withSensitive } from '@/utils/request'
import type { Result } from '@/types/common'
import type { PeriodVO } from '@/types/voucher'
import type { PeriodCloseWizardRequest, PeriodCloseWizardVO } from '@/types/period'

export interface TrialBalanceResultVO {
  items: { subjectCode: string; subjectName: string; debitBalance: number; creditBalance: number }[]
  totalDebit: number
  totalCredit: number
  balanced: boolean
}

export interface ClosePeriodResultVO {
  success: boolean
  message: string
  uncheckedVouchers: number
}

export const periodApi = {
  listPeriods(accountSetId: number): Promise<Result<PeriodVO[]>> {
    return request.get('/accountset/period/list', { params: { accountSetId } })
  },
  initPeriods(accountSetId: number, year: number): Promise<Result<void>> {
    return request.post('/accountset/period/init', null, { params: { accountSetId, year } })
  },
  trialBalance(accountSetId: number, year: number, month: number): Promise<Result<TrialBalanceResultVO>> {
    return request.get('/period/trial-balance', { params: { accountSetId, year, month } })
  },
  /**
   * 结账(敏感操作,后端 @SensitiveOperation 标注)
   * 调用方应先通过 ElMessageBox.confirm 二次确认,确认后调用本方法。
   */
  close(accountSetId: number, year: number, month: number): Promise<Result<ClosePeriodResultVO>> {
    return request.post('/period/close', null, withSensitive({ params: { accountSetId, year, month } }))
  },
  reopen(accountSetId: number, year: number, month: number): Promise<Result<void>> {
    return request.post('/period/reopen', null, { params: { accountSetId, year, month } })
  },
  carryForward(accountSetId: number, year: number, month: number): Promise<Result<void>> {
    return request.post('/period/carry-forward', null, { params: { accountSetId, year, month } })
  },
  /**
   * 执行期末结账向导
   * <p>
   * 一键完成"结转损益 + 结账 + 下月开启"的月末结账流程,
   * 后端一次返回所有步骤结果,前端通过 setTimeout 模拟逐步执行展示动画。
   *
   * @param accountSetId 账套 ID
   * @param year          年度
   * @param month         月份
   * @param params        向导请求参数(skipOptionalSteps / autoCloseIfNoErrors,默认 true)
   */
  executeCloseWizard(
    accountSetId: number,
    year: number,
    month: number,
    params: PeriodCloseWizardRequest
  ): Promise<Result<PeriodCloseWizardVO>> {
    return request.post('/period/close-wizard', params, {
      params: { accountSetId, year, month }
    })
  }
}
