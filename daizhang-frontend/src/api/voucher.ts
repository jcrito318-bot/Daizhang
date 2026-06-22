import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { VoucherVO, VoucherCreateRequest, VoucherQueryRequest, VoucherWordVO } from '@/types/voucher'

export const voucherApi = {
  getPage(params: VoucherQueryRequest): Promise<Result<PageResult<VoucherVO>>> {
    return request.get('/voucher/page', { params })
  },
  getById(id: number): Promise<Result<VoucherVO>> {
    return request.get(`/voucher/${id}`)
  },
  create(data: VoucherCreateRequest): Promise<Result<VoucherVO>> {
    return request.post('/voucher', data)
  },
  update(id: number, data: VoucherCreateRequest): Promise<Result<VoucherVO>> {
    return request.put(`/voucher/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/voucher/${id}`)
  },
  audit(id: number): Promise<Result<void>> {
    return request.post(`/voucher/${id}/audit`)
  },
  unaudit(id: number): Promise<Result<void>> {
    return request.post(`/voucher/${id}/unaudit`)
  },
  post(id: number): Promise<Result<void>> {
    return request.post(`/voucher/${id}/post`)
  },
  getWordList(accountSetId: number): Promise<Result<VoucherWordVO[]>> {
    return request.get('/voucher/word/list', { params: { accountSetId } })
  }
}
