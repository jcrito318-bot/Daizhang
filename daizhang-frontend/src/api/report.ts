import request from '@/utils/request'
import type { Result } from '@/types/common'
import type { BalanceSheetVO, IncomeStatementVO, ReportQueryRequest } from '@/types/report'

export const reportApi = {
  getBalanceSheet(params: ReportQueryRequest): Promise<Result<BalanceSheetVO>> {
    return request.get('/report/balance-sheet', { params })
  },
  getIncomeStatement(params: ReportQueryRequest): Promise<Result<IncomeStatementVO>> {
    return request.get('/report/income-statement', { params })
  }
}
