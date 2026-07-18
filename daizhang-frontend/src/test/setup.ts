// Vitest 全局 setup:在所有测试执行前运行,用于初始化测试环境。
// 主要职责:
// 1. 为 jsdom 补充 localStorage / matchMedia 等 API
// 2. 屏蔽 Element Plus 在 jsdom 下的警告噪音
// 3. 隔离每个测试用例的状态(清空 localStorage)
import { vi, afterEach } from 'vitest'
import { config } from '@vue/test-utils'
import { defineComponent } from 'vue'

// ============ 1. jsdom 兼容补丁 ============

// matchMedia:Element Plus 内部会调用,jsdom 默认未实现
if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false
    })
  })
}

// IntersectionObserver:Element Plus 表格虚拟滚动会用到,jsdom 默认未实现
if (!('IntersectionObserver' in window)) {
  ;(window as any).IntersectionObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
    takeRecords() {
      return []
    }
  }
}

// ResizeObserver:某些组件(如表格宽度自适应)会用到
if (!('ResizeObserver' in window)) {
  ;(window as any).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

// scrollTo:Element Plus 的 el-pagination 等会调用
if (!window.scrollTo) {
  window.scrollTo = () => {}
}

// ============ 2. 隔离每个用例的状态 ============

afterEach(() => {
  // 清空 localStorage,避免前一个测试残留 currentAccountSetId/refreshToken 等
  localStorage.clear()
  // 重置所有 mock 调用历史(不影响 mock 实现)
  vi.clearAllMocks()
})

// ============ 3. Vue Test Utils 全局配置 ============

// 提供一个空的 Element Plus 图标占位组件,避免每个测试都手动 stub
const IconStub = defineComponent({
  name: 'ElIcon',
  template: '<span class="el-icon-stub"><slot /></span>'
})

config.global.stubs = {
  ElIcon: IconStub,
  // 跳过过渡动画,加快测试速度
  Transition: { template: '<slot />' },
  TransitionGroup: { template: '<slot />' }
}

// 全局关闭 Element Plus 的警告(测试环境下不影响断言)
config.global.config.warnHandler = () => {}
