import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { defineComponent, h, nextTick, ref } from 'vue'

// el-autocomplete 的 #default="{ item }" 作用域插槽在 jsdom 下解构失败(slot 未传入 scope)。
// stub 让其以 { item: {} } 调用 slot,避免 TypeError。
const AutocompleteStub = defineComponent({
  name: 'ElAutocomplete',
  inheritAttrs: false,
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup(_, { slots }) {
    return () => h('div', { class: 'stub-autocomplete' }, slots.default?.({ item: {} }))
  }
})

// el-table-column 的 #default="{ row }" 作用域插槽在 jsdom 下同样会解构失败。
const TableColumnStub = defineComponent({
  name: 'ElTableColumn',
  setup(_, { slots }) {
    return () => h('div', { class: 'stub-col' }, slots.default?.({ row: {} }))
  }
})

// el-form 的 validate 方法在 jsdom 下不存在,stub 提供模拟实现(总是返回 valid=true)。
// 使用 Options API 的 expose + methods 使 formRef.value.validate 可调用。
const FormStub = defineComponent({
  name: 'ElForm',
  expose: ['validate'],
  methods: {
    validate(cb: (valid: boolean) => void) {
      cb(true)
      return Promise.resolve(true)
    }
  },
  render() {
    return h('form', { class: 'stub-form' }, (this.$slots as any).default?.())
  }
})

const mountOptions = {
  global: {
    stubs: {
      ElAutocomplete: AutocompleteStub,
      ElTableColumn: TableColumnStub,
      ElForm: FormStub
    }
  }
}

// mock vue-router:提供 useRoute / useRouter / onBeforeRouteLeave
const mockRoute = ref({ params: {} as Record<string, string>, query: {} as Record<string, string> })
const mockRouterPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => mockRoute.value,
  useRouter: () => ({ push: mockRouterPush }),
  onBeforeRouteLeave: vi.fn()
}))

// mock element-plus(ElMessage / ElMessageBox)
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

// mock voucherApi
vi.mock('@/api/voucher', () => ({
  voucherApi: {
    getWordList: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn()
  },
  templateApi: {
    getList: vi.fn(),
    apply: vi.fn()
  },
  abstractApi: {
    search: vi.fn(),
    incrementUse: vi.fn()
  }
}))

// mock subjectApi
vi.mock('@/api/subject', () => ({
  subjectApi: {
    getTree: vi.fn()
  }
}))

import VoucherCreate from '@/views/voucher/VoucherCreate.vue'
import { voucherApi } from '@/api/voucher'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

/**
 * VoucherCreate.vue 组件测试
 *
 * 覆盖核心功能:
 * 1. 初始渲染:默认有一行空分录,显示 11 位金额格表头
 * 2. 金额输入:借方/贷方互斥,输入框 → 位格解析
 * 3. 科目选择:总账科目联动明细科目
 * 4. 增删行:增加一行 / 删除行(至少保留一行)
 * 5. 提交校验:空表单、缺摘要、借贷不平衡
 * 6. 编辑模式:加载已有凭证并反查科目
 */
