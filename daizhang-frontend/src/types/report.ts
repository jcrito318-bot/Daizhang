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

/**
 * 现金流量表明细项
 */
export interface CashFlowItemVO {
  /** 项目编码（对应 CashFlowItem 枚举名） */
  itemCode: string
  /** 项目名称 */
  itemName: string
  /** 分类：operating / investing / financing / other / summary */
  category: string
  /** 方向：inflow / outflow / net / balance */
  direction: string
  /** 金额 */
  amount: number
}

/**
 * 现金流量表
 */
export interface CashFlowStatementVO {
  year: number
  month: number
  accountSetId: number

  /** 经营活动现金流入 */
  operatingInflow: number
  /** 经营活动现金流出 */
  operatingOutflow: number
  /** 经营活动现金净额 */
  operatingNetFlow: number

  /** 投资活动现金流入 */
  investingInflow: number
  /** 投资活动现金流出 */
  investingOutflow: number
  /** 投资活动现金净额 */
  investingNetFlow: number

  /** 筹资活动现金流入 */
  financingInflow: number
  /** 筹资活动现金流出 */
  financingOutflow: number
  /** 筹资活动现金净额 */
  financingNetFlow: number

  /** 现金及现金等价物净增加额 */
  netIncrease: number

  /** 经营活动产生的现金流量净额 */
  operatingNetCashFlow: number
  /** 投资活动产生的现金流量净额 */
  investingNetCashFlow: number
  /** 筹资活动产生的现金流量净额 */
  financingNetCashFlow: number
  /** 现金及现金等价物净增加额 */
  netIncreaseInCash: number
  /** 期初现金及现金等价物余额 */
  beginningCashBalance: number
  /** 期末现金及现金等价物余额 */
  endingCashBalance: number
  /** 汇率变动对现金的影响 */
  exchangeEffect: number
  /** 勾稽校验是否通过 */
  balanceCheck: boolean

  /** 明细项列表 */
  items: CashFlowItemVO[]
}

// ==================== 账龄分析 ====================

/**
 * 账龄分桶金额
 * 按凭证日期与截止日期的天数差分桶
 */
export interface AgeBucket {
  /** 0-30 天(正常) */
  within30Days: number
  /** 31-60 天(关注) */
  days31To60: number
  /** 61-90 天(预警) */
  days61To90: number
  /** 91-180 天(逾期) */
  days91To180: number
  /** 180 天以上(坏账风险) */
  over180Days: number
}

/**
 * 账龄分析明细项(单个客户/供应商)
 */
export interface AgingItemVO {
  /** 客户/供应商ID */
  customerId: number
  /** 客户/供应商名称 */
  customerName: string
  /** 总金额(未核销余额合计) */
  totalAmount: number
  /** 账龄分桶金额 */
  ageBuckets: AgeBucket
  /** 最早凭证日期 */
  oldestDate: string
  /** 最长逾期天数(从最早凭证日期到截止日期) */
  oldestDays: number | null
  /** 涉及凭证条数 */
  voucherCount: number
}

/**
 * 账龄分析汇总信息
 */
export interface AgingSummaryVO {
  /** 应收总额 */
  totalReceivable: number
  /** 应付总额 */
  totalPayable: number
  /** 逾期应收(31 天以上) */
  overdueReceivable: number
  /** 逾期应付(31 天以上) */
  overduePayable: number
  /** 有未核销应收余额的客户数 */
  customerCount: number
  /** 有未核销应付余额的供应商数 */
  supplierCount: number
}

/**
 * 完整账龄分析结果(应收 + 应付 + 汇总)
 */
export interface AgingAnalysisVO {
  /** 账套ID */
  accountSetId: number
  /** 截止日期 */
  asOfDate: string
  /** 客户应收账龄列表 */
  customerAging: AgingItemVO[]
  /** 供应商应付账龄列表 */
  supplierAging: AgingItemVO[]
  /** 汇总信息 */
  summary: AgingSummaryVO
}
