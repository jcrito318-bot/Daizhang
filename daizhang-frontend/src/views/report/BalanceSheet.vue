<template>
  <div class="balance-sheet-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 200px"
          >
            <el-option
              v-for="item in accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="queryForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <span>资产负债表</span>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe show-summary :summary-method="getSummary">
        <el-table-column prop="rowNo" label="行次" width="60" align="center" />
        <el-table-column prop="name" label="项目" min-width="250" />
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="beginningBalance" label="期初余额" width="160" align="right">
          <template #default="{ row }">
            {{ row.beginningBalance ? row.beginningBalance.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="endingBalance" label="期末余额" width="160" align="right">
          <template #default="{ row }">
            {{ row.endingBalance ? row.endingBalance.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="total-info" v-if="reportData">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="资产总计">{{ formatAmount(reportData.totalAssets) }}</el-descriptions-item>
          <el-descriptions-item label="负债总计">{{ formatAmount(reportData.totalLiabilities) }}</el-descriptions-item>
          <el-descriptions-item label="所有者权益总计">{{ formatAmount(reportData.totalEquity) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { reportApi } from '@/api/report'
import { accountSetApi } from '@/api/accountset'
import { useAppStore } from '@/stores/app'
import type { BalanceSheetItem, BalanceSheetVO, ReportQueryRequest } from '@/types/report'
import type { AccountSetVO } from '@/types/accountset'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<BalanceSheetItem[]>([])
const reportData = ref<BalanceSheetVO | null>(null)
const accountSetList = ref<AccountSetVO[]>([])

const queryForm = reactive<ReportQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

function formatAmount(val: number): string {
  if (!val) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getSummary({ data }: { data: BalanceSheetItem[] }) {
  const sums: string[] = []
  const beginTotal = data.reduce((sum, row) => sum + (row.beginningBalance || 0), 0)
  const endTotal = data.reduce((sum, row) => sum + (row.endingBalance || 0), 0)
  sums.push('合计')
  sums.push('')
  sums.push('')
  sums.push(formatAmount(beginTotal))
  sums.push(formatAmount(endTotal))
  return sums
}

async function loadData() {
  if (!queryForm.accountSetId) return
  loading.value = true
  try {
    const res = await reportApi.getBalanceSheet(queryForm)
    reportData.value = res.data
    tableData.value = res.data.items
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

onMounted(async () => {
  try {
    const res = await accountSetApi.getList()
    accountSetList.value = res.data
    if (accountSetList.value.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = accountSetList.value[0].id
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.balance-sheet-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.total-info {
  margin-top: 16px;
}
</style>
