import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { defineComponent, h } from 'vue'

// el-table-column 在 jsdom 下渲染 scoped slot 时不会传入 { row } scope,
// 导致 <template #default="{ row }"> 解构失败。这里 stub 让其以 { row: {} } 调用 slot。
const TableColumnStub = defineComponent({
  name: 'ElTableColumn',
  setup(_, { slots }) {
    return () => h('div', { class: 'stub-col' }, slots.default?.({ row: {} }))
  }
})

const mountOptions = { global: { stubs: { ElTableColumn: TableColumnStub } } }

// mock element-plus
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn(),
      alert: vi.fn()
    }
  }
})

// mock salaryApi
vi.mock('@/api/salary', () => ({
  salaryApi: {
    getSalarySheetPage: vi.fn(),
    confirmSalarySheet: vi.fn(),
    deleteSalarySheet: vi.fn()
  }
}))

import SalarySheetList from '@/views/salary/SalarySheetList.vue'
import { salaryApi } from '@/api/salary'
import { useAppStore } from '@/stores/app'

/**
 * SalarySheetList.vue 测试 (BUG-05 修复)
 *
 * 修复点:之前 loadData 未传 accountSetId 参数,后端 mybatis-plus eq(null) 会忽略该条件,
 * 可能返回所有账套数据(IDOR 风险)。
 *
 * 修复后:
 *   - loadData 检查 appStore.currentAccountSetId,未选账套时不发请求并清空 tableData
 *   - 调用 salaryApi.getSalarySheetPage 时显式传入 accountSetId 参数
 *
 * 本测试聚焦验证修复点(避免重复测试 UI 渲染,聚焦安全修复行为):
 * 1. 未选账套(currentAccountSetId=null)时 loadData 不调 API 且清空 tableData
 * 2. 已选账套时调 API 且第一个参数携带正确的 accountSetId
 * 3. 即使 store 中 currentAccountSetId 为 0/falsy 也不发请求(fail-closed)
 */
describe('SalarySheetList.vue - BUG-05 修复:accountSetId 隔离', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('未选账套(currentAccountSetId=null)时 loadData 应直接返回,不调 API 且清空 tableData', async () => {
    const wrapper = mount(SalarySheetList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    // 当前账套为 null
    const appStore = useAppStore()
    appStore.currentAccountSetId = null
    vm.tableData = [{ id: 1 }] // 模拟历史残留数据
    vm.pagination.total = 1

    await vm.loadData()
    await flushPromises()

    expect(salaryApi.getSalarySheetPage).not.toHaveBeenCalled()
    expect(vm.tableData).toEqual([])
    expect(vm.pagination.total).toBe(0)
  })

  it('已选账套时应调 API 且第一个参数携带 accountSetId', async () => {
    ;(salaryApi.getSalarySheetPage as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: [{ id: 1, year: 2026, month: 7, employeeName: '张三', baseSalary: 5000, netSalary: 4500, status: 0 }], total: 1, pageNum: 1, pageSize: 10, pages: 1 },
      timestamp: 0
    })

    const wrapper = mount(SalarySheetList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    const appStore = useAppStore()
    appStore.currentAccountSetId = 42

    await vm.loadData()
    await flushPromises()

    expect(salaryApi.getSalarySheetPage).toHaveBeenCalledTimes(1)
    const args = (salaryApi.getSalarySheetPage as any).mock.calls[0][0]
    expect(args.accountSetId).toBe(42)
  })

  it('currentAccountSetId=0(falsy)时也应 fail-closed 不发请求', async () => {
    const wrapper = mount(SalarySheetList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    const appStore = useAppStore()
    appStore.currentAccountSetId = 0

    await vm.loadData()
    await flushPromises()

    expect(salaryApi.getSalarySheetPage).not.toHaveBeenCalled()
    expect(vm.tableData).toEqual([])
  })
})
