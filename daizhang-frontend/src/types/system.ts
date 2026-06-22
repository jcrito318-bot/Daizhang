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
