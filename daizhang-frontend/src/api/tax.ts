import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  TaxDeclarationVO, TaxDeclarationCreateRequest, TaxDeclarationUpdateRequest, TaxDeclarationQueryRequest,
  TaxCalculationVO, TaxCalculationCreateRequest, TaxCalculationUpdateRequest, TaxCalculationQueryRequest,
  TaxWarningVO, TaxTrendVO, TaxBenchmark, TaxBenchmarkUpdateRequest
} from '@/types/tax'

export const taxApi = {
  // 税务申报
  getDeclarationPage(params: TaxDeclarationQueryRequest): Promise<Result<PageResult<TaxDeclarationVO>>> {
    return request.get('/tax/declaration/page', { params })
  },
  getDeclarationById(id: number): Promise<Result<TaxDeclarationVO>> {
    return request.get(`/tax/declaration/${id}`)
  },
  createDeclaration(data: TaxDeclarationCreateRequest): Promise<Result<void>> {
    return request.post('/tax/declaration', data)
  },
  updateDeclaration(id: number, data: TaxDeclarationUpdateRequest): Promise<Result<void>> {
    return request.put(`/tax/declaration/${id}`, data)
  },
  deleteDeclaration(id: number): Promise<Result<void>> {
    return request.delete(`/tax/declaration/${id}`)
  },
  declare(id: number): Promise<Result<void>> {
    return request.post(`/tax/declaration/${id}/declare`)
  },
  pay(id: number): Promise<Result<void>> {
    return request.post(`/tax/declaration/${id}/pay`)
  },

  // 税务计算
  getCalculationPage(params: TaxCalculationQueryRequest): Promise<Result<PageResult<TaxCalculationVO>>> {
    return request.get('/tax/calculation/page', { params })
  },
  getCalculationById(id: number): Promise<Result<TaxCalculationVO>> {
    return request.get(`/tax/calculation/${id}`)
  },
  createCalculation(data: TaxCalculationCreateRequest): Promise<Result<void>> {
    return request.post('/tax/calculation', data)
  },
  updateCalculation(id: number, data: TaxCalculationUpdateRequest): Promise<Result<void>> {
    return request.put(`/tax/calculation/${id}`, data)
  },
  deleteCalculation(id: number): Promise<Result<void>> {
    return request.delete(`/tax/calculation/${id}`)
  },

  // 税务统计
  calculateTax(accountSetId: number, year: number, month: number, taxType: string): Promise<Result<number>> {
    return request.get('/tax/calculate', { params: { accountSetId, year, month, taxType } })
  }
}

// 税负预警 API
export const warningApi = {
  /** 当月税负预警 */
  getWarning(accountSetId: number, year: number, month: number): Promise<Result<TaxWarningVO>> {
    return request.get('/tax/warning', { params: { accountSetId, year, month } })
  },
  /** 全年税负趋势 */
  getTrend(accountSetId: number, year: number): Promise<Result<TaxTrendVO[]>> {
    return request.get('/tax/trend', { params: { accountSetId, year } })
  },
  /** 行业税负率基准列表 */
  listBenchmarks(): Promise<Result<TaxBenchmark[]>> {
    return request.get('/tax/benchmarks')
  },
  /** 更新行业税负率基准(ADMIN only) */
  updateBenchmark(id: number, data: TaxBenchmarkUpdateRequest): Promise<Result<void>> {
    return request.put(`/tax/benchmarks/${id}`, data)
  }
}
