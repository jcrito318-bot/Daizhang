import { describe, it, expect } from 'vitest'

// Vitest 配置冒烟测试:验证测试框架可正常加载
describe('vitest 配置冒烟测试', () => {
  it('应正确执行基本断言', () => {
    expect(1 + 1).toBe(2)
    expect(true).toBe(true)
    expect('hello').toContain('ell')
  })

  it('应正确处理对象比较', () => {
    const obj = { a: 1, b: { c: 2 } }
    expect(obj).toEqual({ a: 1, b: { c: 2 } })
  })

  it('应在 jsdom 环境下运行(有 window/document)', () => {
    expect(window).toBeDefined()
    expect(document).toBeDefined()
    const div = document.createElement('div')
    div.textContent = 'hello jsdom'
    expect(div.textContent).toBe('hello jsdom')
  })

  it('应支持 localStorage(jsdom 默认提供)', () => {
    localStorage.setItem('test_key', 'test_value')
    expect(localStorage.getItem('test_key')).toBe('test_value')
  })
})
