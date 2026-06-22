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
