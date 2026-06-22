import request from '@/utils/request'
import type { Result, PageResult } from '@/types/common'
import type { SysUserVO, UserCreateRequest, UserUpdateRequest, SysRoleVO, RoleCreateRequest, SysMenuVO, SysOperationLogVO, DashboardStatsVO } from '@/types/system'

export const userApi = {
  page(params: { username?: string; realName?: string; status?: number; pageNum: number; pageSize: number }): Promise<Result<PageResult<SysUserVO>>> {
    return request.get('/system/user/page', { params })
  },
  create(data: UserCreateRequest): Promise<Result<void>> {
    return request.post('/system/user', data)
  },
  update(id: number, data: UserUpdateRequest): Promise<Result<void>> {
    return request.put(`/system/user/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/system/user/${id}`)
  },
  resetPassword(id: number, newPassword: string): Promise<Result<void>> {
    return request.post(`/system/user/${id}/reset-password`, { newPassword })
  },
  updateStatus(id: number, status: number): Promise<Result<void>> {
    return request.put(`/system/user/${id}/status`, { status })
  },
  assignRoles(id: number, roleIds: number[]): Promise<Result<void>> {
    return request.post(`/system/user/${id}/roles`, { roleIds })
  }
}

export const roleApi = {
  list(): Promise<Result<SysRoleVO[]>> {
    return request.get('/system/role/list')
  },
  page(params: { pageNum: number; pageSize: number }): Promise<Result<PageResult<SysRoleVO>>> {
    return request.get('/system/role/page', { params })
  },
  create(data: RoleCreateRequest): Promise<Result<void>> {
    return request.post('/system/role', data)
  },
  update(id: number, data: Partial<RoleCreateRequest>): Promise<Result<void>> {
    return request.put(`/system/role/${id}`, data)
  },
  delete(id: number): Promise<Result<void>> {
    return request.delete(`/system/role/${id}`)
  },
  assignMenus(id: number, menuIds: number[]): Promise<Result<void>> {
    return request.post(`/system/role/${id}/menus`, { menuIds })
  },
  getMenuIds(id: number): Promise<Result<number[]>> {
    return request.get(`/system/role/${id}/menus`)
  }
}

export const menuApi = {
  tree(): Promise<Result<SysMenuVO[]>> {
    return request.get('/system/menu/tree')
  }
}

export const logApi = {
  page(params: { username?: string; operation?: string; startDate?: string; endDate?: string; pageNum: number; pageSize: number }): Promise<Result<PageResult<SysOperationLogVO>>> {
    return request.get('/system/log/page', { params })
  }
}

export const dashboardApi = {
  stats(accountSetId?: number): Promise<Result<DashboardStatsVO>> {
    return request.get('/dashboard/stats', { params: { accountSetId } })
  }
}
