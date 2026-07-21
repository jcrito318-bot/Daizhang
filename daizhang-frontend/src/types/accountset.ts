export interface AccountSetVO {
  id: number
  code: string
  name: string
  companyName: string
  industryType: string
  accountingStandard: string
  startYear: number
  startMonth: number
  currencyCode: string
  taxpayerType: string
  contactPerson: string
  contactPhone: string
  address: string
  status: number
  createTime: string
}

export interface AccountSetCreateRequest {
  code: string
  name: string
  companyName?: string
  industryType?: string
  accountingStandard?: string
  startYear: number
  startMonth: number
  currencyCode?: string
  taxpayerType?: string
  contactPerson?: string
  contactPhone?: string
  address?: string
}

export interface AccountSetUpdateRequest {
  name?: string
  companyName?: string
  industryType?: string
  accountingStandard?: string
  taxpayerType?: string
  contactPerson?: string
  contactPhone?: string
  address?: string
  status?: number
}

export interface AccountSetQueryRequest {
  code?: string
  name?: string
  companyName?: string
  status?: number
  pageNum: number
  pageSize: number
}

/**
 * 账套偏好视图对象(对应后端 AccountSetPreferenceVO)
 * 用于顶部账套切换器的"最近访问 + 收藏置顶"
 */
export interface AccountSetPreferenceVO {
  /** 账套ID */
  accountSetId: number
  /** 账套名称 */
  accountSetName: string
  /** 是否收藏 0-否 1-是 */
  isFavorite: number
  /** 最近访问时间(ISO 字符串,可能为 null) */
  lastAccessedAt: string | null
  /** 访问次数 */
  accessCount: number
  /** 排序 */
  sortOrder: number
}

/**
 * 账套偏好排序项(批量更新排序用)
 */
export interface AccountSetSortItem {
  /** 账套ID */
  accountSetId: number
  /** 排序序号 */
  sortOrder: number
}

/**
 * 最近访问账套(前端 store 内部结构)
 */
export interface RecentAccountSet {
  id: number
  name: string
  lastAccessedAt: string | null
}
