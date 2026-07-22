<template>
  <div class="document-detail-container" v-loading="loading">
    <el-page-header @back="handleBack" title="返回票据列表">
      <template #content>
        <span class="page-title">票据详情</span>
      </template>
    </el-page-header>

    <template v-if="document">
      <!-- 基本信息 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <div>
              <el-button type="primary" @click="handleEdit">编辑</el-button>
              <el-button
                type="success"
                @click="handleLinkVoucher"
                v-if="!document.voucherId"
              >
                关联凭证
              </el-button>
              <el-button
                type="warning"
                @click="handleUnlinkVoucher"
                v-if="document.voucherId"
              >
                取消关联
              </el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="票据号码">{{ document.documentNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="票据类型">{{ documentTypeText(document.documentType) }}</el-descriptions-item>
          <el-descriptions-item label="开票日期">{{ document.documentDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票代码">{{ document.invoiceCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ document.invoiceNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(document.status)">{{ statusText(document.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatAmount(document.amount) }}</el-descriptions-item>
          <el-descriptions-item label="税额">{{ formatAmount(document.taxAmount) }}</el-descriptions-item>
          <el-descriptions-item label="价税合计">
            <span class="total-amount">{{ formatAmount(document.totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="销方名称" :span="3">{{ document.sellerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="购方名称" :span="3">{{ document.buyerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ document.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="OCR识别内容" :span="3">
            <div class="ocr-content">{{ document.ocrContent || '-' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ document.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ document.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ document.updateTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 附件区域 -->
      <el-card class="attachment-card">
        <template #header>
          <span>票据附件</span>
        </template>
        <div v-if="document.fileUrl" class="attachment-content">
          <template v-if="isImage(document.fileUrl)">
            <el-image
              :src="document.fileUrl"
              fit="contain"
              class="preview-image"
              :preview-src-list="[document.fileUrl]"
            />
          </template>
          <template v-else-if="isPdf(document.fileUrl)">
            <div class="pdf-preview">
              <el-icon :size="48"><Document /></el-icon>
              <p>PDF 文件</p>
              <el-button type="primary" @click="openFile(document.fileUrl)">查看 PDF</el-button>
            </div>
          </template>
          <template v-else>
            <div class="file-info">
              <el-icon :size="32"><Document /></el-icon>
              <el-button type="primary" link @click="openFile(document.fileUrl)">下载附件</el-button>
            </div>
          </template>
        </div>
        <el-empty v-else description="暂无附件" :image-size="80" />
      </el-card>

      <!-- 关联凭证信息 -->
      <el-card class="voucher-card">
        <template #header>
          <span>关联凭证</span>
        </template>
        <div v-if="document.voucherId && voucher">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="凭证号">
              <el-button type="primary" link @click="goToVoucher(voucher)">{{ voucher.voucherNo }}</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="凭证日期">{{ voucher.voucherDate }}</el-descriptions-item>
            <el-descriptions-item label="凭证字">{{ voucher.voucherWordName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="借方合计">{{ formatAmount(voucher.totalDebit) }}</el-descriptions-item>
            <el-descriptions-item label="贷方合计">{{ formatAmount(voucher.totalCredit) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="voucherStatusTagType(voucher.status)">{{ voucherStatusText(voucher.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-table :data="voucher.details" border class="voucher-detail-table">
            <el-table-column prop="lineNo" label="行号" width="60" align="center" />
            <el-table-column prop="summary" label="摘要" min-width="200" />
            <el-table-column label="科目" min-width="200">
              <template #default="{ row }">
                {{ row.subjectCode }} {{ row.subjectName }}
              </template>
            </el-table-column>
            <el-table-column label="借方金额" width="120" align="right">
              <template #default="{ row }">
                {{ row.debit ? formatAmount(row.debit) : '' }}
              </template>
            </el-table-column>
            <el-table-column label="贷方金额" width="120" align="right">
              <template #default="{ row }">
                {{ row.credit ? formatAmount(row.credit) : '' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="暂未关联凭证" :image-size="80">
          <el-button type="primary" @click="handleLinkVoucher">关联凭证</el-button>
        </el-empty>
      </el-card>
    </template>

    <!-- 关联凭证弹窗 -->
    <el-dialog
      v-model="linkDialogVisible"
      title="关联凭证"
      width="700px"
    >
      <el-form :model="linkSearchForm" inline>
        <el-form-item label="凭证号">
          <el-input v-model="linkSearchForm.voucherNo" placeholder="请输入凭证号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchVoucher">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="voucherList"
        v-loading="voucherSearchLoading"
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { documentApi } from '@/api/document'
import { voucherApi } from '@/api/voucher'
import { useAppStore } from '@/stores/app'
import type { DocumentVO } from '@/types/document'
import type { VoucherVO, VoucherQueryRequest } from '@/types/voucher'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const document = ref<DocumentVO | null>(null)
const voucher = ref<VoucherVO | null>(null)

// 关联凭证弹窗
const linkDialogVisible = ref(false)
const voucherSearchLoading = ref(false)
const voucherList = ref<VoucherVO[]>([])
const selectedVoucher = ref<VoucherVO | null>(null)
const linkSearchForm = reactive({ voucherNo: '' })

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

function voucherStatusText(status: number): string {
  const map: Record<number, string> = { 0: '未审核', 1: '已审核', 2: '已过账' }
  return map[status] || '未知'
}

function voucherStatusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

function formatAmount(val: number | undefined): string {
  if (val === undefined || val === null || val === 0) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function isImage(url: string): boolean {
  return /\.(jpg|jpeg|png|gif|webp)$/i.test(url)
}

function isPdf(url: string): boolean {
  return /\.pdf$/i.test(url)
}

function openFile(url: string) {
  // BF-01 修复:校验 URL 协议白名单,防止 javascript:/data: 等协议在新窗口执行任意脚本(XSS)。
  // 仅允许 http/https 协议;非 http(s) URL 不打开并提示用户。
  if (!url || !/^https?:\/\//i.test(url)) {
    ElMessage.error('文件 URL 非法,无法打开')
    return
  }
  // noopener 防止新窗口通过 window.opener 引用操纵原窗口;noreferrer 不泄露 Referer
  window.open(url, '_blank', 'noopener,noreferrer')
}

function handleBack() {
  router.push('/document')
}

function handleEdit() {
  // 编辑弹窗在列表页内联实现,跳回列表页并通过 query 触发编辑
  router.push(`/document?edit=${route.params.id}`)
}

function goToVoucher(v: VoucherVO) {
  router.push(`/voucher/${v.id}`)
}

async function loadDocument() {
  const id = route.params.id as string
  if (!id) return

  loading.value = true
  try {
    const res = await documentApi.getById(Number(id))
    document.value = res.data

    if (res.data.voucherId) {
      await loadVoucher(res.data.voucherId)
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadVoucher(voucherId: number) {
  try {
    const res = await voucherApi.getById(voucherId)
    voucher.value = res.data
  } catch {
    // handled by interceptor
  }
}

function handleLinkVoucher() {
  linkSearchForm.voucherNo = ''
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
    voucherNo: linkSearchForm.voucherNo,
    pageNum: 1,
    pageSize: 50
  }

  voucherSearchLoading.value = true
  try {
    const res = await voucherApi.getPage(params)
    voucherList.value = res.data.list
  } catch {
    // handled by interceptor
  } finally {
    voucherSearchLoading.value = false
  }
}

function handleVoucherSelect(row: VoucherVO | null) {
  selectedVoucher.value = row
}

async function confirmLinkVoucher() {
  if (!selectedVoucher.value || !document.value) return

  try {
    await documentApi.linkVoucher(document.value.id, selectedVoucher.value.id)
    ElMessage.success('关联成功')
    linkDialogVisible.value = false
    await loadDocument()
  } catch {
    // handled by interceptor
  }
}

async function handleUnlinkVoucher() {
  if (!document.value) return

  await ElMessageBox.confirm('确定要取消关联凭证吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await documentApi.unlinkVoucher(document.value.id)
    ElMessage.success('取消关联成功')
    voucher.value = null
    if (document.value) {
      document.value.voucherId = 0
    }
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadDocument()
})
</script>

<style scoped lang="scss">
.document-detail-container {
  padding: 20px;
}

.el-page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card,
.attachment-card,
.voucher-card {
  margin-bottom: 16px;
}

.total-amount {
  font-weight: 600;
  color: #f56c6c;
}

.ocr-content {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.attachment-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.preview-image {
  max-width: 100%;
  max-height: 500px;
}

.pdf-preview {
  text-align: center;
  color: #909399;

  p {
    margin: 12px 0;
  }
}

.file-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.voucher-detail-table {
  margin-top: 16px;
}
</style>
