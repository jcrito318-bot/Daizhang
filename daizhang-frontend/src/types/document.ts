export interface DocumentVO {
  id: number
  accountSetId: number
  documentNo: string
  documentType: number
  documentDate: string
  amount: number
  taxAmount: number
  totalAmount: number
  sellerName: string
  buyerName: string
  invoiceCode: string
  invoiceNumber: string
  ocrContent: string
  fileUrl: string
  status: number
  voucherId: number
  remark: string
  createBy: number
  createByName: string
  createTime: string
  updateBy: number
  updateTime: string
}

export interface DocumentCreateRequest {
  accountSetId: number
  documentNo?: string
  documentType: number
  documentDate?: string
  amount?: number
  taxAmount?: number
  totalAmount?: number
  sellerName?: string
  buyerName?: string
  invoiceCode?: string
  invoiceNumber?: string
  ocrContent?: string
  fileUrl?: string
  remark?: string
}

export interface DocumentUpdateRequest {
  documentNo?: string
  documentType?: number
  documentDate?: string
  amount?: number
  taxAmount?: number
  totalAmount?: number
  sellerName?: string
  buyerName?: string
  invoiceCode?: string
  invoiceNumber?: string
  ocrContent?: string
  fileUrl?: string
  remark?: string
}

export interface DocumentQueryRequest {
  accountSetId: number
  documentType?: number
  status?: number
  documentNo?: string
  sellerName?: string
  buyerName?: string
  startDate?: string
  endDate?: string
  voucherId?: number
  pageNum?: number
  pageSize?: number
}
