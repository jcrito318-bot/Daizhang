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
