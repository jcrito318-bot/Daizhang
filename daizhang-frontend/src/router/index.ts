import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由 meta.roles 约定:未设置表示所有登录用户可访问;设置后只允许列出的角色(ADMIN 隐式通过)
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
        path: 'voucher/template',
        name: 'VoucherTemplate',
        component: () => import('@/views/voucher/VoucherTemplateList.vue'),
        meta: { title: '凭证模板', icon: 'Document', roles: ['ADMIN', 'ACCOUNTANT'] }
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
        path: 'report/cash-flow',
        name: 'CashFlowStatement',
        component: () => import('@/views/report/CashFlowStatement.vue'),
        meta: { title: '现金流量表' }
      },
      {
        path: 'report/aging',
        name: 'AgingAnalysis',
        component: () => import('@/views/report/AgingAnalysis.vue'),
        meta: { title: '账龄分析', icon: 'Timer' }
      },
      {
        path: 'system/user',
        name: 'UserList',
        component: () => import('@/views/system/user/UserList.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'system/role',
        name: 'RoleList',
        component: () => import('@/views/system/role/RoleList.vue'),
        meta: { title: '角色管理', roles: ['ADMIN'] }
      },
      {
        path: 'system/log',
        name: 'OperationLogList',
        component: () => import('@/views/system/log/OperationLogList.vue'),
        meta: { title: '操作日志', roles: ['ADMIN'] }
      },
      {
        path: 'system/setting',
        name: 'SystemSetting',
        component: () => import('@/views/system/setting/SystemSetting.vue'),
        meta: { title: '系统设置', roles: ['ADMIN'] }
      },
      {
        // P4.2: 安全设置(双因素认证管理),所有登录用户均可访问以管理自身账户安全
        path: 'system/security',
        name: 'SecuritySettings',
        component: () => import('@/views/system/SecuritySettings.vue'),
        meta: { title: '安全设置' }
      },
      {
        path: 'period',
        name: 'PeriodManage',
        component: () => import('@/views/period/PeriodManage.vue'),
        meta: { title: '会计期间管理' }
      },
      {
        path: 'period/close-wizard',
        name: 'PeriodCloseWizard',
        component: () => import('@/views/period/PeriodCloseWizard.vue'),
        meta: { title: '期末结账向导', icon: 'MagicStick', roles: ['ADMIN', 'ACCOUNTANT'] }
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
        path: 'tax/warning',
        name: 'TaxWarning',
        component: () => import('@/views/tax/TaxWarningDashboard.vue'),
        meta: { title: '税负预警', icon: 'WarningFilled' }
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
        path: 'bank/smart-reconciliation',
        name: 'SmartReconciliation',
        component: () => import('@/views/bank/SmartReconciliation.vue'),
        meta: { title: '智能对账', icon: 'Connection' }
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
      },
      {
        path: 'batch',
        name: 'BatchOperation',
        component: () => import('@/views/batch/BatchOperation.vue'),
        meta: { title: '批量操作', icon: 'Operation', roles: ['ADMIN', 'ACCOUNTANT'] }
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

// 首次导航时静默初始化认证:若存在 refresh token 则换取新的 access token
let authInitialized = false

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  if (!authInitialized) {
    authInitialized = true
    await userStore.initializeAuth()
  }
  const loggedIn = userStore.isLoggedIn
  if (to.meta.requiresAuth && !loggedIn) {
    // 防 Open Redirect：拒绝以 // 开头的路径（会被浏览器解析为协议相对 URL 外跳）
    const redirectPath = to.fullPath.startsWith('//') ? '/dashboard' : to.fullPath
    next({ path: '/login', query: { redirect: redirectPath } })
    return
  }
  if (to.path === '/login' && loggedIn) {
    next({ path: '/dashboard' })
    return
  }
  // 角色级访问控制:meta.roles 限定可访问的角色,ADMIN 隐式放行
  if (to.meta.roles && Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
    if (!userStore.hasAnyRole(to.meta.roles as string[])) {
      next({ path: '/dashboard' })
      return
    }
  }
  next()
})

export default router
