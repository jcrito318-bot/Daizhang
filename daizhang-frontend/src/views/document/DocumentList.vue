<template>
  <div class="document-list-container">
    <!-- 搜索筛选区域 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="票据类型">
          <el-select v-model="queryForm.documentType" placeholder="请选择类型" clearable style="width: 140px">
            <el-option label="发票" :value="1" />
            <el-option label="银行回单" :value="2" />
            <el-option label="费用单据" :value="3" />
            <el-option label="收据" :value="4" />
            <el-option label="其他" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="票据号码">
          <el-input v-model="queryForm.documentNo" placeholder="请输入票据号码" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="销方名称">
          <el-input v-model="queryForm.sellerName" placeholder="请输入销方名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>票据列表</span>
          <div>
            <el-button
              type="danger"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>新增票据
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        empty-text="暂无票据数据,可点击「新增票据」或调整筛选条件"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="documentNo" label="票据号码" width="140" />
        <el-table-column label="票据类型" width="120">
          <template #default="{ row }">
            {{ documentTypeText(row.documentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="documentDate" label="开票日期" width="120" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column label="税额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.taxAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="价税合计" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="销方名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="购方名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联凭证" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.voucherId" type="success">已关联</el-tag>
            <el-tag v-else type="info">未关联</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleLinkVoucher(row)" v-if="!row.voucherId">
              关联凭证
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑票据' : '新增票据'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
        class="document-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="票据类型" prop="documentType">
              <el-select v-model="form.documentType" placeholder="请选择票据类型" style="width: 100%">
                <el-option label="发票" :value="1" />
                <el-option label="银行回单" :value="2" />
                <el-option label="费用单据" :value="3" />
                <el-option label="收据" :value="4" />
                <el-option label="其他" :value="9" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开票日期" prop="documentDate">
              <el-date-picker
                v-model="form.documentDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="票据号码" prop="documentNo">
              <el-input v-model="form.documentNo" placeholder="请输入票据号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发票代码" prop="invoiceCode">
              <el-input v-model="form.invoiceCode" placeholder="请输入发票代码" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发票号码" prop="invoiceNumber">
              <el-input v-model="form.invoiceNumber" placeholder="请输入发票号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额" prop="amount">
              <el-input-number
                v-model="form.amount"
                :precision="2"
                :min="0"
                style="width: 100%"
                @change="calculateTotal"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="税额" prop="taxAmount">
              <el-input-number
                v-model="form.taxAmount"
                :precision="2"
                :min="0"
                style="width: 100%"
                @change="calculateTotal"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价税合计">
              <el-input-number
                v-model="form.totalAmount"
                :precision="2"
                :min="0"
                style="width: 100%"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="销方名称" prop="sellerName">
          <el-input v-model="form.sellerName" placeholder="请输入销方名称" />
        </el-form-item>

        <el-form-item label="购方名称" prop="buyerName">
          <el-input v-model="form.buyerName" placeholder="请输入购方名称" />
        </el-form-item>

        <el-form-item label="OCR识别内容" prop="ocrContent">
          <el-input
            v-model="form.ocrContent"
            type="textarea"
            :rows="3"
            placeholder="请输入OCR识别内容"
          />
        </el-form-item>

        <el-form-item label="票据附件" prop="fileUrl">
          <div class="upload-section">
            <el-upload
              v-model:file-list="fileList"
              :http-request="customUploadRequest"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              :limit="1"
              accept=".jpg,.jpeg,.png,.pdf"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>上传附件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">支持 JPG/PNG/PDF 格式，文件大小不超过 10MB</div>
              </template>
            </el-upload>
            
            <el-button 
              type="success" 
              :loading="aiRecognizing"
              :disabled="!form.fileUrl"
              @click="handleAiRecognize"
              style="margin-left: 10px;"
            >
              <el-icon><MagicStick /></el-icon>
              {{ aiRecognizing ? 'AI识别中...' : 'AI智能识别' }}
            </el-button>
          </div>
          
          <div v-if="aiRecognizeResult" class="ai-result-tip">
            <el-alert
              title="AI识别成功，已自动填充表单信息"
              type="success"
              :closable="true"
              show-icon
            />
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联凭证弹窗 -->
    <el-dialog
      v-model="linkDialogVisible"
      title="关联凭证"
      width="600px"
    >
      <el-form :model="linkForm" inline>
        <el-form-item label="凭证号">
          <el-input v-model="linkForm.voucherNo" placeholder="请输入凭证号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchVoucher">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="voucherList"
        v-loading="voucherLoading"
        border
        highlight-current-row
        @current-change="handleVoucherSelect"
        max-height="400"
      >
        <el-table-column prop="voucherNo" label="凭证号" width="140" />
        <el-table-column prop="voucherDate" label="凭证日期" width="120" />
        <el-table-column label="摘要" min-width="200">
          <template #default="{ row }">
            {{ row.details && row.details.length > 0 ? row.details[0].summary : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalDebit" label="借方金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalDebit) }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedVoucher" @click="confirmLinkVoucher">
          确定关联
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions, UploadUserFile } from 'element-plus'
import { documentApi } from '@/api/document'
import { voucherApi } from '@/api/voucher'
import { aiApi } from '@/api/ai'
import request from '@/utils/request'
import { useAppStore } from '@/stores/app'
import type { DocumentVO, DocumentCreateRequest, DocumentQueryRequest } from '@/types/document'
import type { VoucherVO, VoucherQueryRequest } from '@/types/voucher'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const total = ref(0)
const tableData = ref<DocumentVO[]>([])
const selectedIds = ref<number[]>([])

// AI识别相关状态
const aiRecognizing = ref(false)
const aiRecognizeResult = ref(false)

const dateRange = ref<[string, string] | null>(null)
const queryForm = reactive<DocumentQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  documentType: undefined,
  documentNo: '',
  sellerName: '',
  startDate: undefined,
  endDate: undefined,
  pageNum: 1,
  pageSize: 10
})

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const fileList = ref<UploadUserFile[]>([])

