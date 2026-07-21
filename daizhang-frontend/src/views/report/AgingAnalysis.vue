<template>
  <div class="aging-container">
    <!-- 查询条件 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 220px"
          >
            <el-option
              v-for="item in appStore.accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="queryForm.asOfDate"
            type="date"
            placeholder="默认本月最后一天"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="queryForm.type">
            <el-radio-button value="receivable">应收</el-radio-button>
            <el-radio-button value="payable">应付</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleExport" :disabled="!tableData.length">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">应收总额</div>
        <div class="summary-value">{{ formatAmount(summary.totalReceivable) }}</div>
      </el-card>
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">应付总额</div>
        <div class="summary-value">{{ formatAmount(summary.totalPayable) }}</div>
      </el-card>
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">逾期应收</div>
        <div class="summary-value warning-text">{{ formatAmount(summary.overdueReceivable) }}</div>
      </el-card>
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">逾期应付</div>
        <div class="summary-value warning-text">{{ formatAmount(summary.overduePayable) }}</div>
      </el-card>
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">客户数</div>
        <div class="summary-value">{{ summary.customerCount }}</div>
      </el-card>
      <el-card class="summary-card" shadow="hover">
        <div class="summary-label">供应商数</div>
        <div class="summary-value">{{ summary.supplierCount }}</div>
      </el-card>
    </div>

    <!-- 账龄分析表 -->
    <el-card class="table-card">
      <template #header>
        <span>{{ queryForm.type === 'receivable' ? '应收账龄分析' : '应付账龄分析' }}</span>
        <span v-if="tableData.length" class="period-text">
          截止 {{ displayAsOfDate }} · 共 {{ tableData.length }} 个{{ queryForm.type === 'receivable' ? '客户' : '供应商' }}
        </span>
      </template>

      <el-table
        ref="tableRef"
        :data="tableDataWithTotal"
        v-loading="loading"
        border
        stripe
        :row-class-name="rowClassName"
        :span-method="spanMethod"
        :expand-row-keys="expandedRowKeys"
        row-key="rowKey"
        @expand-change="handleExpandChange"
        empty-text="暂无账龄数据,请选择账套和截止日期后查询"
      >
        <!-- 展开行:点击 180+ 天列后展开,显示该客户/供应商的明细 -->
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content" v-if="!row.isTotal">
              <el-descriptions :column="3" border size="small" class="expand-desc">
                <el-descriptions-item label="客户/供应商">
                  {{ row.customerName }}
                </el-descriptions-item>
                <el-descriptions-item label="总金额">
                  <span class="amount-text">{{ formatAmount(row.totalAmount) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="凭证数">
                  {{ row.voucherCount }} 条
                </el-descriptions-item>
                <el-descriptions-item label="最早凭证日期">
                  {{ row.oldestDate || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="最长逾期天数">
                  <span :class="{ 'danger-text': (row.oldestDays ?? 0) > 180 }">
                    {{ row.oldestDays ?? '-' }} 天
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="逾期占比">
                  {{ formatPercent(overdueRatio(row)) }}
                </el-descriptions-item>
              </el-descriptions>
              <el-alert
                v-if="(row.ageBuckets?.over180Days ?? 0) > 0"
                title="该客户/供应商存在 180 天以上未核销余额,坏账风险较高,建议优先催收并核查可回收性"
                type="error"
                :closable="false"
                show-icon
                class="risk-alert"
              />
              <div class="expand-hint">
                提示:点击 180+ 天 列单元格可展开/收起本明细。完整凭证明细请前往「明细账」按客户辅助核算查询。
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          prop="customerName"
          :label="queryForm.type === 'receivable' ? '客户名称' : '供应商名称'"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column prop="totalAmount" label="总金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="{ 'total-cell': !row.isTotal, 'total-bold': row.isTotal }">
              {{ formatAmount(row.totalAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="ageBuckets.within30Days" label="0-30天" width="120" align="right">
          <template #default="{ row }">
            <span>{{ formatAmount(row.ageBuckets?.within30Days) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ageBuckets.days31To60" label="31-60天" width="120" align="right">
          <template #default="{ row }">
            <span>{{ formatAmount(row.ageBuckets?.days31To60) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ageBuckets.days61To90" label="61-90天" width="120" align="right">
          <template #default="{ row }">
            <span class="bucket-orange">{{ formatAmount(row.ageBuckets?.days61To90) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ageBuckets.days91To180" label="91-180天" width="120" align="right">
          <template #default="{ row }">
            <span class="bucket-yellow">{{ formatAmount(row.ageBuckets?.days91To180) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ageBuckets.over180Days" label="180+天" width="130" align="right">
          <template #default="{ row }">
            <span
              class="bucket-red"
              :class="{ 'bucket-clickable': !row.isTotal && (row.ageBuckets?.over180Days ?? 0) > 0 }"
              @click="handleOver180Click(row)"
            >
              {{ formatAmount(row.ageBuckets?.over180Days) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="oldestDays" label="最长逾期天数" width="120" align="right">
          <template #default="{ row }">
            <span :class="{ 'danger-text': (row.oldestDays ?? 0) > 180 }">
              {{ row.oldestDays != null ? row.oldestDays + ' 天' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="voucherCount" label="凭证数" width="100" align="right" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { agingApi } from '@/api/report'
import type { AgingQueryParams } from '@/api/report'
import { useAppStore } from '@/stores/app'
import type { AgingItemVO, AgingSummaryVO } from '@/types/report'

const appStore = useAppStore()
const loading = ref(false)
const tableRef = ref()

/** 查询表单 */
interface QueryForm {
  accountSetId: number
  asOfDate: string | null
  type: 'receivable' | 'payable'
}
const queryForm = reactive<QueryForm>({
  accountSetId: appStore.currentAccountSetId ?? 0,
  asOfDate: defaultAsOfDate(),
  type: 'receivable'
})

/** 默认截止日期:本月最后一天 */
function defaultAsOfDate(): string {
  const now = new Date()
  // 本月最后一天
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  const y = lastDay.getFullYear()
  const m = String(lastDay.getMonth() + 1).padStart(2, '0')
  const d = String(lastDay.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 表格数据(单边:应收或应付) */
const tableData = ref<AgingItemVO[]>([])
/** 汇总数据(同时包含应收/应付双侧) */
const summary = ref<AgingSummaryVO>({
  totalReceivable: 0,
  totalPayable: 0,
  overdueReceivable: 0,
  overduePayable: 0,
  customerCount: 0,
  supplierCount: 0
})

/** 展开的行 key 列表 */
const expandedRowKeys = ref<string[]>([])

/** 表格行 key(为合计行预留特殊 key 避免冲突) */
function rowKey(row: AgingItemVO | TotalRow): string {
  if (isTotalRow(row)) {
    return row.rowKey
  }
  return String(row.customerId)
}

/** 合计行类型 */
interface TotalRow {
  isTotal: true
  rowKey: string
  customerName: string
  totalAmount: number
  ageBuckets: {
    within30Days: number
    days31To60: number
    days61To90: number
    days91To180: number
    over180Days: number
  }
  oldestDate: string
  oldestDays: number | null
  voucherCount: number
}

/** 类型守卫:判断是否为合计行 */
function isTotalRow(row: AgingItemVO | TotalRow): row is TotalRow {
  return typeof row === 'object' && row !== null && 'isTotal' in row && row.isTotal === true
}

/** 计算合计行 */
function buildTotalRow(rows: AgingItemVO[]): TotalRow {
  const total: TotalRow = {
    isTotal: true,
    rowKey: '__total__',
    customerName: '合计',
    totalAmount: 0,
    ageBuckets: {
      within30Days: 0,
      days31To60: 0,
      days61To90: 0,
      days91To180: 0,
      over180Days: 0
    },
    oldestDate: '',
    oldestDays: null,
    voucherCount: 0
  }
  for (const r of rows) {
    total.totalAmount += r.totalAmount ?? 0
    total.ageBuckets.within30Days += r.ageBuckets?.within30Days ?? 0
    total.ageBuckets.days31To60 += r.ageBuckets?.days31To60 ?? 0
    total.ageBuckets.days61To90 += r.ageBuckets?.days61To90 ?? 0
    total.ageBuckets.days91To180 += r.ageBuckets?.days91To180 ?? 0
    total.ageBuckets.over180Days += r.ageBuckets?.over180Days ?? 0
    total.voucherCount += r.voucherCount ?? 0
  }
  return total
}

/** 表格数据 + 合计行 */
const tableDataWithTotal = computed<Array<AgingItemVO | TotalRow>>(() => {
  if (!tableData.value.length) {
    return []
  }
  return [...tableData.value, buildTotalRow(tableData.value)]
})

/** 展示用的截止日期(后端实际生效的) */
const displayAsOfDate = ref<string>('')

/** 构造查询参数 */
function buildParams(): AgingQueryParams | null {
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return null
  }
  const params: AgingQueryParams = {
    accountSetId: queryForm.accountSetId
  }
  if (queryForm.asOfDate) {
    params.asOfDate = queryForm.asOfDate
  }
  return params
}

/** 加载数据:同时拉取当前类型(应收/应付)明细和汇总 */
async function loadData() {
  const params = buildParams()
  if (!params) {
    return
  }
  loading.value = true
  try {
    // 并发拉取明细和汇总
    const isReceivable = queryForm.type === 'receivable'
    const [detailRes, summaryRes] = await Promise.all([
      isReceivable
        ? agingApi.getReceivableAging(params)
        : agingApi.getPayableAging(params),
      agingApi.getAgingSummary(params)
    ])
    tableData.value = detailRes.data || []
    summary.value = summaryRes.data || summary.value
    // 显示用的截止日期
    displayAsOfDate.value = queryForm.asOfDate || defaultAsOfDate()
    // 清空展开状态(数据已变化)
    expandedRowKeys.value = []
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
 * 180+ 天单元格点击事件:展开/收起该行
 * 仅对数据行生效,合计行不响应
 */
function handleOver180Click(row: AgingItemVO | TotalRow) {
  if (isTotalRow(row)) {
    return
  }
  // 仅当 180+ 天金额 > 0 时才允许展开
  const over180 = row.ageBuckets?.over180Days ?? 0
  if (over180 <= 0) {
    return
  }
  // 调用 Element Plus 表格实例方法切换展开
  const table = tableRef.value
  if (table && typeof table.toggleRowExpansion === 'function') {
    table.toggleRowExpansion(row)
  }
}

/** 展开行变化时同步 expandedRowKeys(用于受控展开) */
function handleExpandChange(_row: AgingItemVO | TotalRow, expandedRows: Array<AgingItemVO | TotalRow>) {
  expandedRowKeys.value = expandedRows
    .filter(r => !isTotalRow(r))
    .map(r => rowKey(r))
}

/** 行样式:合计行加粗灰底 */
function rowClassName({ row }: { row: AgingItemVO | TotalRow }): string {
  if (isTotalRow(row)) {
    return 'aging-total-row'
  }
  return ''
}

/** 合计行合并展开列(合计行不支持展开) */
function spanMethod({
  row,
  columnIndex
}: {
  row: AgingItemVO | TotalRow
  columnIndex: number
}): [number, number] {
  if (isTotalRow(row) && columnIndex === 0) {
    // 合计行的展开列占位但禁用展开
    return [1, 1]
  }
  return [1, 1]
}

/** 逾期占比(31 天以上 / 总金额) */
function overdueRatio(row: AgingItemVO): number {
  const total = row.totalAmount ?? 0
  if (!total) {
    return 0
  }
  const overdue = (row.ageBuckets?.days31To60 ?? 0)
    + (row.ageBuckets?.days61To90 ?? 0)
    + (row.ageBuckets?.days91To180 ?? 0)
    + (row.ageBuckets?.over180Days ?? 0)
  return overdue / total
}

/** 格式化金额:千分位 + 两位小数,null/undefined 显示为空 */
function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) {
    return ''
  }
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化百分比 */
function formatPercent(val: number): string {
  if (!Number.isFinite(val)) {
    return '-'
  }
  return `${(val * 100).toFixed(2)}%`
}

/** 导出 Excel */
function handleExport() {
  if (!tableData.value.length) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const typeLabel = queryForm.type === 'receivable' ? '应收' : '应付'
  const entityLabel = queryForm.type === 'receivable' ? '客户' : '供应商'
  const asOf = displayAsOfDate.value || defaultAsOfDate()

  // 表头
  const header = [
    `${entityLabel}名称`,
    '总金额',
    '0-30天',
    '31-60天',
    '61-90天',
    '91-180天',
    '180+天',
    '最长逾期天数',
    '凭证数'
  ]
  const dataRows: (string | number)[][] = tableData.value.map(row => [
    row.customerName ?? '',
    row.totalAmount ?? 0,
    row.ageBuckets?.within30Days ?? 0,
    row.ageBuckets?.days31To60 ?? 0,
    row.ageBuckets?.days61To90 ?? 0,
    row.ageBuckets?.days91To180 ?? 0,
    row.ageBuckets?.over180Days ?? 0,
    row.oldestDays ?? '',
    row.voucherCount ?? 0
  ])
  // 合计行
  const totalRow = buildTotalRow(tableData.value)
  dataRows.push([
    totalRow.customerName,
    totalRow.totalAmount,
    totalRow.ageBuckets.within30Days,
    totalRow.ageBuckets.days31To60,
    totalRow.ageBuckets.days61To90,
    totalRow.ageBuckets.days91To180,
    totalRow.ageBuckets.over180Days,
    '',
    totalRow.voucherCount
  ])

  const aoa: (string | number)[][] = [
    [`${typeLabel}账龄分析表 截止 ${asOf}`],
    [],
    header,
    ...dataRows
  ]
  const ws = XLSX.utils.aoa_to_sheet(aoa)
  ws['!cols'] = [
    { wch: 25 },
    { wch: 16 },
    { wch: 14 },
    { wch: 14 },
    { wch: 14 },
    { wch: 14 },
    { wch: 14 },
    { wch: 14 },
    { wch: 10 }
  ]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, `${typeLabel}账龄分析`)
  XLSX.writeFile(wb, `${typeLabel}账龄分析_${asOf}.xlsx`)
}

onMounted(async () => {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = appStore.currentAccountSetId ?? list[0].id
    }
    if (queryForm.accountSetId) {
      loadData()
    }
  } catch {
    // 拦截器已处理
  }
})
</script>

<style scoped lang="scss">
.aging-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  text-align: center;

  .summary-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }

  .summary-value {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .warning-text {
    color: #e6a23c;
  }
}

.table-card {
  .period-text {
    margin-left: 16px;
    color: #909399;
    font-size: 14px;
  }
}

// 合计行:加粗 + 灰底
:deep(.aging-total-row) {
  td {
    background-color: #f5f7fa !important;
    font-weight: 700;
    color: #303133;
  }
}

// 0-30 天:正常(默认色)
// 31-60 天:关注(默认色,不加特殊样式)
// 61-90 天:预警 → 橙色
.bucket-orange {
  color: #e6a23c;
  font-weight: 600;
}

// 91-180 天:逾期 → 黄色加粗
.bucket-yellow {
  color: #b88200;
  background-color: #fdf6ec;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
}

// 180+ 天:坏账风险 → 红色加粗
.bucket-red {
  color: #f56c6c;
  background-color: #fef0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 700;
}

// 180+ 天列可点击时的样式
.bucket-clickable {
  cursor: pointer;
  display: inline-block;

  &:hover {
    background-color: #fde2e2;
    text-decoration: underline;
  }
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.amount-text {
  font-weight: 600;
  color: #303133;
}

.total-cell {
  font-weight: 600;
}

.total-bold {
  font-weight: 700;
  color: #303133;
}

.expand-content {
  padding: 12px 20px;
  background-color: #fafafa;

  .expand-desc {
    margin-bottom: 12px;
  }

  .risk-alert {
    margin-bottom: 8px;
  }

  .expand-hint {
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}

// 响应式:小屏单列
@media (max-width: 1200px) {
  .summary-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
