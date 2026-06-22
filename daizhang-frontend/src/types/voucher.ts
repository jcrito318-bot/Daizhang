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
