import request from '@/utils/request'
import type { Result } from '@/types/common'
import type {
  AgingAnalysisVO,
  AgingItemVO,
  AgingSummaryVO,
  BalanceSheetVO,
  CashFlowStatementVO,
  IncomeStatementVO,
  ReportQueryRequest
} from '@/types/report'

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
  /**
   * 现金流量表（直接法，23项标准项目）
   */
  getCashFlowStatement(params: ReportQueryRequest): Promise<Result<CashFlowStatementVO>> {
    return request.get('/report/cash-flow', { params })
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

/**
 * 账龄分析查询参数
 */
export interface AgingQueryParams {
  /** 账套ID */
  accountSetId: number
  /** 截止日期(yyyy-MM-dd,不传时后端默认本月最后一天) */
  asOfDate?: string
}

/**
 * 账龄分析 API
 */
export const agingApi = {
  /** 应收账龄分析(按客户维度) */
  getReceivableAging(params: AgingQueryParams): Promise<Result<AgingItemVO[]>> {
    return request.get('/report/aging/receivable', { params })
  },
  /** 应付账龄分析(按供应商维度) */
  getPayableAging(params: AgingQueryParams): Promise<Result<AgingItemVO[]>> {
    return request.get('/report/aging/payable', { params })
  },
  /** 账龄分析汇总 */
  getAgingSummary(params: AgingQueryParams): Promise<Result<AgingSummaryVO>> {
    return request.get('/report/aging/summary', { params })
  },
  /** 完整账龄分析(应收+应付+汇总,一次请求) */
  getAgingAnalysis(params: AgingQueryParams): Promise<Result<AgingAnalysisVO>> {
    return request.get('/report/aging', { params })
  }
}
