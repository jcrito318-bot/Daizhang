import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  CustomerVO, CustomerCreateRequest, CustomerUpdateRequest, CustomerQueryRequest,
  ContractVO, ContractCreateRequest, ContractUpdateRequest, ContractQueryRequest,
  PaymentVO, PaymentCreateRequest, PaymentUpdateRequest, PaymentQueryRequest
} from '@/types/customer'

export const customerApi = {
  getPage(params: CustomerQueryRequest): Promise<Result<PageResult<CustomerVO>>> {
    return request.get('/customer/page', { params })
  },
  getList(): Promise<Result<CustomerVO[]>> {
    return request.get('/customer/list')
  },
  getById(id: number): Promise<Result<CustomerVO>> {
    return request.get(`/customer/${id}`)
  },
  create(data: CustomerCreateRequest): Promise<Result<void>> {
    return request.post('/customer', data)
  },
  update(id: number, data: CustomerUpdateRequest): Promise<Result<void>> {
    return request.put(`/customer/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/customer/${id}`)
  }
}

export const contractApi = {
  getPage(params: ContractQueryRequest): Promise<Result<PageResult<ContractVO>>> {
    return request.get('/contract/page', { params })
  },
  getByCustomerId(customerId: number): Promise<Result<ContractVO[]>> {
    return request.get(`/contract/customer/${customerId}`)
  },
  getById(id: number): Promise<Result<ContractVO>> {
    return request.get(`/contract/${id}`)
  },
  create(data: ContractCreateRequest): Promise<Result<void>> {
    return request.post('/contract', data)
  },
  update(id: number, data: ContractUpdateRequest): Promise<Result<void>> {
    return request.put(`/contract/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/contract/${id}`)
  }
}

export const paymentApi = {
  getPage(params: PaymentQueryRequest): Promise<Result<PageResult<PaymentVO>>> {
    return request.get('/payment/page', { params })
  },
  getByCustomerId(customerId: number): Promise<Result<PaymentVO[]>> {
    return request.get(`/payment/customer/${customerId}`)
  },
  getByContractId(contractId: number): Promise<Result<PaymentVO[]>> {
    return request.get(`/payment/contract/${contractId}`)
  },
  getById(id: number): Promise<Result<PaymentVO>> {
    return request.get(`/payment/${id}`)
  },
  create(data: PaymentCreateRequest): Promise<Result<void>> {
    return request.post('/payment', data)
  },
  update(id: number, data: PaymentUpdateRequest): Promise<Result<void>> {
    return request.put(`/payment/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/payment/${id}`)
  }
}
