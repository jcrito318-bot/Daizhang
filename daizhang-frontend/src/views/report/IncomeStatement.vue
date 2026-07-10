<template>
  <div class="income-statement-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 200px"
          >
            <el-option
              v-for="item in appStore.accountSetList"
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
        <span>利润表</span>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe show-summary :summary-method="getSummary">
        <el-table-column prop="rowNo" label="行次" width="60" align="center" />
        <el-table-column prop="name" label="项目" min-width="250" />
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="currentAmount" label="本期金额" width="160" align="right">
          <template #default="{ row }">
            {{ row.currentAmount ? row.currentAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="yearAmount" label="本年累计金额" width="160" align="right">
          <template #default="{ row }">
            {{ row.yearAmount ? row.yearAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="total-info" v-if="reportData">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="营业收入合计">{{ formatAmount(reportData.totalRevenue) }}</el-descriptions-item>
          <el-descriptions-item label="营业支出合计">{{ formatAmount(reportData.totalExpense) }}</el-descriptions-item>
          <el-descriptions-item label="净利润">{{ formatAmount(reportData.netProfit) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { reportApi } from '@/api/report'
import { useAppStore } from '@/stores/app'
import type { IncomeStatementItem, IncomeStatementVO, ReportQueryRequest } from '@/types/report'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<IncomeStatementItem[]>([])
const reportData = ref<IncomeStatementVO | null>(null)

const queryForm = reactive<ReportQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

function formatAmount(val: number): string {
  if (!val) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getSummary({ data }: { data: IncomeStatementItem[] }) {
  const sums: string[] = []
  const currentTotal = data.reduce((sum, row) => sum + (row.currentAmount || 0), 0)
  const yearTotal = data.reduce((sum, row) => sum + (row.yearAmount || 0), 0)
  sums.push('合计')
  sums.push('')
  sums.push('')
  sums.push(formatAmount(currentTotal))
  sums.push(formatAmount(yearTotal))
  return sums
}

async function loadData() {
  if (!queryForm.accountSetId) return
  loading.value = true
  try {
    const res = await reportApi.getIncomeStatement(queryForm)
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
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = appStore.currentAccountSetId || list[0].id
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.income-statement-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.total-info {
  margin-top: 16px;
}
</style>
