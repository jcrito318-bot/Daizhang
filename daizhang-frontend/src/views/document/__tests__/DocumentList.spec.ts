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

// mock element-plus(避免 ElMessage 在 jsdom 下产生副作用)
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

// mock documentApi:控制 loadData 行为
vi.mock('@/api/document', () => ({
  documentApi: {
    getPage: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    linkVoucher: vi.fn(),
    unlinkVoucher: vi.fn()
  }
}))

// mock voucherApi
vi.mock('@/api/voucher', () => ({
  voucherApi: {
    getPage: vi.fn()
  }
}))

// mock aiApi
vi.mock('@/api/ai', () => ({
  aiApi: {
    recognizeInvoiceByUrl: vi.fn()
  }
}))

import DocumentList from '@/views/document/DocumentList.vue'
import { documentApi } from '@/api/document'
import { ElMessage } from 'element-plus'

/**
 * DocumentList.vue 测试 (BUG-02 修复)
 *
 * 修复点:之前 customUploadRequest 直接 form.fileUrl = res.data.fileUrl,
 * 而 handleUploadSuccess 又 form.fileUrl = response.data(对象,而非 data.fileUrl 字符串),
 * 导致 fileUrl 类型错误(对象 vs 字符串)。
 *
 * 修复后:
 *   - customUploadRequest 只负责发起请求并转发 onSuccess/onError,不操作 form.fileUrl
 *   - handleUploadSuccess 统一从 response.data.fileUrl 取值并赋给 form.fileUrl
 *
 * 本测试聚焦验证:
 * 1. handleUploadSuccess 在 response.data.fileUrl 存在时正确赋值给 form.fileUrl
 * 2. response.code !== 200 时不应赋值(走错误路径)
 * 3. response.data.fileUrl 缺失时不应赋值且提示错误
 */
describe('DocumentList.vue - BUG-02 修复:handleUploadSuccess fileUrl 赋值', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // 让 onMounted 的 loadData 直接返回空,避免 jsdom 下 warning
    ;(documentApi.getPage as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: { list: [], total: 0, pageNum: 1, pageSize: 10, pages: 0 },
      timestamp: 0
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('response.code === 200 且 data.fileUrl 存在时,应将 form.fileUrl 设为字符串 URL', async () => {
    const wrapper = mount(DocumentList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    // 模拟后端 /document/upload 返回的 Result
    const fakeResponse = {
      code: 200,
      message: 'ok',
      data: {
        fileName: 'invoice.pdf',
        fileUrl: '/uploads/document/abc123.pdf',
        fileSize: '1024'
      },
      timestamp: 0
    }
    // 直接调用 setup 中暴露的 handleUploadSuccess
    // (Element Plus 的 onSuccess 签名:(response, uploadFile?) => void)
    vm.handleUploadSuccess(fakeResponse)
    await flushPromises()

    expect(vm.form.fileUrl).toBe('/uploads/document/abc123.pdf')
    expect(typeof vm.form.fileUrl).toBe('string')
    expect(ElMessage.success).toHaveBeenCalledWith('上传成功')
  })

  it('response.code !== 200 时不应赋值 fileUrl,且应调用 ElMessage.error', async () => {
    const wrapper = mount(DocumentList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    const originalUrl = vm.form.fileUrl
    const fakeResponse = {
      code: 500,
      message: '上传失败:文件损坏',
      data: null,
      timestamp: 0
    }
    vm.handleUploadSuccess(fakeResponse)
    await flushPromises()

    // fileUrl 保持不变(不被错误响应覆盖)
    expect(vm.form.fileUrl).toBe(originalUrl)
    expect(ElMessage.error).toHaveBeenCalledWith('上传失败:文件损坏')
  })

  it('response.data.fileUrl 缺失时不应赋值,且应提示错误', async () => {
    const wrapper = mount(DocumentList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    const originalUrl = vm.form.fileUrl
    const fakeResponse = {
      code: 200,
      message: '上传成功但未返回文件 URL',
      data: { fileName: 'invoice.pdf', fileSize: '1024' }, // 缺 fileUrl
      timestamp: 0
    }
    vm.handleUploadSuccess(fakeResponse)
    await flushPromises()

    expect(vm.form.fileUrl).toBe(originalUrl)
    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('response.data.fileUrl 不应为对象(回归:修复前曾错误地用 response.data 对象赋值)', async () => {
    const wrapper = mount(DocumentList, mountOptions)
    await flushPromises()

    const vm = wrapper.vm as any
    const fakeResponse = {
      code: 200,
      message: 'ok',
      data: {
        fileName: 'invoice.pdf',
        fileUrl: '/uploads/document/xyz789.pdf',
        fileSize: '2048'
      },
      timestamp: 0
    }
    vm.handleUploadSuccess(fakeResponse)
    await flushPromises()

    // 关键回归点:form.fileUrl 必须是字符串,而不是 data 对象
    expect(vm.form.fileUrl).not.toBe(fakeResponse.data)
    expect(typeof vm.form.fileUrl).toBe('string')
  })
})