const form = reactive<DocumentCreateRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  documentType: 1,
  documentDate: new Date().toISOString().slice(0, 10),
  documentNo: '',
  invoiceCode: '',
  invoiceNumber: '',
  amount: 0,
  taxAmount: 0,
  totalAmount: 0,
  sellerName: '',
  buyerName: '',
  ocrContent: '',
  fileUrl: '',
  remark: ''
})

const formRules: FormRules = {
  documentType: [{ required: true, message: '请选择票据类型', trigger: 'change' }],
  documentDate: [{ required: true, message: '请选择开票日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

// F-010 修复:el-upload 不再使用 action+headers(会绕过 axios 拦截器,丢失 401 自动刷新/统一错误处理),
// 改用 :http-request 接入 request 工具,调用后端 /document/upload 端点。
// BUG-02 修复:统一 fileUrl 赋值入口在 handleUploadSuccess 中完成,避免双重赋值且类型不一致。
//   - 之前:customUploadRequest 内 form.fileUrl = res.data.fileUrl,然后 onSuccess(res)
//          handleUploadSuccess 又 form.fileUrl = response.data(对象,而非 data.fileUrl 字符串)
//   - 现在:customUploadRequest 只负责发起请求并转发 onSuccess/onError,fileUrl 由 handleUploadSuccess 统一处理。
const customUploadRequest = async (options: UploadRequestOptions) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await request.post('/document/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    // Element Plus onSuccess 签名:(response, uploadFile?) => void
    // fileUrl 的赋值统一交给 handleUploadSuccess,这里不直接操作 form.fileUrl
    onSuccess?.(res)
  } catch (err) {
    // onError 期望 UploadAjaxError 形状,补齐 status/method/url 字段
    const e = err as Error & { status?: number; method?: string; url?: string }
    e.status = 0
    e.method = 'POST'
    e.url = '/document/upload'
    onError?.(e as Error & { status: number; method: string; url: string })
  }
}

// 关联凭证弹窗
const linkDialogVisible = ref(false)
const linkDocumentId = ref<number>(0)
const voucherLoading = ref(false)
const voucherList = ref<VoucherVO[]>([])
const selectedVoucher = ref<VoucherVO | null>(null)
const linkForm = reactive({
  voucherNo: ''
})

function documentTypeText(type: number): string {
  const map: Record<number, string> = {
    1: '发票',
    2: '银行回单',
    3: '费用单据',
    4: '收据',
    9: '其他'
  }
  return map[type] || '未知'
}

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '待处理', 1: '已处理', 2: '已作废' }
  return map[status] || '未知'
}

function statusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

function formatAmount(val: number | undefined): string {
  if (val === undefined || val === null || val === 0) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calculateTotal() {
  form.totalAmount = (form.amount || 0) + (form.taxAmount || 0)
}

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }

  // 处理日期范围
  if (dateRange.value) {
    queryForm.startDate = dateRange.value[0]
    queryForm.endDate = dateRange.value[1]
  } else {
    queryForm.startDate = undefined
    queryForm.endDate = undefined
  }

  loading.value = true
  try {
    const res = await documentApi.getPage(queryForm)
    tableData.value = res.data.list
    total.value = res.data.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

function handleReset() {
  dateRange.value = null
  queryForm.documentType = undefined
  queryForm.documentNo = ''
  queryForm.sellerName = ''
  queryForm.startDate = undefined
  queryForm.endDate = undefined
  queryForm.pageNum = 1
  loadData()
}

function handleSelectionChange(selection: DocumentVO[]) {
  selectedIds.value = selection.map(item => item.id)
}

function handleCreate() {
  isEdit.value = false
  editId.value = 0
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: DocumentVO) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    accountSetId: row.accountSetId,
    documentType: row.documentType,
    documentDate: row.documentDate,
    documentNo: row.documentNo,
    invoiceCode: row.invoiceCode,
    invoiceNumber: row.invoiceNumber,
    amount: row.amount,
    taxAmount: row.taxAmount,
    totalAmount: row.totalAmount,
    sellerName: row.sellerName,
    buyerName: row.buyerName,
    ocrContent: row.ocrContent,
    fileUrl: row.fileUrl,
    remark: row.remark
  })
  if (row.fileUrl) {
    fileList.value = [{ name: '附件', url: row.fileUrl }]
  } else {
    fileList.value = []
  }
  dialogVisible.value = true
}

function handleView(row: DocumentVO) {
  router.push(`/document/${row.id}`)
}

