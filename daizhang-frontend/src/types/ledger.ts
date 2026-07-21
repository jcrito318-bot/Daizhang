export interface DetailLedgerVO {
  voucherDate: string
  voucherNo: string
  summary: string
  subjectCode: string
  subjectName: string
  debit: number
  credit: number
  direction: string
  balance: number
}

export interface GeneralLedgerVO {
  subjectCode: string
  subjectName: string
  beginDebit: number
  beginCredit: number
  periodDebit: number
  periodCredit: number
  endDebit: number
  endCredit: number
}

export interface SubjectBalanceVO {
  subjectCode: string
  subjectName: string
  level: number
  beginDebit: number
  beginCredit: number
  periodDebit: number
  periodCredit: number
  endDebit: number
  endCredit: number
  yearDebit: number
  yearCredit: number
}

export interface CashJournalVO {
  voucherDate: string
  voucherNo: string
  summary: string
  income: number
  expense: number
  balance: number
}

export interface LedgerQueryRequest {
  accountSetId: number
  subjectId?: number
  year: number
  month?: number
  // BUG-03 修复:GeneralLedger.vue 在 UI 上提供"起始月份/截止月份"输入框,
  // 调用接口时通过 spread 把 startMonth/endMonth 添加到请求中。
  // 后端 LedgerQueryRequest 当前不识别这两个字段(被 Spring MVC 忽略),
  // 前端类型把它们声明为可选字段以反映实际传输形态,避免运行时类型不一致。
  startMonth?: number
  endMonth?: number
  startDate?: string
  endDate?: string
  auxiliaryId?: number
  pageNum: number
  pageSize: number
}

export interface SubjectBalanceQueryRequest {
  accountSetId: number
  year: number
  startMonth?: number
  endMonth?: number
  level?: number
}

/**
 * 报表钻取方向:debit-借方 / credit-贷方
 */
export type DrillDownDirection = 'debit' | 'credit'

/**
 * 报表钻取命中的单条凭证分录
 */
export interface DrillDownVoucher {
  /** 凭证ID(用于跳转凭证详情) */
  voucherId: number
  /** 凭证号 */
  voucherNo: string
  /** 凭证日期(ISO 字符串) */
  voucherDate: string
  /** 凭证整体摘要(取首条命中分录摘要) */
  summary: string
  /** 命中分录的借方金额合计 */
  debitAmount: number
  /** 命中分录的贷方金额合计 */
  creditAmount: number
  /** 该凭证下命中条件的分录摘要列表 */
  abstracts: string[]
}

/**
 * 报表钻取结果
 */
export interface DrillDownResultVO {
  /** 命中的凭证分录列表 */
  vouchers: DrillDownVoucher[]
  /** 钻取使用的科目编码 */
  subjectCode: string
  /** 钻取使用的金额 */
  amount: number
  /** 钻取方向 */
  direction: DrillDownDirection
  /** 是否模糊匹配 */
  fuzzy: boolean
}

/**
 * 报表钻取请求参数
 */
export interface DrillDownRequest {
  accountSetId: number
  subjectCode: string
  year: number
  month: number
  amount: number
  direction: DrillDownDirection
  /** 是否模糊匹配(±0.01 容差),默认 false */
  fuzzy?: boolean
}
