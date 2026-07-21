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

// ==================== 智能对账增强 ====================

/**
 * 智能对账匹配建议
 */
export interface MatchSuggestionVO {
  /** 银行流水ID */
  transactionId: number
  /** 凭证ID */
  voucherId: number
  /** 匹配分数(0-100) */
  score: number
  /** 匹配原因列表(形如 "金额精确匹配 +50") */
  reasons: string[]
  /** 匹配类型 exact/fuzzy/suggested */
  matchType: string
  /** 匹配类型名称(强烈建议/建议匹配/不推荐) */
  matchTypeName: string
  /** 流水交易日期(yyyy-MM-dd) */
  transactionDate: string
  /** 流水金额 */
  transactionAmount: number
  /** 流水交易类型 1-收入 2-支出 */
  transactionType: number
  /** 流水交易对方 */
  counterparty: string
  /** 流水摘要 */
  transactionSummary: string
  /** 凭证号 */
  voucherNo: string
  /** 凭证日期(yyyy-MM-dd) */
  voucherDate: string
  /** 凭证摘要 */
  voucherSummary: string
  /** 凭证金额 */
  voucherAmount: number
}

/**
 * 已匹配建议项(简化视图)
 * (历史保留:若仅需 transactionId/voucherId/score/type 四字段,可使用此类型)
 */
export interface MatchedItem {
  transactionId: number
  voucherId: number
  /** 匹配分数(0-100) */
  score: number
  /** 匹配类型 exact/fuzzy/suggested */
  type: string
}

/**
 * 智能对账匹配结果
 */
export interface MatchResultVO {
  /** 已匹配建议列表(按分数倒序,包含完整流水/凭证详情与匹配原因) */
  matched: MatchSuggestionVO[]
  /** 未匹配流水ID列表 */
  unmatchedTransactions: number[]
  /** 未匹配凭证ID列表 */
  unmatchedVouchers: number[]
  /** 已匹配建议总数 */
  totalMatched: number
  /** 未匹配总数(流水未匹配数 + 凭证未匹配数) */
  totalUnmatched: number
}

/**
 * 历史匹配模式
 */
export interface MatchHistoryPattern {
  /** 主键ID */
  id: number
  /** 账套ID */
  accountSetId: number
  /** 交易对方 */
  counterparty: string
  /** 金额范围最小值 */
  amountRangeMin: number
  /** 金额范围最大值 */
  amountRangeMax: number
  /** 对应凭证科目编码 */
  voucherSubjectCode: string
  /** 历史匹配次数 */
  matchCount: number
  /** 最近匹配时间(yyyy-MM-dd HH:mm:ss) */
  lastMatchedAt: string
  /** 创建时间(yyyy-MM-dd HH:mm:ss) */
  createTime: string
}

/**
 * 智能匹配请求参数
 */
export interface SmartMatchRequest {
  accountSetId: number
  bankAccount: string
  year: number
  month: number
}

/**
 * 智能匹配建议批量应用请求项
 */
export interface ApplySuggestionItem {
  transactionId: number
  voucherId: number
}

/**
 * 智能匹配建议批量应用请求
 */
export interface ApplySuggestionsRequest {
  accountSetId: number
  items: ApplySuggestionItem[]
}
