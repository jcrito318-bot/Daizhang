export interface AssetCategoryVO {
  id: number
  accountSetId: number
  categoryCode: string
  categoryName: string
  depreciationMethod: string
  usefulLife: number
  residualRate: number
  parentId: number
  parentName: string
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
}

export interface AssetCategoryCreateRequest {
  accountSetId: number
  categoryCode: string
  categoryName: string
  depreciationMethod: string
  usefulLife: number
  residualRate: number
  parentId?: number
  remark?: string
}

export interface AssetCategoryUpdateRequest {
  categoryName: string
  depreciationMethod: string
  usefulLife: number
  residualRate: number
  parentId?: number
  remark?: string
}

export interface AssetCategoryQueryRequest {
  accountSetId?: number
  categoryCode?: string
  categoryName?: string
  depreciationMethod?: string
  parentId?: number
  pageNum?: number
  pageSize?: number
}

export interface FixedAssetVO {
  id: number
  accountSetId: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName: string
  purchaseDate: string
  purchaseAmount: number
  depreciationMethod: string
  usefulLife: number
  residualValue: number
  monthlyDepreciation: number
  accumulatedDeprecation: number
  netValue: number
  status: number
  statusName: string
  department: string
  keeper: string
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
}

export interface FixedAssetCreateRequest {
  accountSetId: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName?: string
  purchaseDate: string
  purchaseAmount: number
  depreciationMethod: string
  usefulLife: number
  residualValue: number
  department?: string
  keeper?: string
  remark?: string
}

export interface FixedAssetUpdateRequest {
  assetName: string
  categoryId?: number
  categoryName?: string
  depreciationMethod?: string
  usefulLife?: number
  residualValue?: number
  department?: string
  keeper?: string
  remark?: string
}

export interface FixedAssetQueryRequest {
  accountSetId?: number
  assetCode?: string
  assetName?: string
  categoryId?: number
  status?: number
  department?: string
  pageNum?: number
  pageSize?: number
}

export interface AssetStatusChangeRequest {
  assetId: number
  targetStatus: number
  remark?: string
}

export interface DepreciationRecordVO {
  id: number
  accountSetId: number
  assetId: number
  assetCode: string
  assetName: string
  year: number
  month: number
  depreciationAmount: number
  accumulatedDepreciation: number
  netValue: number
  voucherId: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
}

export interface DepreciationRequest {
  accountSetId: number
  year: number
  month: number
}

export interface DepreciationRecordQueryRequest {
  accountSetId?: number
  assetId?: number
  assetCode?: string
  assetName?: string
  year?: number
  month?: number
  pageNum?: number
  pageSize?: number
}
