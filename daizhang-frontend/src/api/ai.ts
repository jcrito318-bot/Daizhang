import request from '@/utils/request'

/**
 * AI相关API
 */
export const aiApi = {
  /**
   * 票据OCR识别（文件上传）
   * @param file 票据图片文件
   * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他（可选，不传则自动识别）
   */
  recognizeInvoice(file: File, invoiceType?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (invoiceType !== undefined) {
      formData.append('invoiceType', String(invoiceType))
    }
    return request.post('/ai/recognize/invoice', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /**
   * 票据OCR识别（Base64）
   * @param imageBase64 图片Base64编码
   * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他（可选）
   */
  recognizeInvoiceBase64(imageBase64: string, invoiceType?: number) {
    return request.post('/ai/recognize/invoice/base64', {
      imageBase64,
      invoiceType
    })
  },

  /**
   * 票据OCR识别（通过已上传的图片URL）
   * @param fileUrl 图片URL
   * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他（可选）
   */
  recognizeInvoiceByUrl(fileUrl: string, invoiceType?: number) {
    return request.post('/ai/recognize/invoice/url', { fileUrl, invoiceType })
  },

  /**
   * 智能记账建议
   */
  suggestAccounting(description: string, amount: number, accountSetId?: number) {
    return request.post('/ai/suggest/accounting', {
      description,
      amount,
      accountSetId
    })
  }
}
