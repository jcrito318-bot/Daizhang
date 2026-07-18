import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

// mock vue-router 的 useRouter
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

import NotFound from '@/views/error/NotFound.vue'

/**
 * NotFound.vue 组件测试
 *
 * 覆盖:
 * - 渲染 404 错误码与提示文案
 * - 点击「返回首页」按钮调用 router.push('/dashboard')
 */
describe('NotFound.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // 使用 ElementPlus 插件挂载,使 el-button 等组件被正确解析
  const mountOptions = { global: { plugins: [ElementPlus] } }

  it('应渲染 404 错误码与提示文案', () => {
    const wrapper = mount(NotFound, mountOptions)
    expect(wrapper.find('.error-code').text()).toBe('404')
    expect(wrapper.find('.error-message').text()).toContain('页面不存在')
    expect(wrapper.find('.error-hint').text()).toContain('返回首页')
  })

  it('点击「返回首页」按钮应调用 router.push(/dashboard)', async () => {
    const wrapper = mount(NotFound, mountOptions)
    const allButtons = wrapper.findAll('button')
    const homeBtn = allButtons.find(b => b.html().includes('返回首页'))
    expect(homeBtn).toBeTruthy()
    await homeBtn!.trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/dashboard')
    expect(mockPush).toHaveBeenCalledTimes(1)
  })
})
