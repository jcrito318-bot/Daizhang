import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { DetailLedgerVO, GeneralLedgerVO, SubjectBalanceVO, CashJournalVO, LedgerQueryRequest, SubjectBalanceQueryRequest, DrillDownResultVO, DrillDownRequest } from '@/types/ledger'

export const ledgerApi = {
  getDetailLedger(params: LedgerQueryRequest): Promise<Result<PageResult<DetailLedgerVO>>> {
    return request.get('/ledger/detail', { params })
  },
  getGeneralLedger(params: LedgerQueryRequest): Promise<Result<GeneralLedgerVO[]>> {
    return request.get('/ledger/general', { params })
  },
  getSubjectBalance(params: SubjectBalanceQueryRequest): Promise<Result<SubjectBalanceVO[]>> {
    return request.get('/ledger/subject-balance', { params })
  },
  getCashJournal(params: LedgerQueryRequest): Promise<Result<PageResult<CashJournalVO>>> {
    return request.get('/ledger/cash-journal', { params })
  },
  getBankJournal(params: LedgerQueryRequest): Promise<Result<PageResult<CashJournalVO>>> {
    return request.get('/ledger/bank-journal', { params })
  },
  /**
   * 报表钻取:按"科目+期间+金额+方向"反查已过账凭证
   */
  drillDown(params: DrillDownRequest): Promise<Result<DrillDownResultVO>> {
    return request.get('/ledger/drill-down', { params })
  }
}
