import request from '@/utils/request'
import { withSensitive } from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  AccountSetVO,
  AccountSetCreateRequest,
  AccountSetUpdateRequest,
  AccountSetQueryRequest,
  AccountSetPreferenceVO,
  AccountSetSortItem
} from '@/types/accountset'

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
  /**
   * 删除账套(敏感操作,后端 @SensitiveOperation 标注)
   * 调用方应先通过 ElMessageBox.confirm 二次确认,确认后调用本方法。
   */
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/accountset/${id}`, withSensitive())
  },
  init(id: number): Promise<Result<void>> {
    return request.post(`/accountset/${id}/init`)
  }
}

/**
 * 账套偏好 API(顶部账套切换器:最近访问 + 收藏置顶)
 * 对应后端 UserAccountSetPreferenceController(/account-set/**)
 */
export const preferenceApi = {
  /** 获取当前用户的账套偏好列表(收藏在前,按最近访问时间倒序) */
  getPreferences(): Promise<Result<AccountSetPreferenceVO[]>> {
    return request.get('/account-set/preferences')
  },
  /** 记录账套访问(后端异步执行,更新最近访问时间+访问次数) */
  recordAccess(accountSetId: number): Promise<Result<void>> {
    return request.post(`/account-set/${accountSetId}/access`)
  },
  /** 切换账套收藏状态,返回切换后的收藏状态(true=已收藏) */
  toggleFavorite(accountSetId: number): Promise<Result<boolean>> {
    return request.post(`/account-set/${accountSetId}/favorite`)
  },
  /** 批量更新账套偏好排序 */
  updateSort(items: AccountSetSortItem[]): Promise<Result<void>> {
    return request.put('/account-set/preferences/sort', items)
  }
}
