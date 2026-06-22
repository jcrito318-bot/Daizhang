export interface BankTransactionVO {
  id: number
  accountSetId: number
  bankAccount: string
  transactionDate: string
  transactionType: number
  transactionTypeName: string
  amount: number
  balance: number
  counterparty: string
  summary: string
  transactionNo: string
  matchedStatus: number
  matchedStatusName: string
  voucherId: number
  voucherNo: string
  remark: string
  createBy: number
  createByName: string
  createTime: string
}

export interface BankTransactionImportRequest {
  accountSetId: number
  bankAccount: string
  transactions: BankTransactionItem[]
}

export interface BankTransactionItem {
  transactionDate: string
  transactionType: number
  amount: number
  balance?: number
  counterparty?: string
  summary?: string
  transactionNo?: string
  remark?: string
}

export interface BankTransactionQueryRequest {
  accountSetId: number
  bankAccount?: string
  transactionType?: number
  matchedStatus?: number
  startDate?: string
  endDate?: string
  counterparty?: string
  summary?: string
  transactionNo?: string
  pageNum?: number
  pageSize?: number
}

export interface AutoMatchRequest {
  accountSetId: number
  bankAccount: string
  year: number
  month: number
}

export interface ManualMatchRequest {
  accountSetId: number
  transactionIds: number[]
  voucherId: number
}

export interface ReconciliationGenerateRequest {
  accountSetId: number
  bankAccount: string
  year: number
  month: number
  remark?: string
}

export interface BankReconciliationVO {
  id: number
  accountSetId: number
  bankAccount: string
  year: number
  month: number
  bankBalance: number
  bookBalance: number
  difference: number
  unreconciledItems: number
  reconciledDate: string
  reconciledBy: number
  reconciledByName: string
  status: number
  statusName: string
  remark: string
  createBy: number
  createByName: string
  createTime: string
  unreconciledTransactions: BankTransactionVO[]
}
