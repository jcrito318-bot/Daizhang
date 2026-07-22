<template>
  <div class="bank-reconciliation-container">
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="银行账户" required>
          <el-input v-model="filterForm.bankAccount" placeholder="请输入银行账户" style="width: 180px" />
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="filterForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="filterForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLoad">加载数据</el-button>
          <el-button type="success" :loading="autoMatchLoading" @click="handleAutoMatch">
            <el-icon><MagicStick /></el-icon>自动匹配
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" class="match-area">
      <!-- 未匹配银行流水 -->
      <el-col :xs="24" :lg="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span>未匹配银行流水 ({{ unmatchedTransactions.length }})</span>
              <el-button
                type="primary"
                size="small"
                :disabled="selectedTransactions.length === 0 || !selectedVoucher"
                @click="handleManualMatch"
              >
                手动匹配选中项
              </el-button>
            </div>
          </template>

          <el-table
            :data="unmatchedTransactions"
            v-loading="transactionsLoading"
            border
            stripe
            height="500"
            @selection-change="handleTransactionSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="transactionDate" label="交易日期" width="110" />
            <el-table-column prop="transactionNo" label="流水号" width="140" show-overflow-tooltip />
            <el-table-column label="类型" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.transactionType === 1 ? 'success' : 'danger'" size="small">
                  {{ row.transactionType === 1 ? '收' : '付' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="120" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.transactionType === 1 ? '#67c23a' : '#f56c6c' }">
                  {{ formatAmount(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="counterparty" label="对方单位" min-width="150" show-overflow-tooltip />
            <el-table-column prop="summary" label="摘要" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <!-- 未匹配凭证 -->
      <el-col :xs="24" :lg="12">
        <el-card class="list-card">
          <template #header>
            <div class="card-header">
              <span>未匹配凭证 ({{ unmatchedVouchers.length }})</span>
              <span v-if="selectedVoucher" class="selected-info">
                已选凭证: <el-tag type="primary">{{ selectedVoucher.voucherNo }}</el-tag>
              </span>
            </div>
          </template>

          <el-table
            :data="unmatchedVouchers"
            v-loading="vouchersLoading"
            border
            stripe
            height="500"
            highlight-current-row
            @current-change="handleVoucherSelect"
          >
            <el-table-column prop="voucherNo" label="凭证号" width="140" />
            <el-table-column prop="voucherDate" label="凭证日期" width="110" />
            <el-table-column label="摘要" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.details && row.details.length > 0 ? row.details[0].summary : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="totalDebit" label="借方金额" width="120" align="right">
              <template #default="{ row }">
                {{ formatAmount(row.totalDebit) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalCredit" label="贷方金额" width="120" align="right">
              <template #default="{ row }">
                {{ formatAmount(row.totalCredit) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 匹配结果 -->
    <el-card v-if="matchResults.length > 0" class="result-card">
      <template #header>
        <span>匹配结果</span>
      </template>
      <el-table :data="matchResults" border stripe>
        <el-table-column prop="transactionNo" label="流水号" width="160" />
        <el-table-column prop="transactionDate" label="交易日期" width="110" />
        <el-table-column prop="amount" label="流水金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="voucherNo" label="匹配凭证号" width="140" />
        <el-table-column prop="voucherAmount" label="凭证金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.voucherAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="差异" width="120" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.diff === 0 ? '#67c23a' : '#f56c6c' }">
              {{ formatAmount(row.diff) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="matchType" label="匹配方式" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { bankApi } from '@/api/bank'
import { voucherApi } from '@/api/voucher'
import { useAppStore } from '@/stores/app'
import type { BankTransactionVO } from '@/types/bank'
import type { VoucherVO, VoucherQueryRequest } from '@/types/voucher'

interface MatchResult {
  transactionNo: string
  transactionDate: string
  amount: number
  voucherNo: string
  voucherAmount: number
  diff: number
  matchType: string
}

const appStore = useAppStore()

const filterForm = reactive({
  bankAccount: '',
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

const transactionsLoading = ref(false)
const vouchersLoading = ref(false)
const autoMatchLoading = ref(false)

const unmatchedTransactions = ref<BankTransactionVO[]>([])
const unmatchedVouchers = ref<VoucherVO[]>([])
const selectedTransactions = ref<BankTransactionVO[]>([])
const selectedVoucher = ref<VoucherVO | null>(null)
const matchResults = ref<MatchResult[]>([])

const hasData = computed(() => unmatchedTransactions.value.length > 0 || unmatchedVouchers.value.length > 0)

function formatAmount(val: number): string {
  if (val === undefined || val === null) return ''
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function handleLoad() {
  if (!filterForm.bankAccount) {
    ElMessage.warning('请输入银行账户')
    return
  }
  if (!appStore.currentAccountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  await Promise.all([loadUnmatchedTransactions(), loadUnmatchedVouchers()])
  matchResults.value = []
}

async function loadUnmatchedTransactions() {
  transactionsLoading.value = true
  try {
    const res = await bankApi.getTransactionPage({
      accountSetId: appStore.currentAccountSetId || 0,
      bankAccount: filterForm.bankAccount,
      matchedStatus: 0,
      startDate: `${filterForm.year}-${String(filterForm.month).padStart(2, '0')}-01`,
      endDate: getMonthEndDate(filterForm.year, filterForm.month),
      pageNum: 1,
      pageSize: 1000
    })
    unmatchedTransactions.value = res.data.list
  } catch {
    // handled by interceptor
  } finally {
    transactionsLoading.value = false
  }
}

async function loadUnmatchedVouchers() {
  vouchersLoading.value = true
  try {
    const params: VoucherQueryRequest = {
      accountSetId: appStore.currentAccountSetId || 0,
      year: filterForm.year,
      month: filterForm.month,
      pageNum: 1,
      pageSize: 1000
    }
    const res = await voucherApi.getPage(params)
    // 当前按"已审核未过账"过滤作为候选(已过账凭证通常已完成对账)
    // 后端应提供 matchedBankTransactionId 等未匹配标记字段以精确过滤
    unmatchedVouchers.value = res.data.list.filter(v => v.status === 1)
  } catch {
    // handled by interceptor
  } finally {
    vouchersLoading.value = false
  }
}

function getMonthEndDate(year: number, month: number): string {
  const lastDay = new Date(year, month, 0).getDate()
  return `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
}

function handleTransactionSelectionChange(selection: BankTransactionVO[]) {
  selectedTransactions.value = selection
}

function handleVoucherSelect(row: VoucherVO | null) {
  selectedVoucher.value = row
}

async function handleAutoMatch() {
  if (!filterForm.bankAccount) {
    ElMessage.warning('请输入银行账户')
    return
  }
  if (!appStore.currentAccountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }

  autoMatchLoading.value = true
  try {
    const res = await bankApi.autoMatch({
      accountSetId: appStore.currentAccountSetId || 0,
      bankAccount: filterForm.bankAccount,
      year: filterForm.year,
      month: filterForm.month
    })
    ElMessage.success(`自动匹配完成，成功匹配 ${res.data} 对记录`)
    await handleLoad()
  } catch {
    // handled by interceptor
  } finally {
    autoMatchLoading.value = false
  }
}

async function handleManualMatch() {
  if (selectedTransactions.value.length === 0) {
    ElMessage.warning('请选择要匹配的银行流水')
    return
  }
  if (!selectedVoucher.value) {
    ElMessage.warning('请选择要匹配的凭证')
    return
  }

  try {
    await bankApi.manualMatch({
      accountSetId: appStore.currentAccountSetId || 0,
      transactionIds: selectedTransactions.value.map(t => t.id),
      voucherId: selectedVoucher.value.id
    })
    ElMessage.success('手动匹配成功')

    // 记录匹配结果用于展示
    const newResults: MatchResult[] = selectedTransactions.value.map(t => ({
      transactionNo: t.transactionNo,
      transactionDate: t.transactionDate,
      amount: t.amount,
      voucherNo: selectedVoucher.value!.voucherNo,
      voucherAmount: selectedVoucher.value!.totalDebit,
      diff: Math.abs(t.amount - selectedVoucher.value!.totalDebit),
      matchType: '手动'
    }))
    matchResults.value = [...newResults, ...matchResults.value]

    await handleLoad()
  } catch {
    // handled by interceptor
  }
}
</script>

<style scoped lang="scss">
.bank-reconciliation-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.match-area {
  margin-bottom: 16px;
}

.list-card {
  height: 100%;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .selected-info {
    font-size: 13px;
    color: #606266;
  }
}

.result-card {
  margin-bottom: 16px;
}
</style>
