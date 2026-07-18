import { describe, bench, beforeAll } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
    getUserInfo: vi.fn()
  }
}))

vi.mock('@/api/system', () => ({
  settingApi: {
    getValue: vi.fn().mockResolvedValue({ code: 200, message: 'ok', data: '', timestamp: 0 }),
    create: vi.fn(),
    update: vi.fn(),
    page: vi.fn().mockResolvedValue({
      code: 200, message: 'ok',
      data: { list: [], total: 0, pageNum: 1, pageSize: 100, pages: 0 },
      timestamp: 0
    })
  }
}))

import SystemSetting from '@/views/system/setting/SystemSetting.vue'

/**
 * 组件渲染性能基准测试
 *
 * 用 bench() 重复执行,测量关键路径的执行时间。
 * Vitest bench 基于 tinybench,会自动 warmup + 多次迭代。
 *
 * 关注指标:
 * - ops/sec:每秒可执行次数(越高越好)
 * - mean:平均单次耗时
 * - p99:99% 分位耗时(尾部延迟)
 */
describe('组件渲染性能基准', () => {
  beforeAll(() => {
    setActivePinia(createPinia())
  })

  bench('SystemSetting.vue 挂载 + onMounted 触发 loadAllConfigs', async () => {
    // 每次迭代需新 pinia(避免 store 状态污染)
    setActivePinia(createPinia())
    const wrapper = mount(SystemSetting)
    await flushPromises()
    wrapper.unmount()
  }, {
    iterations: 20,
    warmupIterations: 5
  })
})
