export interface TaxDeclarationVO {
  id: number
  accountSetId: number
  year: number
  month: number
  taxType: string
  taxableAmount: number
  taxRate: number
  taxAmount: number
  declaredAmount: number
  actualAmount: number
  status: number
  declarationDate: string
  paymentDate: string
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  createByName: string
}

export interface TaxDeclarationCreateRequest {
  accountSetId: number
  year: number
  month: number
  taxType: string
  taxableAmount?: number
  taxRate?: number
  taxAmount?: number
  declaredAmount?: number
  actualAmount?: number
  declarationDate?: string
  paymentDate?: string
  remark?: string
}

export interface TaxDeclarationUpdateRequest {
  taxType?: string
  taxableAmount?: number
  taxRate?: number
  taxAmount?: number
  declaredAmount?: number
  actualAmount?: number
  status?: number
  declarationDate?: string
  paymentDate?: string
  remark?: string
}

export interface TaxDeclarationQueryRequest {
  accountSetId?: number
  year?: number
  month?: number
  taxType?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface TaxCalculationVO {
  id: number
  accountSetId: number
  year: number
  month: number
  taxType: string
  calculationItem: string
  amount: number
  rate: number
  taxAmount: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  createByName: string
}

export interface TaxCalculationCreateRequest {
  accountSetId: number
  year: number
  month: number
  taxType: string
  calculationItem: string
  amount?: number
  rate?: number
  taxAmount?: number
  remark?: string
}

export interface TaxCalculationUpdateRequest {
  taxType?: string
  calculationItem?: string
  amount?: number
  rate?: number
  taxAmount?: number
  remark?: string
}

export interface TaxCalculationQueryRequest {
  accountSetId?: number
  year?: number
  month?: number
  taxType?: string
  calculationItem?: string
  pageNum?: number
  pageSize?: number
}
