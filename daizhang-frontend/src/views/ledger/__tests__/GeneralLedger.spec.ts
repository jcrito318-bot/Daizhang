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
    }
  }
})

// mock ledgerApi:控制 getGeneralLedger 返回数据形状(后端返回 List,非 PageResult)
vi.mock('@/api/ledger', () => ({
  ledgerApi: {
    getGeneralLedger: vi.fn(),
    getDetailLedger: vi.fn(),
    getSubjectBalance: vi.fn(),
    getAuxiliaryBalance: vi.fn(),
    getCashJournal: vi.fn()
  }
}))

// mock subjectApi
vi.mock('@/api/subject', () => ({
  subjectApi: {
    getTree: vi.fn()
  }
}))

import GeneralLedger from '@/views/ledger/GeneralLedger.vue'
import { ledgerApi } from '@/api/ledger'

/**
 * GeneralLedger.vue 测试 (BUG-04 修复)
 *
 * 修复点:后端 /ledger/general 返回 Result<List<GeneralLedgerVO>>(全量数组),
 * 之前前端用 res.data.length 伪造 total,但 tableData 直接赋值全量数组导致分页失效。
 *
 * 修复后:
 *   - allData 存储后端返回的全量数据
 *   - tableData 改为 computed,基于 queryForm.pageNum / pageSize 切片
 *   - total 改为 computed,取 allData.length
 *
 * 本测试聚焦验证:
 * 1. 后端返回 25 条数据时,tableData 在 pageSize=20 下只展示前 20 条
 * 2. 切换到第 2 页时,tableData 展示第 21-25 条
 * 3. total 等于全量数据长度(而非伪造的某个数)
 * 4. 修改 pageSize 后切片随之更新
 */
describe('GeneralLedger.vue - BUG-04 修复:客户端分页切片', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  // 生成 N 条假数据
  function genRecords(n: number) {
    return Array.from({ length: n }, (_, i) => ({
      subjectCode: String(1001 + i).padStart(4, '0'),
      subjectName: `科目${i + 1}`,
      beginDebit: 1000 + i,
      beginCredit: 0,
      periodDebit: 100,
      periodCredit: 0,
      endDebit: 1100 + i,
      endCredit: 0
    }))
  }

  function mockLedgerReturn(records: any[]) {
    ;(ledgerApi.getGeneralLedger as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      // 注意:后端返回 List(直接数组),而非 PageResult
      data: records,
      timestamp: 0
    })
  }

  it('后端返回 25 条时,total 应为 25,tableData 在 pageSize=20 下只展示前 20 条', async () => {
    mockLedgerReturn(genRecords(25))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    // 手动触发 loadData(组件 onMounted 内会尝试加载账套列表,这里直接调 loadData 走接口)
    vm.queryForm.accountSetId = 1
    await vm.loadData()
    await flushPromises()

    expect(vm.total).toBe(25)
    expect(vm.tableData.length).toBe(20)
    // 第 1 页应是前 20 条
    expect(vm.tableData[0].subjectCode).toBe('1001')
    expect(vm.tableData[19].subjectCode).toBe('1020')
  })

  it('切换到第 2 页时,tableData 应展示第 21-25 条(共 5 条)', async () => {
    mockLedgerReturn(genRecords(25))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.queryForm.accountSetId = 1
    await vm.loadData()
    await flushPromises()

    // 切到第 2 页
    vm.queryForm.pageNum = 2
    await flushPromises()

    expect(vm.tableData.length).toBe(5)
    expect(vm.tableData[0].subjectCode).toBe('1021')
    expect(vm.tableData[4].subjectCode).toBe('1025')
  })

  it('total 应等于 allData.length(反映后端全量数据,而非伪造)', async () => {
    mockLedgerReturn(genRecords(37))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.queryForm.accountSetId = 1
    await vm.loadData()
    await flushPromises()

    expect(vm.total).toBe(37)
    expect(vm.allData.length).toBe(37)
    expect(vm.total).toBe(vm.allData.length)
  })

  it('修改 pageSize 应重新切片(45 条数据,pageSize=10 → 第 1 页 10 条)', async () => {
    mockLedgerReturn(genRecords(45))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.queryForm.accountSetId = 1
    vm.queryForm.pageSize = 10
    vm.queryForm.pageNum = 1
    await vm.loadData()
    await flushPromises()

    expect(vm.tableData.length).toBe(10)
    expect(vm.total).toBe(45)
  })

  it('未选账套(accountSetId=0)时 loadData 应直接返回,不调用 API', async () => {
    mockLedgerReturn(genRecords(10))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.queryForm.accountSetId = 0
    ;(ledgerApi.getGeneralLedger as any).mockClear()
    await vm.loadData()
    await flushPromises()

    expect(ledgerApi.getGeneralLedger).not.toHaveBeenCalled()
  })

  it('数据减少导致当前页超出范围时,应回到第 1 页', async () => {
    // 模拟从较多数据(第 2 页)切到较少数据
    mockLedgerReturn(genRecords(25))
    const wrapper = mount(GeneralLedger, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.queryForm.accountSetId = 1
    vm.queryForm.pageSize = 20
    vm.queryForm.pageNum = 2
    await vm.loadData()
    await flushPromises()
    // 第 2 页有 5 条
    expect(vm.tableData.length).toBe(5)

    // 数据减少到 5 条(第 2 页起点 20 >= 5,应回到第 1 页)
    mockLedgerReturn(genRecords(5))
    await vm.loadData()
    await flushPromises()

    expect(vm.queryForm.pageNum).toBe(1)
    expect(vm.tableData.length).toBe(5)
  })
})
