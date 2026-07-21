import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  BankTransactionVO, BankTransactionImportRequest, BankTransactionQueryRequest,
  AutoMatchRequest, ManualMatchRequest, ReconciliationGenerateRequest, BankReconciliationVO,
  SmartMatchRequest, MatchResultVO, MatchHistoryPattern, ApplySuggestionsRequest
} from '@/types/bank'

export const bankApi = {
  // 银行流水
  importTransactions(data: BankTransactionImportRequest): Promise<Result<number>> {
    return request.post('/bank/transaction/import', data)
  },
  getTransactionPage(params: BankTransactionQueryRequest): Promise<Result<PageResult<BankTransactionVO>>> {
    return request.get('/bank/transaction/page', { params })
  },
  getTransactionById(id: number): Promise<Result<BankTransactionVO>> {
    return request.get(`/bank/transaction/${id}`)
  },
  deleteTransaction(id: number): Promise<Result<void>> {
    return request.delete(`/bank/transaction/${id}`)
  },

  // 匹配
  autoMatch(data: AutoMatchRequest): Promise<Result<number>> {
    return request.post('/bank/match/auto', data)
  },
  manualMatch(data: ManualMatchRequest): Promise<Result<void>> {
    return request.post('/bank/match/manual', data)
  },
  cancelMatch(id: number): Promise<Result<void>> {
    return request.post(`/bank/match/cancel/${id}`)
  },

  // 对账单
  generateReconciliation(data: ReconciliationGenerateRequest): Promise<Result<BankReconciliationVO>> {
    return request.post('/bank/reconciliation/generate', data)
  },
  getReconciliation(id: number): Promise<Result<BankReconciliationVO>> {
    return request.get(`/bank/reconciliation/${id}`)
  },
  getReconciliationPage(params: BankTransactionQueryRequest): Promise<Result<PageResult<BankReconciliationVO>>> {
    return request.get('/bank/reconciliation/page', { params })
  }
}

/**
 * 智能对账增强 API
 * 基于评分模型(金额/日期/摘要/对方/历史模式多维度加权)的模糊匹配 + 历史模式学习。
 */
export const smartReconciliationApi = {
  /**
   * 智能匹配 - 对指定账套+银行账号+年月的未匹配流水与候选凭证进行评分,返回建议列表
   */
  smartMatch(params: SmartMatchRequest): Promise<Result<MatchResultVO>> {
    return request.post('/bank/smart-match', null, { params })
  },

  /**
   * 批量应用建议 - 用户确认接受的建议后回写匹配关系并自动学习历史模式
   */
  applySuggestions(data: ApplySuggestionsRequest): Promise<Result<number>> {
    return request.post('/bank/apply-suggestions', data)
  },

  /**
   * 查询历史匹配模式
   */
  getMatchPatterns(accountSetId: number): Promise<Result<MatchHistoryPattern[]>> {
    return request.get('/bank/match-history/patterns', { params: { accountSetId } })
  },

  /**
   * 手动触发历史模式学习 - 扫描已匹配流水聚合金额范围与科目编码
   */
  learnPatterns(accountSetId: number): Promise<Result<number>> {
    return request.post('/bank/match-history/learn', { accountSetId })
  }
}
