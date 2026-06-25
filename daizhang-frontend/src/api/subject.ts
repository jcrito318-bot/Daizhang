import request from '@/utils/request'
import type { Result } from '@/types/common'
import type { SubjectVO, SubjectCreateRequest, SubjectUpdateRequest } from '@/types/subject'

export const subjectApi = {
  getTree(accountSetId: number): Promise<Result<SubjectVO[]>> {
    return request.get('/subject/tree', { params: { accountSetId } })
  },
  getById(id: number): Promise<Result<SubjectVO>> {
    return request.get(`/subject/${id}`)
  },
  create(data: SubjectCreateRequest): Promise<Result<SubjectVO>> {
    return request.post('/subject', data)
  },
  update(id: number, data: SubjectUpdateRequest): Promise<Result<SubjectVO>> {
    return request.put(`/subject/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/subject/${id}`)
  },
  initDefaultSubjects(accountSetId: number, accountingStandard: string = '小企业会计准则'): Promise<Result<void>> {
    return request.post('/subject/init', null, { params: { accountSetId, accountingStandard } })
  }
}
