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
            <span
              v-if="row.currentAmount"
              class="drillable-cell"
              @dblclick="handleDrillDown(row, 'current')"
            >
              {{ row.currentAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="yearAmount" label="本年累计金额" width="160" align="right">
          <template #default="{ row }">
            <span
              v-if="row.yearAmount"
              class="drillable-cell"
              @dblclick="handleDrillDown(row, 'year')"
            >
              {{ row.yearAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
            </span>
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

    <!-- 钻取凭证对话框 -->
    <DrillDownDialog
      v-model:visible="drillVisible"
      :account-set-id="queryForm.accountSetId"
      :subject-code="drillSubjectCode"
      :year="queryForm.year"
      :month="queryForm.month"
      :amount="drillAmount"
      :direction="drillDirection"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { reportApi } from '@/api/report'
import { useAppStore } from '@/stores/app'
import DrillDownDialog from '@/components/DrillDownDialog.vue'
import { inferDirectionFromSubjectCode, createDebounce } from '@/utils/drillDown'
import type { IncomeStatementItem, IncomeStatementVO, ReportQueryRequest } from '@/types/report'
import type { DrillDownDirection } from '@/types/ledger'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<IncomeStatementItem[]>([])
const reportData = ref<IncomeStatementVO | null>(null)

const queryForm = reactive<ReportQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

// ===== 钻取相关状态 =====
const drillVisible = ref(false)
const drillSubjectCode = ref('')
const drillAmount = ref(0)
const drillDirection = ref<DrillDownDirection>('debit')

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

/**
 * 双击金额单元格 → 钻取凭证
 * 使用防抖 300ms 避免误触(连续双击只触发一次)
 * 
 * @param row 报表行数据
 * @param field 'current'=本期金额, 'year'=本年累计金额
 */
const handleDrillDown = createDebounce((row: IncomeStatementItem, field: 'current' | 'year') => {
  // 无科目编码或金额为 0 时不允许钻取
  if (!row || !row.code) {
    return
  }
  const amount = field === 'current' ? row.currentAmount : row.yearAmount
  if (!amount || amount <= 0) {
    return
  }
  drillSubjectCode.value = row.code
  drillAmount.value = amount
  // 利润表项目为损益类科目:根据科目编码首位推断借贷方向
  // 5xxx 收入类 → 贷方;6xxx/7xxx 成本费用类 → 借方
  drillDirection.value = inferDirectionFromSubjectCode(row.code)
  drillVisible.value = true
}, 300)

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

// 可钻取金额单元格:双击高亮提示
.drillable-cell {
  cursor: pointer;
  display: inline-block;
  width: 100%;

  &:hover {
    color: #409eff;
    text-decoration: underline;
  }
}
</style>
