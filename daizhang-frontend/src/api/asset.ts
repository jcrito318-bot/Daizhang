import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  AssetCategoryVO, AssetCategoryCreateRequest, AssetCategoryUpdateRequest, AssetCategoryQueryRequest,
  FixedAssetVO, FixedAssetCreateRequest, FixedAssetUpdateRequest, FixedAssetQueryRequest,
  AssetStatusChangeRequest, DepreciationRecordVO, DepreciationRequest, DepreciationRecordQueryRequest
} from '@/types/asset'

export const assetApi = {
  // 资产分类管理
  getCategoryPage(params: AssetCategoryQueryRequest): Promise<Result<PageResult<AssetCategoryVO>>> {
    return request.get('/asset/category/page', { params })
  },
  getCategoryById(id: number): Promise<Result<AssetCategoryVO>> {
    return request.get(`/asset/category/${id}`)
  },
  createCategory(data: AssetCategoryCreateRequest): Promise<Result<void>> {
    return request.post('/asset/category', data)
  },
  updateCategory(id: number, data: AssetCategoryUpdateRequest): Promise<Result<void>> {
    return request.put(`/asset/category/${id}`, data)
  },
  deleteCategory(id: number): Promise<Result<void>> {
    return request.delete(`/asset/category/${id}`)
  },
  getCategoryTree(accountSetId: number): Promise<Result<AssetCategoryVO[]>> {
    return request.get('/asset/category/tree', { params: { accountSetId } })
  },

  // 固定资产管理
  getAssetPage(params: FixedAssetQueryRequest): Promise<Result<PageResult<FixedAssetVO>>> {
    return request.get('/asset/page', { params })
  },
  getAssetById(id: number): Promise<Result<FixedAssetVO>> {
    return request.get(`/asset/${id}`)
  },
  createAsset(data: FixedAssetCreateRequest): Promise<Result<void>> {
    return request.post('/asset', data)
  },
  updateAsset(id: number, data: FixedAssetUpdateRequest): Promise<Result<void>> {
    return request.put(`/asset/${id}`, data)
  },
  deleteAsset(id: number): Promise<Result<void>> {
    return request.delete(`/asset/${id}`)
  },
  changeAssetStatus(data: AssetStatusChangeRequest): Promise<Result<void>> {
    return request.post('/asset/change-status', data)
  },

  // 折旧管理
  getDepreciationRecordPage(params: DepreciationRecordQueryRequest): Promise<Result<PageResult<DepreciationRecordVO>>> {
    return request.get('/asset/depreciation/page', { params })
  },
  getDepreciationRecordById(id: number): Promise<Result<DepreciationRecordVO>> {
    return request.get(`/asset/depreciation/${id}`)
  },
  calculateDepreciation(data: DepreciationRequest): Promise<Result<void>> {
    return request.post('/asset/depreciation/calculate', data)
  },
  generateDepreciationVoucher(id: number): Promise<Result<void>> {
    return request.post(`/asset/depreciation/${id}/voucher`)
  },
  batchGenerateDepreciationVoucher(data: DepreciationRequest): Promise<Result<void>> {
    return request.post('/asset/depreciation/batch-voucher', data)
  }
}
