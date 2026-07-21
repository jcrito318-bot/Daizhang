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
    return request.put(`/system/user/${id}/password`, null, { params: { newPassword } })
  },
  updateStatus(id: number, status: number): Promise<Result<void>> {
    return request.put(`/system/user/${id}/status`, null, { params: { status } })
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
    return request.put(`/system/role/${id}/menus`, { menuIds })
  },
  getMenuIds(id: number): Promise<Result<number[]>> {
    return request.get(`/system/role/${id}/menu-ids`)
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

// 系统配置 API:对应后端 SysConfigController(/system/config),
// 用于 SystemSetting 页面的公司信息/系统参数持久化。
// 设计为 key-value 形式:每项配置以 (configKey, configValue) 存储,更新时按 key 查找。
export const settingApi = {
  getValue(key: string): Promise<Result<string>> {
    return request.get('/system/config/value', { params: { key } })
  },
  create(data: { configKey: string; configName: string; configValue: string; remark?: string }): Promise<Result<void>> {
    return request.post('/system/config', data)
  },
  update(id: number, data: { configKey: string; configName: string; configValue: string; remark?: string }): Promise<Result<void>> {
    return request.put(`/system/config/${id}`, data)
  },
  page(params: { configKey?: string; configName?: string; pageNum: number; pageSize: number }): Promise<Result<PageResult<{ id: number; configKey: string; configName: string; configValue: string; remark?: string }>>> {
    return request.get('/system/config/page', { params })
  }
}

export const dashboardApi = {
  // 后端端点为 GET /dashboard(返回 DashboardVO,含 summary 嵌套对象),
  // 前端 Dashboard 期望扁平的 DashboardStatsVO,此处做字段映射适配。
  // BF-13 修复:后端 DashboardController.getDashboard 是代账公司全局运营看板,
  // 跨账套汇总(代账公司需看自己所有账套的待办/统计),不接收 accountSetId 参数。
  // 原函数签名声明了 accountSetId 参数但完全忽略它,误导调用方以为做了账套隔离,
  // 此处移除误导的参数。
  async stats(): Promise<Result<DashboardStatsVO>> {
    const res = await request.get('/dashboard')
    const vo = res.data as any
    const summary = vo?.summary || {}
    // 映射后端 DashboardSummary 字段到前端 DashboardStatsVO
    res.data = {
      accountSetCount: summary.totalAccountSets ?? 0,
      monthVoucherCount: summary.unauditedVoucherCount ?? 0,
      pendingAuditCount: summary.unauditedVoucherCount ?? 0,
      pendingTaxCount: summary.undeclaredTaxCount ?? 0,
      totalAssets: 0,
      totalRevenue: 0,
      totalProfit: 0,
      cashBalance: 0
    } as DashboardStatsVO
    return res as unknown as Result<DashboardStatsVO>
  }
}
