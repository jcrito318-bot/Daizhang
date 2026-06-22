export interface EmployeeVO {
  id: number
  accountSetId: number
  employeeCode: string
  employeeName: string
  department: string
  position: string
  idCard: string
  phone: string
  entryDate: string
  status: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  createByName: string
}

export interface EmployeeCreateRequest {
  accountSetId: number
  employeeCode: string
  employeeName: string
  department?: string
  position?: string
  idCard?: string
  phone?: string
  entryDate?: string
  status?: number
  remark?: string
}

export interface EmployeeUpdateRequest {
  employeeCode?: string
  employeeName?: string
  department?: string
  position?: string
  idCard?: string
  phone?: string
  entryDate?: string
  status?: number
  remark?: string
}

export interface EmployeeQueryRequest {
  accountSetId?: number
  employeeCode?: string
  employeeName?: string
  department?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SalaryItemVO {
  id: number
  accountSetId: number
  itemName: string
  itemCode: string
  itemType: string
  calculationMethod: string
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  createByName: string
}

export interface SalaryItemCreateRequest {
  accountSetId: number
  itemName: string
  itemCode: string
  itemType: string
  calculationMethod?: string
  remark?: string
}

export interface SalaryItemUpdateRequest {
  itemName?: string
  itemCode?: string
  itemType?: string
  calculationMethod?: string
  remark?: string
}

export interface SalaryItemQueryRequest {
  accountSetId?: number
  itemName?: string
  itemCode?: string
  itemType?: string
  pageNum?: number
  pageSize?: number
}

export interface SalarySheetVO {
  id: number
  accountSetId: number
  year: number
  month: number
  employeeId: number
  employeeName: string
  baseSalary: number
  allowance: number
  bonus: number
  deduction: number
  socialSecurity: number
  housingFund: number
  taxableIncome: number
  incomeTax: number
  netSalary: number
  status: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  createByName: string
}

export interface SalarySheetCreateRequest {
  accountSetId: number
  year: number
  month: number
  employeeId: number
  baseSalary?: number
  allowance?: number
  bonus?: number
  deduction?: number
  socialSecurity?: number
  housingFund?: number
  remark?: string
}

export interface SalarySheetUpdateRequest {
  baseSalary?: number
  allowance?: number
  bonus?: number
  deduction?: number
  socialSecurity?: number
  housingFund?: number
  remark?: string
}

export interface SalarySheetQueryRequest {
  accountSetId?: number
  year?: number
  month?: number
  employeeId?: number
  employeeName?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SalaryCalculateRequest {
  accountSetId: number
  year: number
  month: number
  threshold?: number
}

export interface SalaryVoucherGenerateRequest {
  accountSetId: number
  year: number
  month: number
  payableSubjectId: number
  bankSubjectId: number
  socialSecuritySubjectId?: number
  housingFundSubjectId?: number
  incomeTaxSubjectId?: number
}
