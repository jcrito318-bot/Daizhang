export interface BalanceSheetVO {
  items: BalanceSheetItem[]
  totalAssets: number
  totalLiabilities: number
  totalEquity: number
}

export interface BalanceSheetItem {
  rowNo: number
  name: string
  code: string
  beginningBalance: number
  endingBalance: number
}

export interface IncomeStatementVO {
  items: IncomeStatementItem[]
  totalRevenue: number
  totalExpense: number
  netProfit: number
}

export interface IncomeStatementItem {
  rowNo: number
  name: string
  code: string
  currentAmount: number
  yearAmount: number
}

export interface ReportQueryRequest {
  accountSetId: number
  year: number
  month: number
}
