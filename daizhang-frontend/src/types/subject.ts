export interface SubjectVO {
  id: number
  accountSetId: number
  subjectCode: string
  subjectName: string
  category: string
  parentId: number
  level: number
  subjectType: number
  balanceDirection: number
  auxiliaryAccounting: number
  isCash: number
  isBank: number
  isCurrent: number
  status: number
  createTime: string
  children?: SubjectVO[]
}

export interface SubjectCreateRequest {
  accountSetId: number
  subjectCode: string
  subjectName: string
  category?: string
  parentId?: number
  level?: number
  subjectType?: number
  balanceDirection?: number
  auxiliaryAccounting?: number
  isCash?: number
  isBank?: number
  isCurrent?: number
}

export interface SubjectUpdateRequest {
  subjectName?: string
  category?: string
  subjectType?: number
  balanceDirection?: number
  auxiliaryAccounting?: number
  isCash?: number
  isBank?: number
  isCurrent?: number
  status?: number
}
