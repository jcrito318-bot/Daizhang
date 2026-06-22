import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { DocumentVO, DocumentCreateRequest, DocumentUpdateRequest, DocumentQueryRequest } from '@/types/document'

export const documentApi = {
  getPage(params: DocumentQueryRequest): Promise<Result<PageResult<DocumentVO>>> {
    return request.get('/document/page', { params })
  },
  getById(id: number): Promise<Result<DocumentVO>> {
    return request.get(`/document/${id}`)
  },
  create(data: DocumentCreateRequest): Promise<Result<void>> {
    return request.post('/document', data)
  },
  update(id: number, data: DocumentUpdateRequest): Promise<Result<void>> {
    return request.put(`/document/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/document/${id}`)
  },
  linkVoucher(id: number, voucherId: number): Promise<Result<void>> {
    return request.post(`/document/${id}/link-voucher/${voucherId}`)
  },
  unlinkVoucher(id: number): Promise<Result<void>> {
    return request.post(`/document/${id}/unlink-voucher`)
  }
}
