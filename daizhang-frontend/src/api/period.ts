import request from '@/utils/request'
import type { Result } from '@/types/common'

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
  listPeriods(accountSetId: number) {
    return request.get('/accountset/period/list', { params: { accountSetId } })
  },
  initPeriods(accountSetId: number, year: number) {
    return request.post('/accountset/period/init', null, { params: { accountSetId, year } })
  },
  closePeriod(accountSetId: number, year: number, month: number) {
    return request.post('/accountset/period/close', null, { params: { accountSetId, year, month } })
  },
  reopenPeriod(accountSetId: number, year: number, month: number) {
    return request.post('/accountset/period/reopen', null, { params: { accountSetId, year, month } })
  },
  trialBalance(accountSetId: number, year: number, month: number): Promise<Result<TrialBalanceResultVO>> {
    return request.get('/period/trial-balance', { params: { accountSetId, year, month } })
  },
  close(accountSetId: number, year: number, month: number): Promise<Result<ClosePeriodResultVO>> {
    return request.post('/period/close', null, { params: { accountSetId, year, month } })
  },
  reopen(accountSetId: number, year: number, month: number): Promise<Result<void>> {
    return request.post('/period/reopen', null, { params: { accountSetId, year, month } })
  },
  carryForward(accountSetId: number, year: number, month: number): Promise<Result<void>> {
    return request.post('/period/carry-forward', null, { params: { accountSetId, year, month } })
  }
}
