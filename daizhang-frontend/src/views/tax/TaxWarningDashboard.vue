<template>
  <div class="tax-warning-dashboard">
    <!-- 顶部:账套 + 年月选择 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 220px"
            @change="handleAccountSetChange"
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
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 130px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="queryForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
        </el-form-item>
        <el-form-item v-if="isAdmin">
          <el-button @click="handleOpenBenchmarkDialog">
            <el-icon><Setting /></el-icon>行业基准
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 空数据占位 -->
    <el-empty v-if="!warning" description="请选择账套和年月后点击查询" />

    <template v-else>
      <!-- 上半部:增值税 / 企业所得税 两个大卡片 -->
      <el-row :gutter="16" class="metric-row">
        <el-col :xs="24" :md="12">
          <div class="metric-card" :class="metricClass(warning.vatWarningLevel)">
            <div class="metric-header">
              <span class="metric-title">增值税税负率</span>
              <el-tag :type="levelTagType(warning.vatWarningLevel)" effect="dark" size="small">
                {{ levelText(warning.vatWarningLevel) }}
              </el-tag>
            </div>
            <div class="metric-value">
              {{ formatPercent(warning.vatActualRate) }}
            </div>
            <div class="metric-meta">
              <div class="metric-meta-row">
                <span class="metric-label">行业基准</span>
                <span class="metric-num">{{ formatPercent(warning.vatBenchmarkRate) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">预警区间</span>
                <span class="metric-num">
                  {{ formatPercent(warning.vatWarningLow) }} ~ {{ formatPercent(warning.vatWarningHigh) }}
                </span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">实缴增值税</span>
                <span class="metric-num">{{ formatAmount(warning.vatActualAmount) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">不含税销售收入</span>
                <span class="metric-num">{{ formatAmount(warning.salesRevenue) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">所属行业</span>
                <span class="metric-num">{{ warning.industryName }}({{ warning.industryCode }})</span>
              </div>
            </div>
            <!-- 进度条可视化:实际值在预警区间中的位置 -->
            <div class="metric-progress">
              <div class="metric-progress-track">
                <div
                  class="metric-progress-range"
                  :style="rangeStyle(warning.vatWarningLow, warning.vatWarningHigh)"
                />
                <div
                  class="metric-progress-marker"
                  :class="metricClass(warning.vatWarningLevel)"
                  :style="markerStyle(warning.vatActualRate)"
                />
              </div>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :md="12">
          <div class="metric-card" :class="metricClass(warning.eitWarningLevel)">
            <div class="metric-header">
              <span class="metric-title">企业所得税税负率</span>
              <el-tag :type="levelTagType(warning.eitWarningLevel)" effect="dark" size="small">
                {{ levelText(warning.eitWarningLevel) }}
              </el-tag>
            </div>
            <div class="metric-value">
              {{ formatPercent(warning.eitActualRate) }}
            </div>
            <div class="metric-meta">
              <div class="metric-meta-row">
                <span class="metric-label">行业基准</span>
                <span class="metric-num">{{ formatPercent(warning.eitBenchmarkRate) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">预警区间</span>
                <span class="metric-num">
                  {{ formatPercent(warning.eitWarningLow) }} ~ {{ formatPercent(warning.eitWarningHigh) }}
                </span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">实缴所得税</span>
                <span class="metric-num">{{ formatAmount(warning.eitActualAmount) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">营业收入</span>
                <span class="metric-num">{{ formatAmount(warning.salesRevenue) }}</span>
              </div>
              <div class="metric-meta-row">
                <span class="metric-label">所属行业</span>
                <span class="metric-num">{{ warning.industryName }}({{ warning.industryCode }})</span>
              </div>
            </div>
            <div class="metric-progress">
              <div class="metric-progress-track">
                <div
                  class="metric-progress-range"
                  :style="rangeStyle(warning.eitWarningLow, warning.eitWarningHigh)"
                />
                <div
                  class="metric-progress-marker"
                  :class="metricClass(warning.eitWarningLevel)"
                  :style="markerStyle(warning.eitActualRate)"
                />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 中部:全年趋势折线图 -->
      <el-card class="trend-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>{{ queryForm.year }}年税负率趋势</span>
            <el-radio-group v-model="trendMetric" size="small">
              <el-radio-button label="vat">增值税</el-radio-button>
              <el-radio-button label="eit">企业所得税</el-radio-button>
              <el-radio-button label="both">同时显示</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="trend-chart" />
      </el-card>

      <!-- 下部:预警建议列表 -->
      <el-card class="advice-card" shadow="never">
        <template #header>
          <span>预警明细与建议</span>
        </template>
        <div v-loading="loading" class="advice-content">
          <template v-if="warning.warnings.length > 0">
            <div class="advice-section-title">
              <el-icon class="advice-section-icon" color="#F56C6C"><WarningFilled /></el-icon>
              <span>预警项</span>
            </div>
            <ul class="advice-list">
              <li
                v-for="(item, idx) in warning.warnings"
                :key="`w-${idx}`"
                class="advice-item danger"
              >
                <el-tag type="danger" effect="plain" size="small">预警</el-tag>
                <span class="advice-text">{{ item }}</span>
              </li>
            </ul>
          </template>
          <template v-else>
            <el-alert
              title="当月无税负异常预警,各项指标处于正常区间"
              type="success"
              :closable="false"
              show-icon
            />
          </template>

          <div class="advice-section-title advice-section-title-suggest">
            <el-icon class="advice-section-icon" color="#409EFF"><ChatDotRound /></el-icon>
            <span>建议</span>
          </div>
          <ul class="advice-list">
            <li
              v-for="(item, idx) in warning.suggestions"
              :key="`s-${idx}`"
              class="advice-item"
              :class="suggestionClass(item)"
            >
              <el-tag
                :type="suggestionTagType(item)"
                effect="plain"
                size="small"
              >
                {{ suggestionLabel(item) }}
              </el-tag>
              <span class="advice-text">{{ item }}</span>
            </li>
          </ul>
        </div>
      </el-card>
    </template>

    <!-- 行业基准管理弹窗(ADMIN only) -->
    <el-dialog v-model="benchmarkDialogVisible" title="行业税负率基准" width="900px">
      <el-table :data="benchmarks" border stripe size="small">
        <el-table-column prop="industryCode" label="行业代码" width="100" />
        <el-table-column prop="industryName" label="行业名称" width="160" />
        <el-table-column label="增值税基准" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.vatBenchmarkRate) }}</template>
        </el-table-column>
        <el-table-column label="增值税下限" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.vatWarningLow) }}</template>
        </el-table-column>
        <el-table-column label="增值税上限" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.vatWarningHigh) }}</template>
        </el-table-column>
        <el-table-column label="所得税基准" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.eitBenchmarkRate) }}</template>
        </el-table-column>
        <el-table-column label="所得税下限" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.eitWarningLow) }}</template>
        </el-table-column>
        <el-table-column label="所得税上限" width="100" align="right">
          <template #default="{ row }">{{ formatPercent(row.eitWarningHigh) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEditBenchmark(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 编辑基准弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑行业税负率基准" width="520px" append-to-body>
      <el-form
        v-if="editForm"
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="140px"
      >
        <el-form-item label="行业">
          <span>{{ editForm.industryName }}({{ editForm.industryCode }})</span>
        </el-form-item>
        <el-divider content-position="left">增值税税负率</el-divider>
        <el-form-item label="基准" prop="vatBenchmarkRate">
          <el-input-number
            v-model="editForm.vatBenchmarkRate"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.vatBenchmarkRate) }}</span>
        </el-form-item>
        <el-form-item label="下限预警" prop="vatWarningLow">
          <el-input-number
            v-model="editForm.vatWarningLow"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.vatWarningLow) }}</span>
        </el-form-item>
        <el-form-item label="上限预警" prop="vatWarningHigh">
          <el-input-number
            v-model="editForm.vatWarningHigh"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.vatWarningHigh) }}</span>
        </el-form-item>
        <el-divider content-position="left">企业所得税税负率</el-divider>
        <el-form-item label="基准" prop="eitBenchmarkRate">
          <el-input-number
            v-model="editForm.eitBenchmarkRate"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.eitBenchmarkRate) }}</span>
        </el-form-item>
        <el-form-item label="下限预警" prop="eitWarningLow">
          <el-input-number
            v-model="editForm.eitWarningLow"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.eitWarningLow) }}</span>
        </el-form-item>
        <el-form-item label="上限预警" prop="eitWarningHigh">
          <el-input-number
            v-model="editForm.eitWarningHigh"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            style="width: 200px"
          />
          <span class="form-hint">{{ formatPercent(editForm.eitWarningHigh) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveBenchmark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Search, Setting, WarningFilled, ChatDotRound } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { warningApi } from '@/api/tax'
import type { TaxWarningVO, TaxTrendVO, TaxBenchmark, TaxBenchmarkUpdateRequest, TaxWarningLevel } from '@/types/tax'

const appStore = useAppStore()
const userStore = useUserStore()

// ===== 查询条件 =====
const queryForm = reactive({
  accountSetId: appStore.currentAccountSetId as number | null,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

// ===== 数据 =====
const loading = ref(false)
const saving = ref(false)
const warning = ref<TaxWarningVO | null>(null)
const trendList = ref<TaxTrendVO[]>([])
const benchmarks = ref<TaxBenchmark[]>([])

// ===== 图表 =====
const trendChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
const trendMetric = ref<'vat' | 'eit' | 'both'>('vat')

// ===== 行业基准管理弹窗 =====
const benchmarkDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance | null>(null)
const editForm = ref<(TaxBenchmarkUpdateRequest & { id: number; industryCode: string; industryName: string }) | null>(null)

const editRules: FormRules = {
  vatBenchmarkRate: [{ required: true, message: '请输入增值税基准', trigger: 'blur' }],
  vatWarningLow: [{ required: true, message: '请输入增值税下限', trigger: 'blur' }],
  vatWarningHigh: [{ required: true, message: '请输入增值税上限', trigger: 'blur' }],
  eitBenchmarkRate: [{ required: true, message: '请输入所得税基准', trigger: 'blur' }],
  eitWarningLow: [{ required: true, message: '请输入所得税下限', trigger: 'blur' }],
  eitWarningHigh: [{ required: true, message: '请输入所得税上限', trigger: 'blur' }]
}

// ===== 计算属性 =====
const isAdmin = computed(() => userStore.userInfo?.roles?.includes('ADMIN') === true)

// 当前选中月份对应的基准(用于趋势图基准线)
const currentBenchmark = computed(() => {
  if (!warning.value) {
    return { vat: 0, eit: 0 }
  }
  return {
    vat: warning.value.vatBenchmarkRate,
    eit: warning.value.eitBenchmarkRate
  }
})

// ===== 工具函数 =====

/** 格式化税负率(0.0250 -> 2.50%) */
function formatPercent(rate: number | null | undefined): string {
  if (rate === null || rate === undefined || Number.isNaN(rate)) {
    return '0.00%'
  }
  return (rate * 100).toFixed(2) + '%'
}

/** 格式化金额 */
function formatAmount(amount: number | null | undefined): string {
  if (amount === null || amount === undefined || Number.isNaN(amount)) {
    return '-'
  }
  return '¥ ' + amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 等级文字 */
function levelText(level: TaxWarningLevel): string {
  if (level === 'danger') return '异常偏低'
  if (level === 'warning') return '偏高'
  return '正常'
}

/** 等级标签类型 */
function levelTagType(level: TaxWarningLevel): 'success' | 'warning' | 'danger' {
  if (level === 'danger') return 'danger'
  if (level === 'warning') return 'warning'
  return 'success'
}

/** 卡片样式类 */
function metricClass(level: TaxWarningLevel): string {
  return `metric-${level}`
}

/**
 * 计算进度条中预警区间(0~max 的相对位置)的样式:
 * 进度条横向范围取 [0, max(low,high,actual)*1.2],预警区间在其中的位置
 */
function rangeStyle(low: number, high: number): Record<string, string> {
  const max = Math.max(low, high) * 1.2 || 0.05
  const startPct = (low / max) * 100
  const widthPct = ((high - low) / max) * 100
  return {
    left: `${startPct}%`,
    width: `${widthPct}%`
  }
}

/** 计算实际值标记在进度条中的位置 */
function markerStyle(actual: number): Record<string, string> {
  const max = Math.max(
    warning.value?.vatWarningHigh ?? 0,
    warning.value?.vatWarningLow ?? 0,
    warning.value?.eitWarningHigh ?? 0,
    warning.value?.eitWarningLow ?? 0,
    actual
  ) * 1.2 || 0.05
  const pct = Math.min(100, (actual / max) * 100)
  return { left: `${pct}%` }
}

/** 建议项的样式类:根据文本中是否包含关键词判定红/黄/绿 */
function suggestionClass(text: string): string {
  if (text.includes('低于') || text.includes('偏低') || text.includes('异常')) return 'danger'
  if (text.includes('高于') || text.includes('偏高')) return 'warning'
  return 'normal'
}

/** 建议项标签类型 */
function suggestionTagType(text: string): 'success' | 'warning' | 'danger' {
  const c = suggestionClass(text)
  if (c === 'danger') return 'danger'
  if (c === 'warning') return 'warning'
  return 'success'
}

/** 建议项标签文字 */
function suggestionLabel(text: string): string {
  const c = suggestionClass(text)
  if (c === 'danger') return '风险'
  if (c === 'warning') return '关注'
  return '正常'
}

// ===== 数据加载 =====

async function loadWarning() {
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const [warningRes, trendRes] = await Promise.all([
      warningApi.getWarning(queryForm.accountSetId, queryForm.year, queryForm.month),
      warningApi.getTrend(queryForm.accountSetId, queryForm.year)
    ])
    warning.value = warningRes.data
    trendList.value = trendRes.data || []
    await nextTick()
    renderTrendChart()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadWarning()
}

function handleAccountSetChange() {
  loadWarning()
}

// ===== 图表渲染 =====

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const months = trendList.value.map(t => `${t.month}月`)
  const vatRates = trendList.value.map(t => (t.vatRate !== null ? +(t.vatRate * 100).toFixed(4) : null))
  const eitRates = trendList.value.map(t => (t.eitRate !== null ? +(t.eitRate * 100).toFixed(4) : null))
  const vatBenchmark = +(currentBenchmark.value.vat * 100).toFixed(4)
  const eitBenchmark = +(currentBenchmark.value.eit * 100).toFixed(4)

  const showVat = trendMetric.value === 'vat' || trendMetric.value === 'both'
  const showEit = trendMetric.value === 'eit' || trendMetric.value === 'both'

  const series: echarts.SeriesOption[] = []
  if (showVat) {
    series.push({
      name: '增值税税负率',
      type: 'line',
      data: vatRates,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 2 }
    })
    series.push({
      name: '增值税基准',
      type: 'line',
      data: months.map(() => vatBenchmark),
      symbol: 'none',
      lineStyle: { type: 'dashed', color: '#409EFF', width: 1, opacity: 0.5 }
    })
  }
  if (showEit) {
    series.push({
      name: '企业所得税税负率',
      type: 'line',
      data: eitRates,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      itemStyle: { color: '#67C23A' },
      lineStyle: { width: 2 }
    })
    series.push({
      name: '所得税基准',
      type: 'line',
      data: months.map(() => eitBenchmark),
      symbol: 'none',
      lineStyle: { type: 'dashed', color: '#67C23A', width: 1, opacity: 0.5 }
    })
  }

  const option: echarts.EChartsCoreOption = {
    tooltip: {
      trigger: 'axis',
      valueFormatter: (val: unknown) => {
        if (val === null || val === undefined) return '无数据'
        const num = typeof val === 'number' ? val : Number(val)
        if (Number.isNaN(num)) return '无数据'
        return num.toFixed(2) + '%'
      }
    },
    legend: {
      data: series.map(s => s.name as string),
      top: 0
    },
    grid: {
      top: 40,
      left: 50,
      right: 30,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: months,
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '税负率(%)',
      axisLabel: {
        formatter: (val: number) => val.toFixed(2) + '%'
      }
    },
    series
  }

  trendChart.setOption(option, true)
}

function handleResize() {
  trendChart?.resize()
}

// 监听趋势图指标切换
watch(trendMetric, () => {
  renderTrendChart()
})

// ===== 行业基准管理 =====

async function handleOpenBenchmarkDialog() {
  try {
    const res = await warningApi.listBenchmarks()
    benchmarks.value = res.data || []
    benchmarkDialogVisible.value = true
  } catch {
    // 拦截器已提示
  }
}

function handleEditBenchmark(row: TaxBenchmark) {
  editForm.value = {
    id: row.id,
    industryCode: row.industryCode,
    industryName: row.industryName,
    vatBenchmarkRate: row.vatBenchmarkRate,
    vatWarningLow: row.vatWarningLow,
    vatWarningHigh: row.vatWarningHigh,
    eitBenchmarkRate: row.eitBenchmarkRate,
    eitWarningLow: row.eitWarningLow,
    eitWarningHigh: row.eitWarningHigh
  }
  editDialogVisible.value = true
}

async function handleSaveBenchmark() {
  if (!editFormRef.value || !editForm.value) return
  const form = editForm.value
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const { id, industryCode, industryName, ...rest } = form
      void industryCode
      void industryName
      await warningApi.updateBenchmark(id, rest)
      ElMessage.success('保存成功')
      editDialogVisible.value = false
      // 刷新基准列表
      const res = await warningApi.listBenchmarks()
      benchmarks.value = res.data || []
      // 同步刷新预警(行业基准可能影响等级)
      await loadWarning()
    } catch {
      // 拦截器已提示
    } finally {
      saving.value = false
    }
  })
}

