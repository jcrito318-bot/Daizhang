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
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'RoleList',
        component: () => import('@/views/system/role/RoleList.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/log',
        name: 'OperationLogList',
        component: () => import('@/views/system/log/OperationLogList.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'system/setting',
        name: 'SystemSetting',
        component: () => import('@/views/system/setting/SystemSetting.vue'),
        meta: { title: '系统设置' }
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

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next({ path: '/dashboard' })
  } else {
    next()
  }
})

export default router
