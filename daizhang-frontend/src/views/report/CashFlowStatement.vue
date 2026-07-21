<template>
  <div class="cash-flow-container">
    <!-- 查询条件 -->
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
          <el-button @click="handleExport" :disabled="!reportData">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 勾稽校验警告 -->
    <el-alert
      v-if="reportData && reportData.balanceCheck === false"
      title="勾稽校验未通过"
      :description="balanceCheckDesc"
      type="error"
      show-icon
      :closable="false"
      class="balance-alert"
    />

    <!-- 现金流量表 -->
    <el-card class="table-card">
      <template #header>
        <span>现金流量表（直接法）</span>
        <span v-if="reportData" class="period-text">
          {{ reportData.year }}年{{ reportData.month }}月
        </span>
      </template>

      <el-table
        :data="tableRows"
        v-loading="loading"
        border
        stripe
        :row-class-name="rowClassName"
        :span-method="spanMethod"
      >
        <el-table-column prop="itemName" label="项目" min-width="350">
          <template #default="{ row }">
            <span :class="{ 'row-section': row.rowType === 'section', 'row-bold': row.bold }">
              {{ row.itemName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="本年金额" width="200" align="right">
          <template #default="{ row }">
            <span
              v-if="row.amount !== null && row.amount !== undefined"
              :class="{ 'row-bold': row.bold, 'drillable-cell': row.rowType === 'item' }"
              @dblclick="handleDrillDown(row)"
            >
              {{ formatAmount(row.amount) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { reportApi } from '@/api/report'
import { useAppStore } from '@/stores/app'
import DrillDownDialog from '@/components/DrillDownDialog.vue'
import { getCashFlowDrillConfig, createDebounce } from '@/utils/drillDown'
import type { CashFlowItemVO, CashFlowStatementVO, ReportQueryRequest } from '@/types/report'
import type { DrillDownDirection } from '@/types/ledger'

const appStore = useAppStore()
const loading = ref(false)
const reportData = ref<CashFlowStatementVO | null>(null)

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

/**
 * 表格行类型：区块标题 / 明细项 / 小计 / 汇总
 */
type RowType = 'section' | 'item' | 'subtotal' | 'summary'

interface CashFlowRow {
  rowType: RowType
  itemCode: string
  itemName: string
  amount: number | null
  bold: boolean
}

/**
 * 从明细项列表中按 itemCode 查找金额
 */
function findItemAmount(items: CashFlowItemVO[], code: string): number | null {
  const item = items.find(i => i.itemCode === code)
  if (item && item.amount !== null && item.amount !== undefined) {
    return item.amount
  }
  return null
}

/**
 * 构建表格行数据：按经营活动、投资活动、筹资活动、汇率变动、余额汇总分块。
 */
const tableRows = computed<CashFlowRow[]>(() => {
  if (!reportData.value) {
    return []
  }
  const data = reportData.value
  const items = data.items || []
  const rows: CashFlowRow[] = []

  // 一、经营活动产生的现金流量
  rows.push(sectionRow('一、经营活动产生的现金流量'))
  // 流入项
  rows.push(...itemRows(items, ['SALES_RECEIPTS', 'TAX_REFUNDS', 'OTHER_OPERATING_RECEIPTS']))
  rows.push(subtotalRow('经营活动现金流入小计', data.operatingInflow))
  // 流出项
  rows.push(...itemRows(items, ['PURCHASE_PAYMENTS', 'EMPLOYEE_PAYMENTS', 'TAX_PAYMENTS', 'OTHER_OPERATING_PAYMENTS']))
  rows.push(subtotalRow('经营活动现金流出小计', data.operatingOutflow))
  // 净额
  rows.push(subtotalRow('经营活动产生的现金流量净额', data.operatingNetCashFlow, true))

  // 二、投资活动产生的现金流量
  rows.push(sectionRow('二、投资活动产生的现金流量'))
  rows.push(...itemRows(items, ['INVESTMENT_RECEIPTS', 'INVESTMENT_INCOME', 'ASSET_DISPOSAL', 'OTHER_INVESTING_RECEIPTS']))
  rows.push(subtotalRow('投资活动现金流入小计', data.investingInflow))
  rows.push(...itemRows(items, ['ASSET_PURCHASE', 'INVESTMENT_PAYMENTS', 'OTHER_INVESTING_PAYMENTS']))
  rows.push(subtotalRow('投资活动现金流出小计', data.investingOutflow))
  rows.push(subtotalRow('投资活动产生的现金流量净额', data.investingNetCashFlow, true))

  // 三、筹资活动产生的现金流量
  rows.push(sectionRow('三、筹资活动产生的现金流量'))
  rows.push(...itemRows(items, ['FINANCING_RECEIPTS', 'LOAN_RECEIPTS', 'OTHER_FINANCING_RECEIPTS']))
  rows.push(subtotalRow('筹资活动现金流入小计', data.financingInflow))
  rows.push(...itemRows(items, ['DEBT_REPAYMENT', 'DISTRIBUTION_PAYMENTS', 'OTHER_FINANCING_PAYMENTS']))
  rows.push(subtotalRow('筹资活动现金流出小计', data.financingOutflow))
  rows.push(subtotalRow('筹资活动产生的现金流量净额', data.financingNetCashFlow, true))

  // 四、汇率变动对现金的影响
  rows.push(sectionRow('四、汇率变动对现金的影响'))
  rows.push(...itemRows(items, ['EXCHANGE_EFFECT']))

  // 五、现金及现金等价物净增加额
  rows.push(sectionRow('五、现金及现金等价物净增加额'))
  rows.push(summaryRow('现金及现金等价物净增加额', data.netIncreaseInCash, true))
  rows.push(summaryRow('期初现金及现金等价物余额', data.beginningCashBalance, false))
  rows.push(summaryRow('期末现金及现金等价物余额', data.endingCashBalance, true))

  return rows
})

/** 创建区块标题行 */
function sectionRow(name: string): CashFlowRow {
  return { rowType: 'section', itemCode: '', itemName: name, amount: null, bold: false }
}

/** 创建明细项行 */
function itemRows(items: CashFlowItemVO[], codes: string[]): CashFlowRow[] {
  return codes.map(code => {
    const item = items.find(i => i.itemCode === code)
    return {
      rowType: 'item' as RowType,
      itemCode: code,
      itemName: item ? item.itemName : code,
      amount: findItemAmount(items, code),
      bold: false
    }
  })
}

/** 创建小计行 */
function subtotalRow(name: string, amount: number, bold = false): CashFlowRow {
  return { rowType: 'subtotal', itemCode: '', itemName: name, amount, bold }
}

/** 创建汇总行 */
function summaryRow(name: string, amount: number, bold = false): CashFlowRow {
  return { rowType: 'summary', itemCode: '', itemName: name, amount, bold }
}

/** 行样式：区块标题行加粗、灰底 */
function rowClassName({ row }: { row: CashFlowRow }): string {
  if (row.rowType === 'section') {
    return 'cf-section-row'
  }
  if (row.bold) {
    return 'cf-bold-row'
  }
  return ''
}

/** 合并区块标题行的金额列（标题行不显示金额） */
function spanMethod({ row }: { row: CashFlowRow }): [number, number] {
  if (row.rowType === 'section') {
    // 合并整行：跨2列
    return [1, 2]
  }
  return [1, 1]
}

/** 格式化金额：千分位 + 两位小数 */
function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) {
    return ''
  }
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 勾稽校验描述 */
const balanceCheckDesc = computed(() => {
  if (!reportData.value) {
    return ''
  }
  const d = reportData.value
  const expected = (d.endingCashBalance ?? 0) - (d.beginningCashBalance ?? 0)
  const actual = d.netIncreaseInCash ?? 0
  const diff = Math.abs(actual - expected)
  return `净增加额（${formatAmount(actual)}）≠ 期末余额（${formatAmount(d.endingCashBalance)}）- 期初余额（${formatAmount(d.beginningCashBalance)}）= ${formatAmount(expected)}，差异 ${formatAmount(diff)} 元。可能存在未归类的现金流量项目或内部调拨凭证。`
})

/** 加载数据 */
async function loadData() {
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await reportApi.getCashFlowStatement(queryForm)
    reportData.value = res.data
  } catch {
    // 拦截器已处理错误提示
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
 * 现金流量表项目本身不直接对应单一科目,这里通过 itemCode 映射到
 * 主现金科目(1001 库存现金及其下级)+ 借贷方向(流入=借方,流出=贷方)。
 * 仅明细项行(item)可钻取,小计/汇总/区块标题行忽略。
 *
 * @param row 表格行数据
 */
const handleDrillDown = createDebounce((row: CashFlowRow) => {
  // 仅明细项可钻取(小计/汇总/区块标题行忽略)
  if (!row || row.rowType !== 'item') {
    return
  }
  // 金额为空或为 0 时不钻取
  if (row.amount === null || row.amount === undefined || row.amount <= 0) {
    return
  }
  // 无 itemCode 无法映射(防御性校验)
  if (!row.itemCode) {
    return
  }
  const config = getCashFlowDrillConfig(row.itemCode)
  drillSubjectCode.value = config.subjectCode
  drillAmount.value = row.amount
  drillDirection.value = config.direction
  drillVisible.value = true
}, 300)

/** 导出 Excel */
function handleExport() {
  if (!reportData.value || tableRows.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const data = reportData.value

  // 构建导出数据：标题行 + 空行 + 表头 + 数据行
  const aoa: (string | number)[][] = [
    [`现金流量表 ${data.year}年${data.month}月`],
    [],
    ['项目', '金额'],
    ...tableRows.value.map(row => [
      row.itemName,
      row.amount !== null && row.amount !== undefined ? row.amount : ''
    ])
  ]

  const ws = XLSX.utils.aoa_to_sheet(aoa)
  ws['!cols'] = [{ wch: 45 }, { wch: 18 }]

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '现金流量表')

  XLSX.writeFile(wb, `现金流量表_${data.year}年${data.month}月.xlsx`)
}

onMounted(async () => {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = appStore.currentAccountSetId || list[0].id
    }
    // 自动加载一次数据
    if (queryForm.accountSetId) {
      loadData()
    }
  } catch {
    // 拦截器已处理
  }
})
</script>

<style scoped lang="scss">
.cash-flow-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.balance-alert {
  margin-bottom: 16px;
}

.table-card {
  .period-text {
    margin-left: 16px;
    color: #909399;
    font-size: 14px;
  }
}

// 区块标题行样式
:deep(.cf-section-row) {
  td {
    background-color: #f5f7fa !important;
    font-weight: 700;
    color: #303133;
  }
}

// 加粗行样式（净额行）
:deep(.cf-bold-row) {
  td {
    font-weight: 700;
  }
}

.row-section {
  font-weight: 700;
}

.row-bold {
  font-weight: 700;
}

// 可钻取金额单元格:双击高亮提示(仅明细项)
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