async function handleDelete(row: DocumentVO) {
  await ElMessageBox.confirm(`确定要删除票据"${row.documentNo}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await documentApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条票据吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  // BF-14 修复:原 Promise.all 在部分失败时整个 catch 块吞掉错误,
  // 仍提示"批量删除成功",用户无法感知失败项。
  // 改用 Promise.allSettled 统计 fulfilled/rejected,给出准确反馈。
  const results = await Promise.allSettled(selectedIds.value.map(id => documentApi.delete(id)))
  const succeeded = results.filter(r => r.status === 'fulfilled').length
  const failed = results.length - succeeded
  if (failed === 0) {
    ElMessage.success(`批量删除成功,共 ${succeeded} 条`)
  } else if (succeeded === 0) {
    ElMessage.error(`批量删除失败,共 ${failed} 条`)
  } else {
    // 部分成功:明确告知用户,避免误以为全部成功
    ElMessage.warning(`成功 ${succeeded} 条,失败 ${failed} 条,请检查列表后重试失败项`)
  }
  loadData()
}

function resetForm() {
  form.accountSetId = appStore.currentAccountSetId || 0
  form.documentType = 1
  form.documentDate = new Date().toISOString().slice(0, 10)
  form.documentNo = ''
  form.invoiceCode = ''
  form.invoiceNumber = ''
  form.amount = 0
  form.taxAmount = 0
  form.totalAmount = 0
  form.sellerName = ''
  form.buyerName = ''
  form.ocrContent = ''
  form.fileUrl = ''
  form.remark = ''
  fileList.value = []
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    form.accountSetId = appStore.currentAccountSetId || 0
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await documentApi.update(editId.value, form)
        ElMessage.success('更新成功')
      } else {
        await documentApi.create(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      // handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const isValidType = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png' || rawFile.type === 'application/pdf'
  const isLt10M = rawFile.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只能上传 JPG/PNG/PDF 格式的文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

// BUG-02 修复:后端 /document/upload 返回 Result<{fileUrl:string}>,即 response.data 形状为 { fileUrl: string }。
// 之前错误地用 response.data(对象)直接赋值给 form.fileUrl(应为字符串)。
// 现在统一访问 response.data.fileUrl,确保类型一致。
const handleUploadSuccess: UploadProps['onSuccess'] = (response) => {
  if (response.code === 200 && response.data?.fileUrl) {
    form.fileUrl = response.data.fileUrl
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError: UploadProps['onError'] = () => {
  ElMessage.error('上传失败')
}

function handleLinkVoucher(row: DocumentVO) {
  linkDocumentId.value = row.id
  linkForm.voucherNo = ''
  selectedVoucher.value = null
  voucherList.value = []
  linkDialogVisible.value = true
}

async function searchVoucher() {
  const accountSetId = appStore.currentAccountSetId || 0
  if (!accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }

  const params: VoucherQueryRequest = {
    accountSetId,
    voucherNo: linkForm.voucherNo,
    pageNum: 1,
    pageSize: 50
  }

  voucherLoading.value = true
  try {
    const res = await voucherApi.getPage(params)
    voucherList.value = res.data.list.filter(v => v.status !== 2) // 过滤已过账的凭证
  } catch {
    // handled by interceptor
  } finally {
    voucherLoading.value = false
  }
}

function handleVoucherSelect(row: VoucherVO | null) {
  selectedVoucher.value = row
}

async function confirmLinkVoucher() {
  if (!selectedVoucher.value) return

  try {
    await documentApi.linkVoucher(linkDocumentId.value, selectedVoucher.value.id)
    ElMessage.success('关联成功')
    linkDialogVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  }
}

// ==================== AI 识别相关方法 ====================
/**
 * AI 智能识别票据信息
 * 调用后端 /api/ai/recognize/invoice 接口，解析返回的 JSON 并自动填充表单。
 */
async function handleAiRecognize() {
  if (!form.fileUrl) {
    ElMessage.warning('请先上传票据图片后再进行识别')
    return
  }

  aiRecognizing.value = true
  aiRecognizeResult.value = false
  try {
    // 从已上传的文件 URL 提取路径后调用 OCR 识别
    // 传递票据类型参数（如果已选择）
    const res = await aiApi.recognizeInvoiceByUrl(form.fileUrl, form.documentType)
    const content = (res.data as Record<string, unknown>) || {}

    // 将识别结果回填到表单
    form.documentNo = (content.invoice_number as string) || (content.invoiceNumber as string) || ''
    form.invoiceCode = (content.invoice_code as string) || (content.invoiceCode as string) || ''
    form.invoiceNumber = (content.invoice_number as string) || (content.invoiceNumber as string) || ''
    form.sellerName = (content.seller_name as string) || (content.seller as string) || (content.sellerName as string) || ''
    form.buyerName = (content.buyer_name as string) || (content.buyer as string) || (content.buyerName as string) || ''
    form.documentDate = (content.date as string) || (content.invoice_date as string) || (content.documentDate as string) || form.documentDate

    // 金额字段做数值转换
    const parseAmount = (v: unknown): number => {
      const n = typeof v === 'string' ? parseFloat(v) : typeof v === 'number' ? v : 0
      return isNaN(n) ? 0 : n
    }
    form.amount = parseAmount(content.amount)
    form.taxAmount = parseAmount(content.tax) || parseAmount(content.taxAmount)
    calculateTotal()

    // 保存完整的 OCR 原文用于展示与校对
    form.ocrContent = typeof res.data === 'string' ? res.data : JSON.stringify(res.data, null, 2)

    aiRecognizeResult.value = true
    ElMessage.success('AI 识别成功，已自动填充表单')
  } catch (e: unknown) {
    console.error('AI 识别失败', e)
    ElMessage.error(e instanceof Error ? e.message : 'AI 识别失败，请稍后重试')
  } finally {
    aiRecognizing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.document-list-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.document-form {
  padding: 20px 40px 0 0;
}

.el-upload__tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
