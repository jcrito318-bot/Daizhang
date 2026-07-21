import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'

// ==================== 请求接口 ====================

/**
 * 批量审核凭证请求项(单账套)
 */
export interface VoucherAuditItem {
  /** 账套ID */
  accountSetId: number
  /** 年度 */
  year: number
  /** 月份 */
  month: number
  /**
   * 凭证ID列表。
   * 为空(或未传)时审核该期间所有未审核凭证;非空时仅审核指定的凭证。
   */
  voucherIds?: number[]
}

/**
 * 批量审核凭证请求
 */
export interface BatchVoucherAuditRequest {
  items: VoucherAuditItem[]
}

/**
 * 批量结账请求项(单账套)
 */
export interface PeriodCloseItem {
  /** 账套ID */
  accountSetId: number
  /** 年度 */
  year: number
  /** 月份 */
  month: number
}

/**
 * 批量结账请求
 */
export interface BatchPeriodCloseRequest {
  items: PeriodCloseItem[]
}

/**
 * 批量生成报表请求项(单账套)
 */
export interface ReportGenerateItem {
  /** 账套ID */
  accountSetId: number
  /** 年度 */
  year: number
  /** 月份 */
  month: number
  /**
   * 报表类型列表,可选值:
   * balance-sheet(资产负债表) / income-statement(利润表)
   * / cash-flow-statement(现金流量表) / subject-balance(科目余额表)
   */
  reportTypes: string[]
}

/**
 * 批量生成报表请求
 */
export interface BatchReportGenerateRequest {
  items: ReportGenerateItem[]
}

/**
 * 批量操作历史查询请求(分页)
 */
export interface BatchHistoryQueryRequest {
  /**
   * 操作类型(可选):
   * voucher-audit / period-close / report-generate,为空时查询全部批量操作
   */
  operationType?: string
  /** 起始日期(yyyy-MM-dd) */
  startDate?: string
  /** 结束日期(yyyy-MM-dd) */
  endDate?: string
  /** 页码 */
  pageNum?: number
  /** 每页条数 */
  pageSize?: number
}

// ==================== 响应接口 ====================

/** 批量操作执行状态 */
export type BatchOperationStatus = 'success' | 'partial' | 'failed'

/**
 * 批量操作单条结果
 */
export interface BatchOperationResultVO {
  /** 账套ID */
  accountSetId: number
  /** 账套名称 */
  accountSetName: string | null
  /** 执行状态:success / partial / failed */
  status: BatchOperationStatus
  /** 结果消息(成功/失败原因) */
  message: string
}

/**
 * 批量操作响应
 */
export interface BatchOperationResponse {
  /** 总数(参与操作的账套数量) */
  totalCount: number
  /** 成功数 */
  successCount: number
  /** 失败数(含 partial 与 failed) */
  failCount: number
  /** 各账套详细结果列表 */
  results: BatchOperationResultVO[]
}

/**
 * 操作日志记录(批量操作历史)
 */
export interface SysOperationLog {
  id: number
  userId: number | null
  username: string | null
  operation: string | null
  method: string | null
  params: string | null
  ip: string | null
  status: number | null
  errorMsg: string | null
  costTime: number | null
  createTime: string | null
}

export const batchApi = {
  /** 批量审核凭证(跨多个账套) */
  batchAuditVoucher(data: BatchVoucherAuditRequest): Promise<Result<BatchOperationResponse>> {
    return request.post('/batch/voucher/audit', data)
  },
  /** 批量结账(跨多个账套) */
  batchClosePeriod(data: BatchPeriodCloseRequest): Promise<Result<BatchOperationResponse>> {
    return request.post('/batch/period/close', data)
  },
  /** 批量生成报表(跨多个账套) */
  batchGenerateReport(data: BatchReportGenerateRequest): Promise<Result<BatchOperationResponse>> {
    return request.post('/batch/report/generate', data)
  },
  /** 查询批量操作历史(分页) */
  queryHistory(params: BatchHistoryQueryRequest): Promise<Result<PageResult<SysOperationLog>>> {
    return request.get('/batch/history', { params })
  }
}
