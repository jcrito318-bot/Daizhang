import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type {
  EmployeeVO, EmployeeCreateRequest, EmployeeUpdateRequest, EmployeeQueryRequest,
  SalaryItemVO, SalaryItemCreateRequest, SalaryItemUpdateRequest, SalaryItemQueryRequest,
  SalarySheetVO, SalarySheetCreateRequest, SalarySheetUpdateRequest, SalarySheetQueryRequest,
  SalaryCalculateRequest, SalaryVoucherGenerateRequest
} from '@/types/salary'

export const salaryApi = {
  // 员工管理
  getEmployeePage(params: EmployeeQueryRequest): Promise<Result<PageResult<EmployeeVO>>> {
    return request.get('/salary/employee/page', { params })
  },
  getEmployeeById(id: number): Promise<Result<EmployeeVO>> {
    return request.get(`/salary/employee/${id}`)
  },
  createEmployee(data: EmployeeCreateRequest): Promise<Result<void>> {
    return request.post('/salary/employee', data)
  },
  updateEmployee(id: number, data: EmployeeUpdateRequest): Promise<Result<void>> {
    return request.put(`/salary/employee/${id}`, data)
  },
  deleteEmployee(id: number): Promise<Result<void>> {
    return request.delete(`/salary/employee/${id}`)
  },

  // 薪资项目管理
  getSalaryItemPage(params: SalaryItemQueryRequest): Promise<Result<PageResult<SalaryItemVO>>> {
    return request.get('/salary/item/page', { params })
  },
  getSalaryItemById(id: number): Promise<Result<SalaryItemVO>> {
    return request.get(`/salary/item/${id}`)
  },
  createSalaryItem(data: SalaryItemCreateRequest): Promise<Result<void>> {
    return request.post('/salary/item', data)
  },
  updateSalaryItem(id: number, data: SalaryItemUpdateRequest): Promise<Result<void>> {
    return request.put(`/salary/item/${id}`, data)
  },
  deleteSalaryItem(id: number): Promise<Result<void>> {
    return request.delete(`/salary/item/${id}`)
  },

  // 薪资表管理
  getSalarySheetPage(params: SalarySheetQueryRequest): Promise<Result<PageResult<SalarySheetVO>>> {
    return request.get('/salary/sheet/page', { params })
  },
  getSalarySheetById(id: number): Promise<Result<SalarySheetVO>> {
    return request.get(`/salary/sheet/${id}`)
  },
  createSalarySheet(data: SalarySheetCreateRequest): Promise<Result<void>> {
    return request.post('/salary/sheet', data)
  },
  updateSalarySheet(id: number, data: SalarySheetUpdateRequest): Promise<Result<void>> {
    return request.put(`/salary/sheet/${id}`, data)
  },
  deleteSalarySheet(id: number): Promise<Result<void>> {
    return request.delete(`/salary/sheet/${id}`)
  },
  confirmSalarySheet(id: number): Promise<Result<void>> {
    return request.post(`/salary/sheet/${id}/confirm`)
  },
  paySalarySheet(id: number): Promise<Result<void>> {
    return request.post(`/salary/sheet/${id}/pay`)
  },

  // 薪资计算
  calculateSalary(data: SalaryCalculateRequest): Promise<Result<void>> {
    return request.post('/salary/calculate', data)
  },
  generateSalaryVoucher(data: SalaryVoucherGenerateRequest): Promise<Result<void>> {
    return request.post('/salary/voucher/generate', data)
  }
}
