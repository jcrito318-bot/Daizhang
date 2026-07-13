<template>
  <div class="voucher-detail-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>凭证详情</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="3" border class="voucher-info">
        <el-descriptions-item label="凭证字">{{ voucher.voucherWordName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="凭证号">{{ voucher.voucherNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="凭证日期">{{ voucher.voucherDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="附件数">{{ voucher.attachmentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="制单人">{{ voucher.createByName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(voucher.status)">
            {{ getStatusText(voucher.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="voucher.details" border class="detail-table">
        <el-table-column prop="lineNo" label="行号" width="60" align="center" />
        <el-table-column prop="summary" label="摘要" min-width="200" />
        <el-table-column label="科目" min-width="200">
          <template #default="{ row }">
            {{ row.subjectCode }} {{ row.subjectName }}
          </template>
        </el-table-column>
        <el-table-column label="借方金额" width="140" align="right">
          <template #default="{ row }">
            {{ row.debit ? formatAmount(row.debit) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="贷方金额" width="140" align="right">
          <template #default="{ row }">
            {{ row.credit ? formatAmount(row.credit) : '' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="total-summary">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="借方合计">
            <span style="font-weight: bold; color: #409eff">{{ formatAmount(totalDebit) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="贷方合计">
            <span style="font-weight: bold; color: #67c23a">{{ formatAmount(totalCredit) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { voucherApi } from '@/api/voucher'
import { formatAmount as formatAmountUtil } from '@/utils/format'

interface VoucherDetail {
  lineNo: number
  summary: string
  subjectCode: string
  subjectName: string
  debit: number
  credit: number
}

interface VoucherVO {
  id: number
  voucherWordName: string
  voucherNo: string
  voucherDate: string
  attachmentCount: number
  createByName: string
  status: number
  details: VoucherDetail[]
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const voucher = ref<VoucherVO>({
  id: 0,
  voucherWordName: '',
  voucherNo: '',
  voucherDate: '',
  attachmentCount: 0,
  createByName: '',
  status: 0,
  details: []
})

const totalDebit = computed(() => voucher.value.details.reduce((sum, d) => sum + (d.debit || 0), 0))
const totalCredit = computed(() => voucher.value.details.reduce((sum, d) => sum + (d.credit || 0), 0))

function formatAmount(val: number | string | null | undefined): string {
  // 委托统一工具,确保 null/undefined 不抛 NPE
  return formatAmountUtil(val)
}

function getStatusType(status: number): string {
  const types: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return types[status] || 'info'
}

function getStatusText(status: number): string {
  const texts: Record<number, string> = { 0: '未审核', 1: '已审核', 2: '已过账' }
  return texts[status] || '未知'
}

function handleBack() {
  router.push('/voucher')
}

async function loadVoucherDetail(id: number) {
  loading.value = true
  try {
    const res = await voucherApi.getById(id)
    voucher.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const id = route.params.id as string
  if (id) {
    loadVoucherDetail(Number(id))
  }
})
</script>

<style scoped lang="scss">
.voucher-detail-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.voucher-info {
  margin-bottom: 20px;
}

.detail-table {
  margin-bottom: 16px;
}

.total-summary {
  margin-top: 16px;
}
</style>
