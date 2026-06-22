import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  TaxDeclarationVO, TaxDeclarationCreateRequest, TaxDeclarationUpdateRequest, TaxDeclarationQueryRequest,
  TaxCalculationVO, TaxCalculationCreateRequest, TaxCalculationUpdateRequest, TaxCalculationQueryRequest
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