describe('VoucherCreate.vue - 凭证录入组件', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  // ==================== 辅助函数 ====================

  const mockSubjectTree = [
    {
      id: 1, accountSetId: 1, subjectCode: '1001', subjectName: '库存现金',
      category: '资产', parentId: 0, level: 1, subjectType: 1, balanceDirection: 1,
      auxiliaryAccounting: 0, isCash: 1, isBank: 0, isCurrent: 1, status: 1,
      createTime: '', children: [
        {
          id: 11, accountSetId: 1, subjectCode: '1001.01', subjectName: '人民币',
          category: '资产', parentId: 1, level: 2, subjectType: 1, balanceDirection: 1,
          auxiliaryAccounting: 0, isCash: 1, isBank: 0, isCurrent: 1, status: 1,
          createTime: '', children: []
        }
      ]
    },
    {
      id: 2, accountSetId: 1, subjectCode: '1002', subjectName: '银行存款',
      category: '资产', parentId: 0, level: 1, subjectType: 1, balanceDirection: 1,
      auxiliaryAccounting: 0, isCash: 0, isBank: 1, isCurrent: 1, status: 1,
      createTime: '', children: []
    }
  ]

  const mockVoucherWords = [
    { id: 1, accountSetId: 1, name: '记', code: 'JZ', sortOrder: 1, status: 1 }
  ]

  async function mountComponent(routeParams: Record<string, string> = {}, routeQuery: Record<string, string> = {}) {
    vi.mocked(subjectApi.getTree).mockResolvedValue({ code: 200, message: 'ok', data: mockSubjectTree } as any)
    vi.mocked(voucherApi.getWordList).mockResolvedValue({ code: 200, message: 'ok', data: mockVoucherWords } as any)
    vi.mocked(voucherApi.create).mockResolvedValue({ code: 200, message: 'ok', data: { id: 1 } } as any)
    vi.mocked(voucherApi.update).mockResolvedValue({ code: 200, message: 'ok', data: {} } as any)

    // 设置当前账套 ID(否则 onMounted 中 accountSetId=0 不会加载科目树)
    const appStore = useAppStore()
    appStore.currentAccountSetId = 1

    // 设置 mock route 参数
    mockRoute.value = { params: routeParams, query: routeQuery }

    const wrapper = mount(VoucherCreate, mountOptions)
    await flushPromises()
    return wrapper
  }

  async function mountSimple() {
    const appStore = useAppStore()
    appStore.currentAccountSetId = 1
    mockRoute.value = { params: {}, query: {} }
    const wrapper = mount(VoucherCreate, mountOptions)
    await flushPromises()
    return wrapper
  }

  // ==================== 1. 初始渲染 ====================

  describe('初始渲染', () => {
    it('应渲染凭证表格,表头包含"摘要""总账科目""明细科目""借方金额""贷方金额"', async () => {
      const wrapper = await mountSimple()
      const html = wrapper.html()
      expect(html).toContain('摘要')
      expect(html).toContain('总账科目')
      expect(html).toContain('明细科目')
      expect(html).toContain('借方金额')
      expect(html).toContain('贷方金额')
    })

    it('应渲染 11 个位格标签(亿~分)', async () => {
      const wrapper = await mountSimple()
      const html = wrapper.html()
      expect(html).toContain('亿')
      expect(html).toContain('万')
      expect(html).toContain('元')
      expect(html).toContain('角')
      expect(html).toContain('分')
    })

    it('默认应有一行空分录', async () => {
      const wrapper = await mountSimple()
      const vm = wrapper.vm as any
      expect(vm.form.details).toHaveLength(1)
    })
  })

  // ==================== 2. 金额输入 ====================

  describe('金额输入与位格解析', () => {
    it('输入借方金额 5000 应将 debit 设为 5000', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      // 模拟输入借方金额
      row.debitInput = '5000'
      vm.handleDebitInput(row)
      await nextTick()

      expect(row.debit).toBe(5000)
      expect(row.credit).toBe(0)
      expect(row.creditInput).toBe('')
    })

    it('输入借方金额时应清空贷方金额(互斥)', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      // 先输入贷方
      row.creditInput = '3000'
      vm.handleCreditInput(row)
      expect(row.credit).toBe(3000)

      // 再输入借方,贷方应被清空
      row.debitInput = '5000'
      vm.handleDebitInput(row)
      await nextTick()

      expect(row.debit).toBe(5000)
      expect(row.credit).toBe(0)
      expect(row.creditInput).toBe('')
    })

    it('输入贷方金额时应清空借方金额(互斥)', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.debitInput = '5000'
      vm.handleDebitInput(row)
      expect(row.debit).toBe(5000)

      row.creditInput = '3000'
      vm.handleCreditInput(row)
      await nextTick()

      expect(row.credit).toBe(3000)
      expect(row.debit).toBe(0)
      expect(row.debitInput).toBe('')
    })

    it('输入 5000 时 getDebitDigits 应返回正确的位格(5千0百0十0元,角分空)', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.debitInput = '5000'
      vm.handleDebitInput(row)

      const digits = vm.getDebitDigits(row)
      // 索引 5=千=5, 6=百=0, 7=十=0, 8=元=0, 9=角='', 10=分=''
      expect(digits[5]).toBe('5')
      expect(digits[6]).toBe('0')
      expect(digits[7]).toBe('0')
      expect(digits[8]).toBe('0')
      expect(digits[9]).toBe('')
      expect(digits[10]).toBe('')
    })

    it('输入 5000.56 时位格应角=5 分=6', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.debitInput = '5000.56'
      vm.handleDebitInput(row)

      const digits = vm.getDebitDigits(row)
      expect(digits[9]).toBe('5')  // 角
      expect(digits[10]).toBe('6') // 分
    })

    it('sanitizeAmountInput 应过滤非数字字符并限制小数位', async () => {
      const { sanitizeAmountInput } = await import('@/utils/amountDigits')
      expect(sanitizeAmountInput('abc12.3def')).toBe('12.3')
      expect(sanitizeAmountInput('12.3456')).toBe('12.34')
    })
  })

  // ==================== 3. 科目选择 ====================

  describe('科目选择联动', () => {
    it('选择总账科目后应设置 subjectId 为总账科目 ID', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.generalSubjectId = 1
      vm.handleGeneralSubjectChange(row)
      await nextTick()

      expect(row.subjectId).toBe(1)
      expect(row.detailSubjectId).toBe(0)
    })

    it('getDetailSubjectOptions 应返回总账科目的子科目', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      const options = vm.getDetailSubjectOptions(1) // 库存现金
      expect(options).toHaveLength(1)
      expect(options[0].subjectName).toBe('人民币')
    })

    it('未选择总账科目时 getDetailSubjectOptions 应返回空数组', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      const options = vm.getDetailSubjectOptions(0)
      expect(options).toHaveLength(0)
    })

    it('选择明细科目后 subjectId 应为明细科目 ID', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.generalSubjectId = 1
      row.detailSubjectId = 11
      vm.handleDetailSubjectChange(row)
      await nextTick()

      expect(row.subjectId).toBe(11)
    })

    it('切换总账科目时应清空明细科目', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const row = vm.form.details[0]

      row.generalSubjectId = 1
      row.detailSubjectId = 11
      vm.handleGeneralSubjectChange(row)
      await nextTick()

      expect(row.detailSubjectId).toBe(0)
      expect(row.subjectId).toBe(1)
    })
  })

  // ==================== 4. 增删行 ====================

  describe('增删分录行', () => {
    it('handleAddRow 应增加一行', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any
      const initialCount = vm.form.details.length

      vm.handleAddRow()
      await nextTick()

      expect(vm.form.details).toHaveLength(initialCount + 1)
    })

    it('删除行应减少行数', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.handleAddRow()
      expect(vm.form.details).toHaveLength(2)

      vm.handleRemoveRow(0)
      expect(vm.form.details).toHaveLength(1)
    })

    it('只剩一行时删除应提示"至少保留一行"', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.handleRemoveRow(0)
      expect(ElMessage.warning).toHaveBeenCalledWith('至少保留一行分录')
      expect(vm.form.details).toHaveLength(1)
    })
  })

  // ==================== 5. 提交校验 ====================

  describe('提交校验', () => {
    it('空表单提交应提示"请至少填写一行有效分录"', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      // 设置表头使其通过校验
      vm.form.voucherDate = '2026-01-01'
      vm.form.voucherWordId = 1

      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('请至少填写一行有效分录(含科目和金额)')
    })

    it('有金额但缺摘要应提示"请填写所有分录的摘要"', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.form.voucherDate = '2026-01-01'
      vm.form.voucherWordId = 1
      const row = vm.form.details[0]
      row.generalSubjectId = 1
      row.subjectId = 1
      row.summary = ''
      row.debitInput = '5000'
      vm.handleDebitInput(row)

      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('请填写所有分录的摘要')
    })

    it('借贷不平衡应提示"借贷不平衡"', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.form.voucherDate = '2026-01-01'
      vm.form.voucherWordId = 1

      // 第一行借方 5000
      const row1 = vm.form.details[0]
      row1.summary = '测试'
      row1.generalSubjectId = 1
      row1.subjectId = 1
      row1.debitInput = '5000'
      vm.handleDebitInput(row1)

      // 第二行贷方 3000(不平衡)
      vm.handleAddRow()
      const row2 = vm.form.details[1]
      row2.summary = '测试2'
      row2.generalSubjectId = 2
      row2.subjectId = 2
      row2.creditInput = '3000'
      vm.handleCreditInput(row2)

      await vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('借贷不平衡，请检查金额')
    })

    it('借贷平衡且填写完整时应调用 create API', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.form.voucherDate = '2026-01-01'
      vm.form.voucherWordId = 1

      // 第一行借方 5000
      const row1 = vm.form.details[0]
      row1.summary = '收到现金'
      row1.generalSubjectId = 1
      row1.subjectId = 1
      row1.debitInput = '5000'
      vm.handleDebitInput(row1)

      // 第二行贷方 5000(平衡)
      vm.handleAddRow()
      const row2 = vm.form.details[1]
      row2.summary = '收入'
      row2.generalSubjectId = 2
      row2.subjectId = 2
      row2.creditInput = '5000'
      vm.handleCreditInput(row2)

      await vm.handleSubmit()
      await flushPromises()

      expect(voucherApi.create).toHaveBeenCalled()
      expect(ElMessage.success).toHaveBeenCalledWith('创建成功')
    })
  })

  // ==================== 6. 编辑模式 ====================

  describe('编辑模式:加载已有凭证', () => {
    it('应调用 getById 加载凭证详情并填充表单', async () => {
      const mockVoucher = {
        id: 10, accountSetId: 1, voucherWordId: 1, voucherWordName: '记',
        voucherNo: 'JZ-001', voucherDate: '2026-01-15', year: 2026, month: 1,
        totalDebit: 5000, totalCredit: 5000, attachmentCount: 2, status: 0,
        auditBy: 0, auditByName: '', auditTime: '', postBy: 0, postByName: '',
        postTime: '', createByName: 'admin', createTime: '',
        details: [
          {
            id: 1, voucherId: 10, lineNo: 1, summary: '收到现金',
            subjectId: 1, subjectCode: '1001', subjectName: '库存现金',
            auxiliaryId: 0, debit: 5000, credit: 0, quantity: 0, unitPrice: 0
          },
          {
            id: 2, voucherId: 10, lineNo: 2, summary: '收入',
            subjectId: 2, subjectCode: '1002', subjectName: '银行存款',
            auxiliaryId: 0, debit: 0, credit: 5000, quantity: 0, unitPrice: 0
          }
        ]
      }
      vi.mocked(voucherApi.getById).mockResolvedValue({ code: 200, message: 'ok', data: mockVoucher } as any)

      const wrapper = await mountComponent({ id: '10' })
      await flushPromises()

      const vm = wrapper.vm as any
      expect(voucherApi.getById).toHaveBeenCalledWith(10)
      expect(vm.isEdit).toBe(true)
      expect(vm.editId).toBe(10)
      expect(vm.form.details).toHaveLength(2)
      expect(vm.form.details[0].summary).toBe('收到现金')
      expect(vm.form.details[0].debit).toBe(5000)
      expect(vm.form.details[0].debitInput).toBe('5000')
      expect(vm.form.details[1].credit).toBe(5000)
      expect(vm.form.details[1].creditInput).toBe('5000')
    })

    it('编辑模式下加载凭证后应反查总账/明细科目', async () => {
      const mockVoucher = {
        id: 10, accountSetId: 1, voucherWordId: 1, voucherWordName: '记',
        voucherNo: 'JZ-001', voucherDate: '2026-01-15', year: 2026, month: 1,
        totalDebit: 5000, totalCredit: 5000, attachmentCount: 0, status: 0,
        auditBy: 0, auditByName: '', auditTime: '', postBy: 0, postByName: '',
        postTime: '', createByName: 'admin', createTime: '',
        details: [
          {
            id: 1, voucherId: 10, lineNo: 1, summary: '收到现金',
            subjectId: 11, subjectCode: '1001.01', subjectName: '人民币',
            auxiliaryId: 0, debit: 5000, credit: 0, quantity: 0, unitPrice: 0
          }
        ]
      }
      vi.mocked(voucherApi.getById).mockResolvedValue({ code: 200, message: 'ok', data: mockVoucher } as any)

      const wrapper = await mountComponent({ id: '10' })
      await flushPromises()

      const vm = wrapper.vm as any
      const row = vm.form.details[0]
      // subjectId=11 是"人民币"(二级科目),其一级祖先为 id=1 "库存现金"
      expect(row.subjectId).toBe(11)
      expect(row.generalSubjectId).toBe(1) // 库存现金
      expect(row.detailSubjectId).toBe(11) // 人民币
    })

    it('编辑模式提交应调用 update 而非 create', async () => {
      const mockVoucher = {
        id: 10, accountSetId: 1, voucherWordId: 1, voucherWordName: '记',
        voucherNo: 'JZ-001', voucherDate: '2026-01-15', year: 2026, month: 1,
        totalDebit: 5000, totalCredit: 5000, attachmentCount: 0, status: 0,
        auditBy: 0, auditByName: '', auditTime: '', postBy: 0, postByName: '',
        postTime: '', createByName: 'admin', createTime: '',
        details: [
          {
            id: 1, voucherId: 10, lineNo: 1, summary: '收到现金',
            subjectId: 1, subjectCode: '1001', subjectName: '库存现金',
            auxiliaryId: 0, debit: 5000, credit: 0, quantity: 0, unitPrice: 0
          },
          {
            id: 2, voucherId: 10, lineNo: 2, summary: '收入',
            subjectId: 2, subjectCode: '1002', subjectName: '银行存款',
            auxiliaryId: 0, debit: 0, credit: 5000, quantity: 0, unitPrice: 0
          }
        ]
      }
      vi.mocked(voucherApi.getById).mockResolvedValue({ code: 200, message: 'ok', data: mockVoucher } as any)

      const wrapper = await mountComponent({ id: '10' })
      await flushPromises()

      const vm = wrapper.vm as any
      await vm.handleSubmit()
      await flushPromises()

      expect(voucherApi.update).toHaveBeenCalledWith(10, expect.any(Object))
      expect(ElMessage.success).toHaveBeenCalledWith('更新成功')
    })
  })

  // ==================== 7. 合计行 ====================

  describe('合计行位格', () => {
    it('totalDebitDigits 应正确计算合计金额的位格', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      // 两行各 2500,合计 5000
      const row1 = vm.form.details[0]
      row1.debitInput = '2500'
      vm.handleDebitInput(row1)

      vm.handleAddRow()
      const row2 = vm.form.details[1]
      row2.debitInput = '2500'
      vm.handleDebitInput(row2)

      await nextTick()

      // 合计 5000 → 千位=5, 百位=0, 十位=0, 元=0
      expect(vm.totalDebit).toBe(5000)
      const digits = vm.totalDebitDigits
      expect(digits[5]).toBe('5') // 千
      expect(digits[8]).toBe('0') // 元
      expect(digits[9]).toBe('')  // 角(整数无小数)
    })
  })

  // ==================== 8. 导出功能 ====================

  describe('导出功能', () => {
    it('无可导出数据时应提示警告', async () => {
      const wrapper = await mountComponent()
      const vm = wrapper.vm as any

      vm.handleExport()
      await flushPromises()

      expect(ElMessage.warning).toHaveBeenCalledWith('暂无可导出的分录数据')
    })
  })
})
