import request from '@/utils/request'
import type { Result } from '@/types/common'
import type { SubjectVO, SubjectCreateRequest, SubjectUpdateRequest } from '@/types/subject'

export const subjectApi = {
  getTree(accountSetId: number): Promise<Result<SubjectVO[]>> {
    return request.get('/subject/tree', { params: { accountSetId } })
  },
  create(data: SubjectCreateRequest): Promise<Result<SubjectVO>> {
    return request.post('/subject', data)
  },
  update(id: number, data: SubjectUpdateRequest): Promise<Result<SubjectVO>> {
    return request.put(`/subject/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/subject/${id}`)
  }
}
