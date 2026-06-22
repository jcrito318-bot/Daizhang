import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  BankTransactionVO, BankTransactionImportRequest, BankTransactionQueryRequest,
  AutoMatchRequest, ManualMatchRequest, ReconciliationGenerateRequest, BankReconciliationVO
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
