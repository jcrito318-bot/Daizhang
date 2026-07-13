import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'accountset',
        name: 'AccountSetList',
        component: () => import('@/views/accountset/AccountSetList.vue'),
        meta: { title: '账套管理' }
      },
      {
        path: 'subject',
        name: 'SubjectList',
        component: () => import('@/views/subject/SubjectList.vue'),
        meta: { title: '科目管理' }
      },
      {
        path: 'voucher',
        name: 'VoucherList',
        component: () => import('@/views/voucher/VoucherList.vue'),
        meta: { title: '凭证管理' }
      },
      {
        path: 'voucher/create',
        name: 'VoucherCreate',
        component: () => import('@/views/voucher/VoucherCreate.vue'),
        meta: { title: '新增凭证' }
      },
      {
        // 编辑凭证:复用 VoucherCreate 组件(其内部通过 route.params.id 进入编辑模式)。
        // 原 voucher/:id 仅绑定到 VoucherDetail(只读),导致未审核凭证无法修改——
        // 后端 voucherApi.update 与前端编辑表单均已实现,仅缺路由入口。
        path: 'voucher/:id/edit',
        name: 'VoucherEdit',
        component: () => import('@/views/voucher/VoucherCreate.vue'),
        meta: { title: '编辑凭证' }
      },
      {
        path: 'voucher/:id',
        name: 'VoucherDetail',
        component: () => import('@/views/voucher/VoucherDetail.vue'),
        meta: { title: '凭证详情' }
      },
      {
        path: 'ledger/detail',
        name: 'DetailLedger',
        component: () => import('@/views/ledger/DetailLedger.vue'),
        meta: { title: '明细账' }
      },
      {
        path: 'ledger/general',
        name: 'GeneralLedger',
        component: () => import('@/views/ledger/GeneralLedger.vue'),
        meta: { title: '总账' }
      },
      {
        path: 'ledger/subject-balance',
        name: 'SubjectBalance',
        component: () => import('@/views/ledger/SubjectBalance.vue'),
        meta: { title: '科目余额表' }
      },
      {
        path: 'report/balance-sheet',
        name: 'BalanceSheet',
        component: () => import('@/views/report/BalanceSheet.vue'),
        meta: { title: '资产负债表' }
      },
      {
        path: 'report/income-statement',
        name: 'IncomeStatement',
        component: () => import('@/views/report/IncomeStatement.vue'),
        meta: { title: '利润表' }
      },
      {
        path: 'system/user',
        name: 'UserList',
        component: () => import('@/views/system/user/UserList.vue'),
        meta: { title: '用户管理', roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'system/role',
        name: 'RoleList',
        component: () => import('@/views/system/role/RoleList.vue'),
        meta: { title: '角色管理', roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'system/log',
        name: 'OperationLogList',
        component: () => import('@/views/system/log/OperationLogList.vue'),
        meta: { title: '操作日志', roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'system/setting',
        name: 'SystemSetting',
        component: () => import('@/views/system/setting/SystemSetting.vue'),
        meta: { title: '系统设置', roles: ['ROLE_ADMIN'] }
      },
      {
        path: 'period',
        name: 'PeriodManage',
        component: () => import('@/views/period/PeriodManage.vue'),
        meta: { title: '会计期间管理' }
      },
      {
        path: 'tax/declaration',
        name: 'TaxDeclarationList',
        component: () => import('@/views/tax/TaxDeclarationList.vue'),
        meta: { title: '税务申报' }
      },
      {
        path: 'tax/calculation',
        name: 'TaxCalculation',
        component: () => import('@/views/tax/TaxCalculation.vue'),
        meta: { title: '税务计算' }
      },
      {
        path: 'document',
        name: 'DocumentList',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '票据管理' }
      },
      {
        path: 'document/:id',
        name: 'DocumentDetail',
        component: () => import('@/views/document/DocumentDetail.vue'),
        meta: { title: '票据详情' }
      },
      {
        path: 'bank/transaction',
        name: 'BankTransactionList',
        component: () => import('@/views/bank/BankTransactionList.vue'),
        meta: { title: '银行流水' }
      },
      {
        path: 'bank/reconciliation',
        name: 'BankReconciliation',
        component: () => import('@/views/bank/BankReconciliation.vue'),
        meta: { title: '银行对账' }
      },
      {
        path: 'bank/balance-adjustment',
        name: 'BankBalanceAdjustment',
        component: () => import('@/views/bank/BankBalanceAdjustment.vue'),
        meta: { title: '余额调节表' }
      },
      {
        path: 'salary/employee',
        name: 'EmployeeList',
        component: () => import('@/views/salary/EmployeeList.vue'),
        meta: { title: '员工管理' }
      },
      {
        path: 'salary/sheet',
        name: 'SalarySheetList',
        component: () => import('@/views/salary/SalarySheetList.vue'),
        meta: { title: '工资表' }
      },
      {
        path: 'salary/calculation',
        name: 'SalaryCalculation',
        component: () => import('@/views/salary/SalaryCalculation.vue'),
        meta: { title: '工资计算' }
      },
      {
        path: 'asset/category',
        name: 'AssetCategoryList',
        component: () => import('@/views/asset/AssetCategoryList.vue'),
        meta: { title: '资产分类' }
      },
      {
        path: 'asset/list',
        name: 'AssetList',
        component: () => import('@/views/asset/AssetList.vue'),
        meta: { title: '资产列表' }
      },
      {
        path: 'asset/depreciation',
        name: 'AssetDepreciation',
        component: () => import('@/views/asset/AssetDepreciation.vue'),
        meta: { title: '资产折旧' }
      },
      {
        path: 'customer/list',
        name: 'CustomerList',
        component: () => import('@/views/customer/CustomerList.vue'),
        meta: { title: '客户列表' }
      },
      {
        path: 'customer/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/customer/CustomerDetail.vue'),
        meta: { title: '客户详情' }
      }
    ]
  },
  {
    // 404 兜底:未知路由显示NotFound页,避免白屏
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.path === '/login' && token) {
    next({ path: '/dashboard' })
    return
  }
  // RBAC:路由级角色校验。meta.roles 指定允许访问的角色列表,
  // 用户须拥有其中至少一个角色才能进入,否则重定向到首页。
  // 典型场景:普通记账员不可访问系统管理(用户/角色/日志/设置)页面。
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const { useUserStore } = await import('@/stores/user')
    const userStore = useUserStore()
    // userInfo 可能在页面刷新后为空,先尝试加载
    if (!userStore.userInfo) {
      try {
        await userStore.getUserInfo()
      } catch {
        // 加载失败(如token过期),跳转登录
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }
    }
    const userRoles = userStore.userInfo?.roles ?? []
    const hasRole = requiredRoles.some(r => userRoles.includes(r))
    if (!hasRole) {
      next({ path: '/dashboard' })
      return
    }
  }
  next()
})

export default router
