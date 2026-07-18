import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

// mock element-plus 的 ElMessage(避免 jsdom 下警告),其余组件保留真实实现
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

// mock settingApi:控制 loadAllConfigs / saveConfig 行为
vi.mock('@/api/system', () => ({
  settingApi: {
    getValue: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    page: vi.fn()
  }
}))

import SystemSetting from '@/views/system/setting/SystemSetting.vue'
import { settingApi } from '@/api/system'
import { ElMessage } from 'element-plus'

/**
 * SystemSetting.vue 组件测试 (F-009)
 *
 * 测试策略:Element Plus 在 jsdom 下渲染不完整,无法可靠触发 DOM 事件。
 * 因此本测试聚焦于 F-009 修复的核心价值 —— API 集成逻辑:
 * 1. onMounted 触发并发拉取所有配置项(page + 6 项 getValue)
 * 2. 配置值回显到组件内部 reactive 表单
 * 3. configIdMap 在 loadAllConfigs 完成后正确建立
 * 4. loadAllConfigs 失败时静默处理(不抛错)
 *
 * DOM 交互(按钮点击触发 save)留待 e2e 测试覆盖。
 */
describe('SystemSetting.vue - API 集成 (F-009)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  function mockPageResponse(items: Array<{ id: number; configKey: string; configName: string; configValue: string }>) {
    ;(settingApi.page as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: items, total: items.length, pageNum: 1, pageSize: 100, pages: 1 },
      timestamp: 0
    })
  }

  it('onMounted 应调用 loadAllConfigs(并发拉取 page + 6 项 getValue)', async () => {
    mockPageResponse([])
    ;(settingApi.getValue as any).mockResolvedValue({ code: 200, message: 'ok', data: '', timestamp: 0 })

    mount(SystemSetting)
    await flushPromises()

    // 1 次 page + 6 项 getValue(companyName/address/phone/email/fiscalYearStartMonth/defaultCurrency)
    expect(settingApi.page).toHaveBeenCalledTimes(1)
    expect(settingApi.getValue).toHaveBeenCalledTimes(6)
    // 6 项 key 应覆盖所有 CONFIG_KEYS
    const calledKeys = (settingApi.getValue as any).mock.calls.map((c: [string]) => c[0])
    expect(calledKeys).toEqual(expect.arrayContaining([
      'company.name', 'company.address', 'company.phone', 'company.email',
      'system.fiscal_year_start_month', 'system.default_currency'
    ]))
  })

  it('应将后端返回的配置值回显到表单(通过 vm 访问内部 reactive 状态)', async () => {
    mockPageResponse([
      { id: 10, configKey: 'company.name', configName: '公司名称', configValue: '测试公司' },
      { id: 11, configKey: 'company.address', configName: '公司地址', configValue: '北京市' },
      { id: 12, configKey: 'company.phone', configName: '联系电话', configValue: '13800000000' },
      { id: 13, configKey: 'company.email', configName: '公司邮箱', configValue: 'test@test.com' },
      { id: 14, configKey: 'system.fiscal_year_start_month', configName: '会计年度起始月', configValue: '3' },
      { id: 15, configKey: 'system.default_currency', configName: '默认币种', configValue: 'USD' }
    ])
    ;(settingApi.getValue as any).mockImplementation((k: string) => {
      const map: Record<string, string> = {
        'company.name': '测试公司',
        'company.address': '北京市',
        'company.phone': '13800000000',
        'company.email': 'test@test.com',
        'system.fiscal_year_start_month': '3',
        'system.default_currency': 'USD'
      }
      return Promise.resolve({ code: 200, message: 'ok', data: map[k] ?? '', timestamp: 0 })
    })

    const wrapper = mount(SystemSetting)
    await flushPromises()

    // 通过组件 vm 访问 setup 暴露的 reactive 状态
    const vm = wrapper.vm as any
    expect(vm.companyForm.companyName).toBe('测试公司')
    expect(vm.companyForm.address).toBe('北京市')
    expect(vm.companyForm.phone).toBe('13800000000')
    expect(vm.companyForm.email).toBe('test@test.com')
    expect(vm.systemForm.fiscalYearStartMonth).toBe(3)
    expect(vm.systemForm.defaultCurrency).toBe('USD')
  })

  it('configIdMap 应正确建立(key -> id 映射,用于后续 update)', async () => {
    mockPageResponse([
      { id: 10, configKey: 'company.name', configName: '公司名称', configValue: '' },
      { id: 11, configKey: 'company.address', configName: '公司地址', configValue: '' }
    ])
    ;(settingApi.getValue as any).mockResolvedValue({ code: 200, message: 'ok', data: '', timestamp: 0 })

    const wrapper = mount(SystemSetting)
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.configIdMap['company.name']).toBe(10)
    expect(vm.configIdMap['company.address']).toBe(11)
  })

  it('loadAllConfigs 失败应静默处理(不抛错)', async () => {
    ;(settingApi.page as any).mockRejectedValue(new Error('网络错误'))
    ;(settingApi.getValue as any).mockResolvedValue({ code: 200, message: 'ok', data: '', timestamp: 0 })

    // 不应抛错
    const wrapper = mount(SystemSetting)
    await flushPromises()
    // 组件应正常挂载
    expect(wrapper.find('.system-setting-container').exists()).toBe(true)
  })

  it('某项 getValue 失败时不应影响其他项加载(catch(() => null))', async () => {
    mockPageResponse([])
    // 让 company.name 失败,其他成功
    ;(settingApi.getValue as any).mockImplementation((k: string) => {
      if (k === 'company.name') {
        return Promise.reject(new Error('404'))
      }
      return Promise.resolve({
        code: 200, message: 'ok',
        data: k === 'company.address' ? '北京市' : '',
        timestamp: 0
      })
    })

    const wrapper = mount(SystemSetting)
    await flushPromises()

    // company.name 失败被 catch,不应阻断其他项;address 应正常回显
    const vm = wrapper.vm as any
    expect(vm.companyForm.address).toBe('北京市')
    // company.name 保留默认空字符串
    expect(vm.companyForm.companyName).toBe('')
  })
})
