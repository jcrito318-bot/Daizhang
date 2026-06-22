import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { AccountSetVO, AccountSetCreateRequest, AccountSetUpdateRequest, AccountSetQueryRequest } from '@/types/accountset'

export const accountSetApi = {
  getPage(params: AccountSetQueryRequest): Promise<Result<PageResult<AccountSetVO>>> {
    return request.get('/accountset/page', { params })
  },
  getList(): Promise<Result<AccountSetVO[]>> {
    return request.get('/accountset/list')
  },
  getById(id: number): Promise<Result<AccountSetVO>> {
    return request.get(`/accountset/${id}`)
  },
  create(data: AccountSetCreateRequest): Promise<Result<AccountSetVO>> {
    return request.post('/accountset', data)
  },
  update(id: number, data: AccountSetUpdateRequest): Promise<Result<AccountSetVO>> {
    return request.put(`/accountset/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/accountset/${id}`)
  },
  init(id: number): Promise<Result<void>> {
    return request.post(`/accountset/${id}/init`)
  }
}
