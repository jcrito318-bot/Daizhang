import request from '@/utils/request'
import { withSensitive } from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  VoucherVO,
  VoucherCreateRequest,
  VoucherQueryRequest,
  VoucherWordVO,
  VoucherTemplateVO,
  VoucherTemplateRequest,
  VoucherTemplateQueryRequest,
  AbstractLibraryVO,
  AbstractLibraryRequest,
  AbstractLibraryQueryRequest
} from '@/types/voucher'

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
  /**
   * 批量审核凭证(敏感操作,后端 @SensitiveOperation 标注)
   * 调用方应先通过 ElMessageBox.confirm 二次确认,确认后调用本方法。
   */
  batchAudit(ids: number[]): Promise<Result<number>> {
    return request.post('/voucher/batch-audit', ids, withSensitive())
  },
  /**
   * 批量反审核凭证
   */
  batchUnaudit(ids: number[]): Promise<Result<number>> {
    return request.post('/voucher/batch-unaudit', ids)
  },
  getWordList(accountSetId: number): Promise<Result<VoucherWordVO[]>> {
    return request.get('/voucher/word/list', { params: { accountSetId } })
  }
}

/**
 * 凭证模板 API
 * 代账会计可将每月重复录入的凭证(工资/折旧/社保)保存为模板,后续一键调用。
 */
export const templateApi = {
  /** 分页查询凭证模板 */
  getPage(params: VoucherTemplateQueryRequest): Promise<Result<PageResult<VoucherTemplateVO>>> {
    return request.get('/voucher/template/page', { params })
  },
  /** 不分页查询凭证模板(下拉用) */
  getList(accountSetId: number): Promise<Result<VoucherTemplateVO[]>> {
    return request.get('/voucher/template/list', { params: { accountSetId } })
  },
  /** 根据 ID 查询凭证模板(含明细) */
  getById(id: number): Promise<Result<VoucherTemplateVO>> {
    return request.get(`/voucher/template/${id}`)
  },
  /** 创建凭证模板 */
  create(data: VoucherTemplateRequest): Promise<Result<void>> {
    return request.post('/voucher/template', data)
  },
  /** 更新凭证模板 */
  update(id: number, data: VoucherTemplateRequest): Promise<Result<void>> {
    return request.put(`/voucher/template/${id}`, data)
  },
  /** 删除凭证模板 */
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/voucher/template/${id}`)
  },
  /** 应用模板,返回构造好的凭证数据(不直接保存,由前端调 voucherApi.create 保存) */
  apply(id: number): Promise<Result<VoucherTemplateVO>> {
    return request.post(`/voucher/template/${id}/apply`)
  }
}

/**
 * 常用摘要库 API
 * 凭证录入页面通过 el-autocomplete 模糊搜索摘要,按使用次数 DESC 智能排序。
 * 凭证保存时若使用了摘要库中的摘要,调用 incrementUse 累计使用次数。
 */
export const abstractApi = {
  /** 分页查询常用摘要 */
  getPage(params: AbstractLibraryQueryRequest): Promise<Result<PageResult<AbstractLibraryVO>>> {
    return request.get('/abstract/page', { params })
  },
  /** 搜索常用摘要(按使用次数 DESC 排序,用于凭证录入 el-autocomplete) */
  search(accountSetId: number, keyword: string, limit: number = 10): Promise<Result<AbstractLibraryVO[]>> {
    return request.get('/abstract/search', { params: { accountSetId, keyword, limit } })
  },
  /** 新增常用摘要 */
  create(data: AbstractLibraryRequest): Promise<Result<number>> {
    return request.post('/abstract', data)
  },
  /** 摘要使用次数 +1(凭证保存时调用) */
  incrementUse(id: number): Promise<Result<void>> {
    return request.post('/abstract/increment-use', null, { params: { id } })
  },
  /** 删除常用摘要 */
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/abstract/${id}`)
  }
}
