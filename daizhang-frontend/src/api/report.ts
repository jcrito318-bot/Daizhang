import request from '@/utils/request'
import type { Result } from '@/types/common'
import type { BalanceSheetVO, IncomeStatementVO, ReportQueryRequest } from '@/types/report'

export const reportApi = {
  getBalanceSheet(params: ReportQueryRequest): Promise<Result<BalanceSheetVO>> {
    return request.get('/report/balance-sheet', { params })
  },
  getIncomeStatement(params: ReportQueryRequest): Promise<Result<IncomeStatementVO>> {
    return request.get('/report/income-statement', { params })
  },
  getSubjectBalanceTable(params: ReportQueryRequest): Promise<Result<unknown>> {
    return request.get('/report/subject-balance-table', { params })
  },
  exportBalanceSheet(params: ReportQueryRequest): Promise<Blob> {
    return request.get('/report/balance-sheet/export', { params, responseType: 'blob' })
  },
  exportIncomeStatement(params: ReportQueryRequest): Promise<Blob> {
    return request.get('/report/income-statement/export', { params, responseType: 'blob' })
  },
  exportSubjectBalanceTable(params: ReportQueryRequest): Promise<Blob> {
    return request.get('/report/subject-balance-table/export', { params, responseType: 'blob' })
  }
}
