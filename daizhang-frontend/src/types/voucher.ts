export interface VoucherVO {
  id: number
  accountSetId: number
  voucherWordId: number
  voucherWordName: string
  voucherNo: string
  voucherDate: string
  year: number
  month: number
  totalDebit: number
  totalCredit: number
  attachmentCount: number
  status: number
  auditBy: number
  auditByName: string
  auditTime: string
  postBy: number
  postByName: string
  postTime: string
  createByName: string
  createTime: string
  details: VoucherDetailVO[]
}

export interface VoucherDetailVO {
  id: number
  voucherId: number
  lineNo: number
  summary: string
  subjectId: number
  subjectCode: string
  subjectName: string
  auxiliaryId: number
  debit: number
  credit: number
  quantity: number
  unitPrice: number
}

export interface VoucherCreateRequest {
  accountSetId: number
  voucherWordId?: number
  voucherDate: string
  year: number
  month: number
  attachmentCount?: number
  details: VoucherDetailRequest[]
}

export interface VoucherDetailRequest {
  lineNo?: number
  summary: string
  subjectId: number
  auxiliaryId?: number
  debit: number
  credit: number
  quantity?: number
  unitPrice?: number
}

export interface VoucherQueryRequest {
  accountSetId: number
  year?: number
  month?: number
  status?: number
  voucherNo?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

export interface VoucherWordVO {
  id: number
  accountSetId: number
  name: string
  code: string
  sortOrder: number
  status: number
}

export interface PeriodVO {
  id: number
  accountSetId: number
  year: number
  month: number
  status: number
  startDate: string
  endDate: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
}

// ==================== 凭证模板 ====================

/**
 * 凭证模板明细视图对象(对应 detail_json 中的单条分录)
 */
export interface VoucherTemplateDetailVO {
  subjectCode: string
  subjectName: string
  debitAmount: number
  creditAmount: number
  summary: string
}

/**
 * 凭证模板视图对象
 */
export interface VoucherTemplateVO {
  id: number
  accountSetId: number
  /** 模板编码(账套内唯一) */
  templateCode: string
  /** 模板名称 */
  templateName: string
  /** 模板分类: 工资/折旧/社保/税金/结转/其他 */
  templateCategory: string
  /** 凭证摘要 */
  summary: string
  /** 备注 */
  remark: string
  createBy: number
  createTime: string
  /** 分录明细列表(详情接口返回,列表接口可能为空) */
  details?: VoucherTemplateDetailVO[]
}

/**
 * 凭证模板明细请求(创建/更新模板时分录子表单)
 */
export interface VoucherTemplateDetailRequest {
  subjectCode: string
  subjectName?: string
  debitAmount?: number
  creditAmount?: number
  summary?: string
}

/**
 * 凭证模板创建/更新请求
 */
export interface VoucherTemplateRequest {
  accountSetId: number
  templateCode: string
  templateName: string
  templateCategory?: string
  summary?: string
  remark?: string
  details: VoucherTemplateDetailRequest[]
}

/**
 * 凭证模板分页查询请求
 */
export interface VoucherTemplateQueryRequest {
  accountSetId: number
  templateName?: string
  templateCategory?: string
  templateCode?: string
  pageNum: number
  pageSize: number
}

// ==================== 常用摘要库 ====================

/**
 * 常用摘要视图对象
 */
export interface AbstractLibraryVO {
  id: number
  accountSetId: number
  /** 摘要文本 */
  abstractText: string
  /** 分类: 工资/折旧/社保/税金/报销/采购/销售/其他 */
  abstractCategory: string
  /** 使用次数(用于智能排序) */
  useCount: number
  createBy: number
  createTime: string
}

/**
 * 常用摘要新增请求
 */
export interface AbstractLibraryRequest {
  accountSetId: number
  abstractText: string
  abstractCategory?: string
}

/**
 * 常用摘要分页查询请求
 */
export interface AbstractLibraryQueryRequest {
  accountSetId: number
  abstractText?: string
  abstractCategory?: string
  pageNum: number
  pageSize: number
}
