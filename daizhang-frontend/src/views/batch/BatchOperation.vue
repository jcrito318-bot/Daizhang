<template>
  <div class="batch-operation-container">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span>跨账套批量操作</span>
          <el-button type="info" plain @click="historyDialogVisible = true; loadHistory()">
            <el-icon><Clock /></el-icon>
            操作历史
          </el-button>
        </div>
      </template>
      <el-alert
        title="代账批量操作"
        type="info"
        :closable="false"
        description="选择账套与期间后，可一次性对多个账套执行审核凭证、结账、生成报表。单个账套失败不影响其他账套。"
        show-icon
      />
    </el-card>

    <el-card class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ==================== 批量审核凭证 ==================== -->
        <el-tab-pane label="批量审核凭证" name="voucher-audit">
          <template #label>
            <el-icon><Check /></el-icon>
            <span>批量审核凭证</span>
          </template>
          <div class="tab-tip">
            审核所选账套在指定期间内的所有未审核凭证（制单人≠审核人、借贷平衡、期间未结账等校验同单账套审核）。
          </div>
        </el-tab-pane>

        <!-- ==================== 批量结账 ==================== -->
        <el-tab-pane label="批量结账" name="period-close">
          <template #label>
            <el-icon><Lock /></el-icon>
            <span>批量结账</span>
          </template>
          <div class="tab-tip">
            对所选账套执行期末结账。若某账套存在未审核/未过账凭证或试算不平衡，该账套结账将失败。
          </div>
        </el-tab-pane>

        <!-- ==================== 批量生成报表 ==================== -->
        <el-tab-pane label="批量生成报表" name="report-generate">
          <template #label>
            <el-icon><DataAnalysis /></el-icon>
            <span>批量生成报表</span>
          </template>
          <div class="tab-tip">
            批量校验所选账套指定期间的财务报表可正常生成（资产负债表、利润表、现金流量表、科目余额表）。
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 通用选择区：账套 + 期间 -->
      <el-form :inline="true" class="select-form">
        <el-form-item label="选择账套">
          <el-button text type="primary" @click="selectAllAccountSets">全选</el-button>
          <el-button text @click="clearAccountSetSelection">清空</el-button>
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="queryYear" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="queryMonth" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <!-- 报表类型选择(仅报表 Tab) -->
        <el-form-item v-if="activeTab === 'report-generate'" label="报表类型">
          <el-checkbox-group v-model="selectedReportTypes">
            <el-checkbox label="balance-sheet">资产负债表</el-checkbox>
            <el-checkbox label="income-statement">利润表</el-checkbox>
            <el-checkbox label="cash-flow-statement">现金流量表</el-checkbox>
            <el-checkbox label="subject-balance">科目余额表</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="selectedAccountSetIds.length === 0"
            @click="executeBatch"
          >
            <el-icon><VideoPlay /></el-icon>
            执行批量操作
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 账套列表(多选) -->
      <el-table
        ref="accountSetTableRef"
        :data="accountSetList"
        v-loading="accountSetLoading"
        border
        stripe
        max-height="320"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" reserve-selection />
        <el-table-column prop="code" label="账套编码" width="140" />
        <el-table-column prop="name" label="账套名称" min-width="180" />
        <el-table-column prop="companyName" label="企业名称" min-width="200" />
        <el-table-column prop="taxpayerType" label="纳税人类型" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 进度提示 -->
      <div v-if="loading" class="progress-area">
        <el-progress :percentage="100" status="success" :indeterminate="true" :duration="2" />
        <span class="progress-text">正在处理 {{ selectedAccountSetIds.length }} 个账套，请稍候...</span>
      </div>

      <!-- 结果区 -->
      <div v-if="batchResponse" class="result-area">
        <el-divider content-position="left">
          <span class="result-title">执行结果</span>
        </el-divider>
        <el-row :gutter="16" class="result-summary">
          <el-col :span="8">
            <el-statistic title="总账套数" :value="batchResponse.totalCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="成功" :value="batchResponse.successCount" :value-style="{ color: '#67c23a' }" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="失败/部分失败" :value="batchResponse.failCount" :value-style="{ color: '#f56c6c' }" />
          </el-col>
        </el-row>

        <div class="result-toolbar">
          <el-button type="success" plain size="small" @click="exportResults">
            <el-icon><Download /></el-icon>
            导出结果
          </el-button>
        </div>

        <el-table :data="batchResponse.results" border stripe>
          <el-table-column prop="accountSetName" label="账套名称" min-width="180">
            <template #default="{ row }">
              {{ row.accountSetName || ('账套#' + row.accountSetId) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="结果详情" min-width="320">
            <template #default="{ row }">
              <span :class="{ 'fail-message': row.status === 'failed' }">{{ row.message }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'failed' || row.status === 'partial'"
                type="warning"
                link
                size="small"
                @click="showErrorDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- ==================== 操作历史对话框 ==================== -->
    <el-dialog v-model="historyDialogVisible" title="批量操作历史" width="900px" destroy-on-close>
      <el-form :inline="true" class="history-filter">
        <el-form-item label="操作类型">
          <el-select v-model="historyQuery.operationType" placeholder="全部" clearable style="width: 180px">
            <el-option label="批量审核凭证" value="voucher-audit" />
            <el-option label="批量结账" value="period-close" />
            <el-option label="批量生成报表" value="report-generate" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="historyQuery.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="historyQuery.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadHistory">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="historyList" border stripe v-loading="historyLoading">
        <el-table-column prop="operation" label="操作" min-width="140" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="status" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="110" align="right" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createTime" label="操作时间" min-width="170" />
        <el-table-column label="详情" width="90" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showHistoryDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="historyTotal > 0"
        class="history-pagination"
        v-model:current-page="historyQuery.pageNum"
        v-model:page-size="historyQuery.pageSize"
        :total="historyTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadHistory"
        @current-change="loadHistory"
      />
    </el-dialog>

    <!-- 错误详情对话框 -->
    <el-dialog v-model="errorDetailVisible" title="失败详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账套">{{ errorDetail?.accountSetName || ('账套#' + errorDetail?.accountSetId) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(errorDetail?.status)">{{ statusLabel(errorDetail?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原因">{{ errorDetail?.message }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 历史详情对话框 -->
    <el-dialog v-model="historyDetailVisible" title="操作日志详情" width="700px">
      <el-descriptions :column="1" border v-if="historyDetail">
        <el-descriptions-item label="操作">{{ historyDetail.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ historyDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="historyDetail.status === 1 ? 'success' : 'danger'">
            {{ historyDetail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="方法">{{ historyDetail.method }}</el-descriptions-item>
        <el-descriptions-item label="参数">
          <pre class="json-pre">{{ formatJson(historyDetail.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="historyDetail.errorMsg" label="错误信息">
          <span class="fail-message">{{ historyDetail.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="IP">{{ historyDetail.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ historyDetail.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="时间">{{ historyDetail.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import * as XLSX from 'xlsx'
import { accountSetApi } from '@/api/accountset'
import { batchApi } from '@/api/batch'
import type {
  BatchOperationResponse,
  BatchOperationResultVO,
  BatchOperationStatus,
  SysOperationLog,
  BatchHistoryQueryRequest
} from '@/api/batch'
import type { AccountSetVO } from '@/types/accountset'

// ==================== 公共状态 ====================
const activeTab = ref<'voucher-audit' | 'period-close' | 'report-generate'>('voucher-audit')
const accountSetList = ref<AccountSetVO[]>([])
const accountSetLoading = ref(false)
const selectedAccountSetIds = ref<number[]>([])
const accountSetTableRef = ref<TableInstance>()

const now = new Date()
const queryYear = ref(now.getFullYear())
const queryMonth = ref(now.getMonth() + 1)

// 报表类型选择(报表 Tab 专用)
const selectedReportTypes = ref<string[]>(['balance-sheet', 'income-statement'])

const loading = ref(false)
const batchResponse = ref<BatchOperationResponse | null>(null)

// ==================== 账套加载与选择 ====================
async function loadAccountSets() {
  accountSetLoading.value = true
  try {
    const res = await accountSetApi.getList()
    // 仅显示启用的账套
    accountSetList.value = res.data.filter((item: AccountSetVO) => item.status === 1)
  } catch {
    // 错误由拦截器处理
  } finally {
    accountSetLoading.value = false
  }
}

function handleSelectionChange(selection: AccountSetVO[]) {
  selectedAccountSetIds.value = selection.map((item) => item.id)
}

function selectAllAccountSets() {
  accountSetTableRef.value?.toggleAllSelection()
}

function clearAccountSetSelection() {
  accountSetTableRef.value?.clearSelection()
}

// 切换 Tab 时保留选择与期间(共享),清空结果区避免误读
function handleTabChange() {
  batchResponse.value = null
}

// ==================== 执行批量操作 ====================
async function executeBatch() {
  if (selectedAccountSetIds.value.length === 0) {
    ElMessage.warning('请至少选择一个账套')
    return
  }
  if (activeTab.value === 'report-generate' && selectedReportTypes.value.length === 0) {
    ElMessage.warning('请至少选择一种报表类型')
    return
  }

  const confirmText = buildConfirmText()
  try {
    await ElMessageBox.confirm(confirmText, '批量操作确认', {
      confirmButtonText: '确定执行',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }

  loading.value = true
  batchResponse.value = null
  try {
    let res: BatchOperationResponse
    if (activeTab.value === 'voucher-audit') {
      res = (await batchApi.batchAuditVoucher({ items: buildAuditItems() })).data
    } else if (activeTab.value === 'period-close') {
      res = (await batchApi.batchClosePeriod({ items: buildCloseItems() })).data
    } else {
      res = (await batchApi.batchGenerateReport({ items: buildReportItems() })).data
    }
    batchResponse.value = res
    if (res.failCount === 0) {
      ElMessage.success(`批量操作完成，全部 ${res.totalCount} 个账套执行成功`)
    } else {
      ElMessage.warning(`批量操作完成：成功 ${res.successCount} 个，失败 ${res.failCount} 个`)
    }
  } catch {
    // 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

function buildConfirmText(): string {
  const count = selectedAccountSetIds.value.length
  const period = `${queryYear.value}年${queryMonth.value}月`
  const actionMap: Record<string, string> = {
    'voucher-audit': '审核凭证',
    'period-close': '结账',
    'report-generate': '生成报表'
  }
  let text = `将对 ${count} 个账套执行【${actionMap[activeTab.value]}】操作，期间：${period}。`
  if (activeTab.value === 'report-generate') {
    const names = selectedReportTypes.value.map(reportTypeName).join('、')
    text += `报表类型：${names}。`
  }
  text += '单个账套失败不影响其他账套，是否继续？'
  return text
}

function buildAuditItems() {
  return selectedAccountSetIds.value.map((accountSetId) => ({
    accountSetId,
    year: queryYear.value,
    month: queryMonth.value
    // voucherIds 为空：审核该期间所有未审核凭证
  }))
}

function buildCloseItems() {
  return selectedAccountSetIds.value.map((accountSetId) => ({
    accountSetId,
    year: queryYear.value,
    month: queryMonth.value
  }))
}

function buildReportItems() {
  return selectedAccountSetIds.value.map((accountSetId) => ({
    accountSetId,
    year: queryYear.value,
    month: queryMonth.value,
    reportTypes: [...selectedReportTypes.value]
  }))
}

// ==================== 结果展示辅助 ====================
function statusLabel(status?: BatchOperationStatus): string {
  switch (status) {
    case 'success':
      return '成功'
    case 'partial':
      return '部分成功'
    case 'failed':
      return '失败'
    default:
      return '未知'
  }
}

function statusTagType(status?: BatchOperationStatus): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'success':
      return 'success'
    case 'partial':
      return 'warning'
    case 'failed':
      return 'danger'
    default:
      return 'info'
  }
}

function reportTypeName(type: string): string {
  const map: Record<string, string> = {
    'balance-sheet': '资产负债表',
    'income-statement': '利润表',
    'cash-flow-statement': '现金流量表',
    'subject-balance': '科目余额表'
  }
  return map[type] || type
}

// 错误详情
const errorDetailVisible = ref(false)
const errorDetail = ref<BatchOperationResultVO | null>(null)
function showErrorDetail(row: BatchOperationResultVO) {
  errorDetail.value = row
  errorDetailVisible.value = true
}

// ==================== 导出 Excel ====================
function exportResults() {
  if (!batchResponse.value || batchResponse.value.results.length === 0) {
    ElMessage.warning('暂无结果可导出')
    return
  }
  const actionMap: Record<string, string> = {
    'voucher-audit': '批量审核凭证',
    'period-close': '批量结账',
    'report-generate': '批量生成报表'
  }
  const rows = batchResponse.value.results.map((r) => ({
    账套名称: r.accountSetName || '账套#' + r.accountSetId,
    账套ID: r.accountSetId,
    状态: statusLabel(r.status),
    结果详情: r.message
  }))
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '批量操作结果')
  worksheet['!cols'] = [{ wch: 24 }, { wch: 12 }, { wch: 12 }, { wch: 60 }]
  const fileName = `${actionMap[activeTab.value]}_${queryYear.value}${String(queryMonth.value).padStart(2, '0')}.xlsx`
  XLSX.writeFile(workbook, fileName)
}

// ==================== 操作历史 ====================
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref<SysOperationLog[]>([])
const historyTotal = ref(0)
const historyQuery = reactive<BatchHistoryQueryRequest>({
  operationType: undefined,
  startDate: undefined,
  endDate: undefined,
  pageNum: 1,
  pageSize: 10
})

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await batchApi.queryHistory({
      operationType: historyQuery.operationType,
      startDate: historyQuery.startDate,
      endDate: historyQuery.endDate,
      pageNum: historyQuery.pageNum,
      pageSize: historyQuery.pageSize
    })
    historyList.value = res.data.list
    historyTotal.value = res.data.total
  } catch {
    // 错误由拦截器处理
  } finally {
    historyLoading.value = false
  }
}

// 历史详情
const historyDetailVisible = ref(false)
const historyDetail = ref<SysOperationLog | null>(null)
function showHistoryDetail(row: SysOperationLog) {
  historyDetail.value = row
  historyDetailVisible.value = true
}

function formatJson(params: string | null): string {
  if (!params) return ''
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params
  }
}

onMounted(() => {
  loadAccountSets()
})
</script>

<style scoped lang="scss">
.batch-operation-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.main-card {
  .tab-tip {
    padding: 8px 12px;
    margin-bottom: 12px;
    background-color: #f4f4f5;
    border-radius: 4px;
    color: #606266;
    font-size: 13px;
    line-height: 1.6;
  }

  .select-form {
    margin: 12px 0;
  }

  .progress-area {
    margin: 16px 0;
    padding: 16px;
    background-color: #ecf5ff;
    border-radius: 4px;
    display: flex;
    align-items: center;
    gap: 12px;

    .progress-text {
      color: #409eff;
      font-size: 14px;
    }
  }

  .result-area {
    margin-top: 16px;

    .result-title {
      font-weight: 600;
      color: #303133;
    }

    .result-summary {
      margin-bottom: 16px;
      padding: 12px 0;
    }

    .result-toolbar {
      margin-bottom: 12px;
    }
  }

  .fail-message {
    color: #f56c6c;
  }

  .json-pre {
    margin: 0;
    max-height: 240px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 12px;
  }
}

.history-filter {
  margin-bottom: 12px;
}

.history-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
