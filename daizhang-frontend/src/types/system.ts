export interface SysUserVO {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  status: number
  createTime: string
  roles: string[]
}

export interface UserCreateRequest {
  username: string
  password: string
  realName?: string
  phone?: string
  email?: string
  status?: number
}

export interface UserUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  status?: number
}

export interface SysRoleVO {
  id: number
  roleName: string
  roleCode: string
  description: string
  status: number
  createTime: string
  menuIds?: number[]
}

export interface RoleCreateRequest {
  roleName: string
  roleCode: string
  description?: string
  status?: number
}

export interface SysMenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sortOrder: number
  menuType: number
  permission: string
  visible: number
  status: number
  children?: SysMenuVO[]
}

export interface SysOperationLogVO {
  id: number
  userId: number
  username: string
  operation: string
  method: string
  params: string
  ip: string
  status: number
  errorMsg: string
  createTime: string
  // P3.4 扩展字段
  /** 客户端 User-Agent */
  userAgent?: string
  /** 操作前值(JSON) */
  beforeValue?: string
  /** 操作后值(JSON) */
  afterValue?: string
  /** 请求路径 */
  requestPath?: string
  /** HTTP 方法 */
  requestMethod?: string
}

export interface DashboardStatsVO {
  accountSetCount: number
  monthVoucherCount: number
  pendingAuditCount: number
  pendingTaxCount: number
  totalAssets: number
  totalRevenue: number
  totalProfit: number
  cashBalance: number
}

/**
 * 备份记录视图对象 (P3.3)
 */
export interface BackupRecordVO {
  id: number
  /** 备份文件名 */
  fileName: string
  /** 文件大小(字节) */
  fileSize: number
  /** 创建时间 */
  createdAt: string
  /** 创建人ID */
  createdBy: number | null
  /** 创建人名称 */
  createdByName: string | null
  /** 触发方式: manual(手动) / auto(自动) */
  type: string
  /** 备份状态: success / failed / in_progress */
  status: string
  /** 备份类型: full / incremental */
  backupType: string
  /** 备注 */
  remark: string | null
}
