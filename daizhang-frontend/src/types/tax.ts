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

// ==================== 税负预警 ====================

/**
 * 税负预警等级
 * - normal: 绿色(在预警区间内)
 * - warning: 黄色(高于上限,可能多交税)
 * - danger: 红色(低于下限,涉嫌异常低)
 */
export type TaxWarningLevel = 'normal' | 'warning' | 'danger'

/**
 * 当月税负预警视图对象
 */
export interface TaxWarningVO {
  accountSetId: number
  year: number
  month: number
  /** 账套所属行业代码 */
  industryCode: string
  /** 账套所属行业名称 */
  industryName: string
  // ===== 增值税 =====
  /** 增值税实际税负率(0.0300 = 3.00%) */
  vatActualRate: number
  /** 增值税行业基准税负率 */
  vatBenchmarkRate: number
  /** 增值税税负率预警下限 */
  vatWarningLow: number
  /** 增值税税负率预警上限 */
  vatWarningHigh: number
  /** 增值税税负率预警等级 */
  vatWarningLevel: TaxWarningLevel
  // ===== 企业所得税 =====
  /** 企业所得税实际税负率 */
  eitActualRate: number
  /** 企业所得税行业基准税负率 */
  eitBenchmarkRate: number
  /** 企业所得税税负率预警下限 */
  eitWarningLow: number
  /** 企业所得税税负率预警上限 */
  eitWarningHigh: number
  /** 企业所得税税负率预警等级 */
  eitWarningLevel: TaxWarningLevel
  // ===== 明细金额 =====
  /** 实际缴纳增值税 */
  vatActualAmount: number
  /** 实际缴纳企业所得税 */
  eitActualAmount: number
  /** 不含税销售收入(主营业务收入) */
  salesRevenue: number
  /** 建议 */
  suggestions: string[]
  /** 预警明细 */
  warnings: string[]
}

/**
 * 全年税负趋势(单月)
 */
export interface TaxTrendVO {
  year: number
  month: number
  /** 增值税实际税负率(无数据时为 null) */
  vatRate: number | null
  /** 企业所得税实际税负率(无数据时为 null) */
  eitRate: number | null
}

/**
 * 行业税负率基准
 */
export interface TaxBenchmark {
  id: number
  industryCode: string
  industryName: string
  /** 增值税税负率基准 */
  vatBenchmarkRate: number
  /** 增值税税负率下限预警 */
  vatWarningLow: number
  /** 增值税税负率上限预警 */
  vatWarningHigh: number
  /** 企业所得税税负率基准 */
  eitBenchmarkRate: number
  /** 企业所得税税负率下限预警 */
  eitWarningLow: number
  /** 企业所得税税负率上限预警 */
  eitWarningHigh: number
  createTime: string
  updateTime: string
}

/**
 * 行业税负率基准更新请求(ADMIN only)
 */
export interface TaxBenchmarkUpdateRequest {
  vatBenchmarkRate: number
  vatWarningLow: number
  vatWarningHigh: number
  eitBenchmarkRate: number
  eitWarningLow: number
  eitWarningHigh: number
}
