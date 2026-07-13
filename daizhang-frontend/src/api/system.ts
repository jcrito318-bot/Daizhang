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
    // 密码走请求体,避免明文出现在 URL/浏览器历史/网关日志
    return request.put(`/system/user/${id}/password`, { newPassword })
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

export const dashboardApi = {
  // 后端端点为 GET /dashboard(返回 DashboardVO,含 summary 嵌套对象),
  // 前端 Dashboard 期望扁平的 DashboardStatsVO,此处做字段映射适配
  async stats(accountSetId?: number): Promise<Result<DashboardStatsVO>> {
    // 将 accountSetId 作为查询参数传递给后端,后端会在可访问范围内进一步限定到该账套,
    // 避免账套过滤被静默丢弃导致首页统计跨账套混算
    const res = await request.get('/dashboard', { params: { accountSetId: accountSetId ?? undefined } })
    const vo = res.data as any
    const summary = vo?.summary || {}
    // 映射后端 DashboardSummary 字段到前端 DashboardStatsVO
    // monthVoucherCount(本月凭证总数) 与 pendingAuditCount(待审核数) 必须区分,
    // 否则首页两块卡片会显示相同数字
    res.data = {
      accountSetCount: summary.totalAccountSets ?? 0,
      monthVoucherCount: summary.monthVoucherCount ?? 0,
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
