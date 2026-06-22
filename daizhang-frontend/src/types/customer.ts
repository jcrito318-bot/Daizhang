export interface CustomerVO {
  id: number
  customerCode: string
  customerName: string
  customerType: string
  industry: string
  scale: string
  taxpayerType: string
  contactPerson: string
  contactPhone: string
  email: string
  address: string
  taxNo: string
  bankName: string
  bankAccount: string
  status: number
  accountSetId: number
  remark: string
  createTime: string
  updateTime: string
}

export interface CustomerCreateRequest {
  customerCode: string
  customerName: string
  customerType?: string
  industry?: string
  scale?: string
  taxpayerType?: string
  contactPerson?: string
  contactPhone?: string
  email?: string
  address?: string
  taxNo?: string
  bankName?: string
  bankAccount?: string
  accountSetId?: number
  remark?: string
}

export interface CustomerUpdateRequest {
  customerName?: string
  customerType?: string
  industry?: string
  scale?: string
  taxpayerType?: string
  contactPerson?: string
  contactPhone?: string
  email?: string
  address?: string
  taxNo?: string
  bankName?: string
  bankAccount?: string
  status?: number
  remark?: string
}

export interface CustomerQueryRequest {
  customerCode?: string
  customerName?: string
  customerType?: string
  industry?: string
  taxpayerType?: string
  contactPhone?: string
  status?: number
  accountSetId?: number
  pageNum?: number
  pageSize?: number
}

export interface ContractVO {
  id: number
  contractNo: string
  customerId: number
  customerName: string
  contractName: string
  contractType: string
  startDate: string
  endDate: string
  serviceContent: string
  amount: number
  paymentMethod: string
  status: number
  remark: string
  createTime: string
  updateTime: string
}

export interface ContractCreateRequest {
  contractNo: string
  customerId: number
  contractName: string
  contractType?: string
  startDate?: string
  endDate?: string
  serviceContent?: string
  amount?: number
  paymentMethod?: string
  remark?: string
}

export interface ContractUpdateRequest {
  contractName?: string
  contractType?: string
  startDate?: string
  endDate?: string
  serviceContent?: string
  amount?: number
  paymentMethod?: string
  status?: number
  remark?: string
}

export interface ContractQueryRequest {
  contractNo?: string
  contractName?: string
  customerId?: number
  contractType?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface PaymentVO {
  id: number
  contractId: number
  contractNo: string
  customerId: number
  customerName: string
  paymentDate: string
  amount: number
  paymentMethod: string
  paymentType: string
  voucherNo: string
  remark: string
  createTime: string
  updateTime: string
}

export interface PaymentCreateRequest {
  contractId?: number
  customerId: number
  paymentDate?: string
  amount: number
  paymentMethod?: string
  paymentType?: string
  voucherNo?: string
  remark?: string
}

export interface PaymentUpdateRequest {
  contractId?: number
  paymentDate?: string
  amount?: number
  paymentMethod?: string
  paymentType?: string
  voucherNo?: string
  remark?: string
}

export interface PaymentQueryRequest {
  contractId?: number
  customerId?: number
  paymentMethod?: string
  paymentType?: string
  pageNum?: number
  pageSize?: number
}
