<template>
  <div class="bank-balance-adjustment-container">
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
          <el-button type="primary" @click="handleGenerate">
            <el-icon><Document /></el-icon>生成调节表
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="reconciliationList.length > 0" class="list-card">
      <template #header>
        <div class="card-header">
          <span>银行余额调节表</span>
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>导出Excel
          </el-button>
        </div>
      </template>

      <el-table :data="reconciliationList" v-loading="loading" border stripe>
        <el-table-column prop="bankAccount" label="银行账户" width="180" />
        <el-table-column label="调节期间" width="120" align="center">
          <template #default="{ row }">
            {{ row.year }}年{{ row.month }}月
          </template>
        </el-table-column>
        <el-table-column prop="bookBalance" label="企业账面余额" width="150" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.bookBalance >= 0 ? '#303133' : '#f56c6c' }">
              {{ formatAmount(row.bookBalance) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="bankBalance" label="银行账面余额" width="150" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.bankBalance >= 0 ? '#303133' : '#f56c6c' }">
              {{ formatAmount(row.bankBalance) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异金额" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.difference === 0 ? '#67c23a' : '#f56c6c' }">
              {{ formatAmount(row.difference) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="unreconciledItems" label="未达账项数" width="110" align="center" />
        <el-table-column prop="statusName" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reconciledByName" label="调节人" width="100" />
        <el-table-column prop="reconciledDate" label="调节日期" width="120" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadReconciliationList"
        @current-change="loadReconciliationList"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="调节表详情" width="900px">
      <div v-if="currentReconciliation" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="银行账户">{{ currentReconciliation.bankAccount }}</el-descriptions-item>
          <el-descriptions-item label="调节期间">{{ currentReconciliation.year }}年{{ currentReconciliation.month }}月</el-descriptions-item>
          <el-descriptions-item label="企业账面余额">
            <span :style="{ color: currentReconciliation.bookBalance >= 0 ? '#303133' : '#f56c6c' }">
              {{ formatAmount(currentReconciliation.bookBalance) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="银行账面余额">
            <span :style="{ color: currentReconciliation.bankBalance >= 0 ? '#303133' : '#f56c6c' }">
              {{ formatAmount(currentReconciliation.bankBalance) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="差异金额">
            <span :style="{ color: currentReconciliation.difference === 0 ? '#67c23a' : '#f56c6c' }">
              {{ formatAmount(currentReconciliation.difference) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="未达账项数">{{ currentReconciliation.unreconciledItems }}</el-descriptions-item>
          <el-descriptions-item label="调节人">{{ currentReconciliation.reconciledByName }}</el-descriptions-item>
          <el-descriptions-item label="调节日期">{{ currentReconciliation.reconciledDate }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentReconciliation.remark || '无' }}</el-descriptions-item>
        </el-descriptions>

        <div class="unreconciled-section">
          <h4>未达账项明细</h4>
          <el-table
            :data="currentReconciliation.unreconciledTransactions"
            border
            stripe
            max-height="400"
          >
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
            <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
            <el-table-column label="匹配状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.matchedStatus === 0 ? 'warning' : 'success'" size="small">
                  {{ row.matchedStatusName }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { bankApi } from '@/api/bank'
import { useAppStore } from '@/stores/app'
import type { BankReconciliationVO } from '@/types/bank'

const appStore = useAppStore()

const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const reconciliationList = ref<BankReconciliationVO[]>([])

const filterForm = reactive({
  bankAccount: '',
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

const detailDialogVisible = ref(false)
const currentReconciliation = ref<BankReconciliationVO | null>(null)

function formatAmount(val: number): string {
  if (val === undefined || val === null) return ''
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function handleGenerate() {
  if (!filterForm.bankAccount) {
    ElMessage.warning('请输入银行账户')
    return
  }
  if (!appStore.currentAccountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }

  loading.value = true
  try {
    await bankApi.generateReconciliation({
      accountSetId: appStore.currentAccountSetId || 0,
      bankAccount: filterForm.bankAccount,
      year: filterForm.year,
      month: filterForm.month
    })
    ElMessage.success('调节表生成成功')
    await loadReconciliationList()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadReconciliationList() {
  if (!filterForm.bankAccount) {
    return
  }
  if (!appStore.currentAccountSetId) {
    return
  }

  loading.value = true
  try {
    const res = await bankApi.getReconciliationPage({
      accountSetId: appStore.currentAccountSetId || 0,
      bankAccount: filterForm.bankAccount,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    reconciliationList.value = res.data.list
    total.value = res.data.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleViewDetail(row: BankReconciliationVO) {
  currentReconciliation.value = row
  detailDialogVisible.value = true
}

function handleExport() {
  if (reconciliationList.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const exportData = reconciliationList.value.map(item => ({
    '银行账户': item.bankAccount,
    '调节期间': `${item.year}年${item.month}月`,
    '企业账面余额': item.bookBalance,
    '银行账面余额': item.bankBalance,
    '差异金额': item.difference,
    '未达账项数': item.unreconciledItems,
    '状态': item.statusName,
    '调节人': item.reconciledByName,
    '调节日期': item.reconciledDate,
    '备注': item.remark || ''
  }))

  const worksheet = XLSX.utils.json_to_sheet(exportData)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '银行余额调节表')

  // 设置列宽
  worksheet['!cols'] = [
    { wch: 18 }, // 银行账户
    { wch: 12 }, // 调节期间
    { wch: 15 }, // 企业账面余额
    { wch: 15 }, // 银行账面余额
    { wch: 13 }, // 差异金额
    { wch: 11 }, // 未达账项数
    { wch: 10 }, // 状态
    { wch: 10 }, // 调节人
    { wch: 12 }, // 调节日期
    { wch: 30 }  // 备注
  ]

  XLSX.writeFile(workbook, `银行余额调节表_${filterForm.year}年${filterForm.month}月.xlsx`)
  ElMessage.success('导出成功')
}

onMounted(() => {
  // 页面加载时不自动加载数据，需要用户输入银行账户后点击生成
})
</script>

<style scoped lang="scss">
.bank-balance-adjustment-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.list-card {
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

.detail-content {
  .unreconciled-section {
    margin-top: 20px;

    h4 {
      margin-bottom: 12px;
      color: #303133;
      font-size: 15px;
      font-weight: 600;
    }
  }
}
</style>