// ===== 生命周期 =====

onMounted(async () => {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = appStore.currentAccountSetId ?? list[0].id
    }
    if (queryForm.accountSetId) {
      await loadWarning()
    }
  } catch {
    // 拦截器已提示
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped lang="scss">
.tax-warning-dashboard {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border-left: 6px solid #67c23a;
  height: 100%;
  transition: box-shadow 0.2s;

  &.metric-danger {
    border-left-color: #f56c6c;
    background: linear-gradient(to right, #fff5f5 0%, #fff 30%);
  }

  &.metric-warning {
    border-left-color: #e6a23c;
    background: linear-gradient(to right, #fdf6ec 0%, #fff 30%);
  }

  &.metric-normal {
    border-left-color: #67c23a;
    background: linear-gradient(to right, #f0f9eb 0%, #fff 30%);
  }
}

.metric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.metric-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 12px;
}

.metric-meta {
  font-size: 13px;
  color: #606266;
}

.metric-meta-row {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
}

.metric-label {
  color: #909399;
}

.metric-num {
  font-weight: 500;
  color: #303133;
}

.metric-progress {
  margin-top: 14px;
}

.metric-progress-track {
  position: relative;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: visible;
}

.metric-progress-range {
  position: absolute;
  top: 0;
  height: 8px;
  background: rgba(103, 194, 58, 0.3);
  border-radius: 4px;
}

.metric-progress-marker {
  position: absolute;
  top: -4px;
  width: 4px;
  height: 16px;
  border-radius: 2px;
  transform: translateX(-50%);
  background: #67c23a;

  &.metric-danger {
    background: #f56c6c;
  }

  &.metric-warning {
    background: #e6a23c;
  }
}

.trend-card {
  margin-bottom: 16px;
}

.trend-chart {
  width: 100%;
  height: 360px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.advice-card {
  margin-bottom: 16px;
}

.advice-content {
  min-height: 80px;
}

.advice-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 12px 0 8px;

  &.advice-section-title-suggest {
    margin-top: 20px;
  }
}

.advice-section-icon {
  font-size: 16px;
}

.advice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.advice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: 4px;
  background: #f4f4f5;
  font-size: 13px;
  color: #303133;

  &.danger {
    background: #fef0f0;
  }

  &.warning {
    background: #fdf6ec;
  }

  &.normal {
    background: #f0f9eb;
  }
}

.advice-text {
  flex: 1;
  line-height: 1.6;
}

.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}
</style>
