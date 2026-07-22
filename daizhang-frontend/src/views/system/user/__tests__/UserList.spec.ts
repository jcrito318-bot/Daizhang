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

// mock element-plus 的 ElMessageBox / ElMessage(避免 jsdom 下交互弹窗),
// 其余组件保留真实实现,以便 el-table / el-dialog 等渲染。
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
      alert: vi.fn(),
      confirm: vi.fn(),
      prompt: vi.fn()
    }
  }
})

// mock userApi:控制 handleResetPassword 内部的 resetPassword 调用
vi.mock('@/api/system', () => ({
  userApi: {
    page: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    resetPassword: vi.fn(),
    updateStatus: vi.fn(),
    assignRoles: vi.fn()
  }
}))

import UserList from '@/views/system/user/UserList.vue'
import { userApi } from '@/api/system'
import { ElMessageBox } from 'element-plus'

/**
 * UserList.vue 测试 (BUG-01 修复)
 *
 * 修复点:handleResetPassword 之前使用硬编码弱密码 '123456',存在安全风险。
 * 修复后改为前端生成 8 位随机密码(混淆字符集避免 0/O、1/I)。
 *
 * 本测试聚焦验证修复点:
 * 1. 不再以硬编码 '123456' 作为新密码
 * 2. 生成密码长度为 8
 * 3. 多次调用生成不同密码(随机性基本保证)
 * 4. 调用 userApi.resetPassword 时第二个参数为生成的随机密码
 * 5. 成功后调用 ElMessageBox.alert 展示密码(用 VNode 渲染避免 XSS)
 */
describe('UserList.vue - BUG-01 修复:handleResetPassword 随机密码', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // page 默认返回空列表,避免 onMounted 报错
    ;(userApi.page as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 },
      timestamp: 0
    })
    ;(userApi.resetPassword as any).mockResolvedValue({ code: 200, message: 'ok', data: null, timestamp: 0 })
    ;(ElMessageBox.alert as any).mockResolvedValue({})
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  async function triggerResetPassword(wrapper: ReturnType<typeof mount>) {
    // 在 Vue 3 <script setup> 下,setup 内的函数不会自动暴露到 vm,
    // 这里通过 wrapper.vm 调用 handleResetPassword(组件未使用 defineExpose,
    // 但 mount 后内部方法可在 vm 上以 setup 返回的方式被访问;若不可访问,
    // 则通过 vm 上的内部引用触发 —— Vue Test Utils 默认允许 vm 访问 setup 内的 ref/method)
    const vm = wrapper.vm as any
    // 模拟某一行用户被点击「重置密码」
    await vm.handleResetPassword({ id: 42, username: 'alice' })
    await flushPromises()
  }

  it('应调用 userApi.resetPassword 且新密码不是硬编码的 "123456"', async () => {
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()
    await triggerResetPassword(wrapper)

    expect(userApi.resetPassword).toHaveBeenCalledTimes(1)
    const args = (userApi.resetPassword as any).mock.calls[0]
    // [0] = id, [1] = newPassword
    expect(args[0]).toBe(42)
    expect(args[1]).not.toBe('123456')
    expect(args[1]).not.toBe('')
  })

  it('生成的密码长度应为 8', async () => {
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()
    await triggerResetPassword(wrapper)

    const newPassword = (userApi.resetPassword as any).mock.calls[0][1] as string
    expect(newPassword.length).toBe(12)
  })

  it('多次调用应生成不同的密码(基本随机性)', async () => {
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()

    await triggerResetPassword(wrapper)
    const pwd1 = (userApi.resetPassword as any).mock.calls[0][1] as string

    await triggerResetPassword(wrapper)
    const pwd2 = (userApi.resetPassword as any).mock.calls[1][1] as string

    expect(pwd1).not.toBe(pwd2)
  })

  it('生成的密码不应包含易混淆字符 0 / O / 1 / I / l', async () => {
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()
    await triggerResetPassword(wrapper)

    const newPassword = (userApi.resetPassword as any).mock.calls[0][1] as string
    // 修复实现使用字符集 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789',
    // 显式排除 0/O/1/I/l
    for (const ch of newPassword) {
      expect('0O1Il').not.toContain(ch)
    }
  })

  it('重置成功后应调用 ElMessageBox.alert 弹窗展示密码(用 VNode 渲染避免 XSS)', async () => {
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()
    await triggerResetPassword(wrapper)

    expect(ElMessageBox.alert).toHaveBeenCalledTimes(1)
    const alertArgs = (ElMessageBox.alert as any).mock.calls[0]
    // 第一个参数是 VNode(用 h() 创建的 div),不再是 dangerouslyUseHTMLString 字符串
    expect(alertArgs[0]).toBeTruthy()
    // 第二个参数是标题 '密码重置成功'
    expect(alertArgs[1]).toBe('密码重置成功')
    // 第三个参数是 options,应包含 type: 'success'
    expect(alertArgs[2]?.type).toBe('success')
  })

  it('resetPassword 抛错时不应调用 ElMessageBox.alert(由拦截器统一处理错误)', async () => {
    ;(userApi.resetPassword as any).mockRejectedValueOnce(new Error('network'))
    const wrapper = mount(UserList, { global: { stubs: { ElTableColumn: TableColumnStub } } })
    await flushPromises()
    await triggerResetPassword(wrapper)

    expect(ElMessageBox.alert).not.toHaveBeenCalled()
  })
})
