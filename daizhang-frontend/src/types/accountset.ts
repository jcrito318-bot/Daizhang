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
