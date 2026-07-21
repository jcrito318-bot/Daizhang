<template>
  <div class="smart-reconciliation-container">
    <!-- 顶部筛选 + 操作 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="账套" required>
          <el-select
            v-model="filterForm.accountSetId"
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
        <el-form-item label="银行账户" required>
          <el-input v-model="filterForm.bankAccount" placeholder="请输入银行账号" style="width: 200px" />
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="filterForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="filterForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="matchLoading" @click="handleSmartMatch">
            <el-icon><Connection /></el-icon>智能匹配
          </el-button>
          <el-button @click="handleResetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <!-- 主区域:建议列表 -->
      <el-col :xs="24" :lg="17">
        <el-card class="suggestions-card">
          <template #header>
            <div class="card-header">
              <span>
                匹配建议 ({{ visibleSuggestions.length }}/{{ suggestions.length }})
                <el-tag v-if="suggestions.length > 0" type="info" size="small" class="ml-8">
                  共 {{ suggestions.length }} 条建议
                </el-tag>
              </span>
              <div>
                <el-button
                  type="success"
                  size="small"
                  :disabled="strongSuggestions.length === 0"
                  @click="handleAcceptAllStrong"
                >
                  全部接受强建议(≥80)
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  :loading="applyLoading"
                  :disabled="acceptedItems.length === 0"
                  @click="handleApplyAccepted"
                >
                  应用已接受建议({{ acceptedItems.length }})
                </el-button>
              </div>
            </div>
          </template>

          <div v-loading="matchLoading">
            <el-empty v-if="!matchLoading && suggestions.length === 0" description="暂无匹配建议,请点击「智能匹配」按钮开始分析" />

            <div v-else class="suggestion-list">
              <el-card
                v-for="suggestion in visibleSuggestions"
                :key="suggestion.transactionId"
                shadow="hover"
                class="suggestion-item"
                :class="getSuggestionClass(suggestion)"
              >
                <!-- 顶栏:分数进度条 + 类型 + 操作 -->
                <div class="suggestion-top">
                  <div class="score-block">
                    <span class="score-label">匹配分数</span>
                    <el-progress
                      :percentage="suggestion.score"
                      :color="getScoreColor(suggestion.score)"
                      :stroke-width="14"
                      :text-inside="true"
                      style="width: 180px"
                    />
                    <el-tag :color="getScoreColor(suggestion.score)" effect="dark" size="small" class="ml-8">
                      {{ suggestion.matchTypeName }}
                    </el-tag>
                  </div>
                  <div class="action-block">
                    <el-button
                      v-if="acceptedSet.has(suggestion.transactionId)"
                      type="success"
                      size="small"
                      disabled
                    >
                      已接受
                    </el-button>
                    <el-button
                      v-else-if="rejectedSet.has(suggestion.transactionId)"
                      type="info"
                      size="small"
                      disabled
                    >
                      已拒绝
                    </el-button>
                    <template v-else>
                      <el-button type="success" size="small" @click="handleAccept(suggestion)">
                        接受
                      </el-button>
                      <el-button type="danger" size="small" plain @click="handleReject(suggestion)">
                        拒绝
                      </el-button>
                    </template>
                    <el-button size="small" text @click="handleViewDetail(suggestion)">
                      查看详情
                    </el-button>
                  </div>
                </div>

                <!-- 中栏:流水信息 | ↔ | 凭证信息 -->
                <el-row :gutter="8" class="suggestion-middle">
                  <el-col :span="11">
                    <div class="info-block transaction-block">
                      <div class="info-title">银行流水</div>
                      <div class="info-row">
                        <span class="info-label">日期:</span>
                        <span>{{ suggestion.transactionDate || '-' }}</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">金额:</span>
                        <span :class="suggestion.transactionType === 1 ? 'amount-in' : 'amount-out'">
                          {{ suggestion.transactionType === 1 ? '+' : '-' }}{{ formatAmount(suggestion.transactionAmount) }}
                        </span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">对方:</span>
                        <span class="info-text" :title="suggestion.counterparty">
                          {{ suggestion.counterparty || '-' }}
                        </span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">摘要:</span>
                        <span class="info-text" :title="suggestion.transactionSummary">
                          {{ suggestion.transactionSummary || '-' }}
                        </span>
                      </div>
                    </div>
                  </el-col>
                  <el-col :span="2" class="link-col">
                    <el-icon class="link-icon"><Sort /></el-icon>
                  </el-col>
                  <el-col :span="11">
                    <div class="info-block voucher-block">
                      <div class="info-title">凭证</div>
                      <div class="info-row">
                        <span class="info-label">编号:</span>
                        <span>{{ suggestion.voucherNo || '-' }}</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">日期:</span>
                        <span>{{ suggestion.voucherDate || '-' }}</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">金额:</span>
                        <span class="amount-neutral">{{ formatAmount(suggestion.voucherAmount) }}</span>
                      </div>
                      <div class="info-row">
                        <span class="info-label">摘要:</span>
                        <span class="info-text" :title="suggestion.voucherSummary">
                          {{ suggestion.voucherSummary || '-' }}
                        </span>
                      </div>
                    </div>
                  </el-col>
                </el-row>

                <!-- 底栏:匹配原因标签 -->
                <div class="suggestion-bottom">
                  <span class="reasons-label">匹配原因:</span>
                  <el-tag
                    v-for="reason in suggestion.reasons"
                    :key="reason"
                    type="success"
                    effect="plain"
                    size="small"
                    class="reason-tag"
                  >
                    {{ reason }}
                  </el-tag>
                  <span v-if="!suggestion.reasons || suggestion.reasons.length === 0" class="no-reason">
                    无
                  </span>
                </div>
              </el-card>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧:未匹配统计 + 历史模式 -->
      <el-col :xs="24" :lg="7">
        <el-card class="stats-card">
          <template #header>
            <span>对账统计</span>
          </template>
          <div class="stats-block">
            <el-statistic title="已匹配建议" :value="matchResult?.totalMatched ?? 0" />
            <el-statistic title="未匹配总数" :value="matchResult?.totalUnmatched ?? 0" />
          </div>
          <el-divider />
          <div class="stats-detail">
            <div class="stats-row">
              <el-icon class="stats-icon"><Money /></el-icon>
              <span class="stats-label">未匹配流水:</span>
              <span class="stats-value">{{ matchResult?.unmatchedTransactions.length ?? 0 }} 条</span>
            </div>
            <div class="stats-row">
              <el-icon class="stats-icon"><Document /></el-icon>
              <span class="stats-label">未匹配凭证:</span>
              <span class="stats-value">{{ matchResult?.unmatchedVouchers.length ?? 0 }} 条</span>
            </div>
          </div>
        </el-card>

        <el-card class="patterns-card">
          <template #header>
            <div class="card-header">
              <span>历史匹配模式</span>
              <el-button
                type="primary"
                size="small"
                text
                :loading="learnLoading"
                @click="handleLearnPatterns"
              >
                <el-icon><MagicStick /></el-icon>学习
              </el-button>
            </div>
          </template>
          <div v-loading="patternsLoading">
            <el-empty v-if="patterns.length === 0" description="暂无历史模式,点击「学习」从已匹配流水生成" :image-size="60" />
            <div v-else class="pattern-list">
              <div v-for="pattern in patterns" :key="pattern.id" class="pattern-item">
                <div class="pattern-top">
                  <span class="pattern-counterparty" :title="pattern.counterparty">
                    {{ pattern.counterparty }}
                  </span>
                  <el-tag type="warning" size="small">
                    匹配 {{ pattern.matchCount }} 次
                  </el-tag>
                </div>
                <div class="pattern-detail">
                  <span>金额范围:</span>
                  <span>{{ formatAmount(pattern.amountRangeMin) }} ~ {{ formatAmount(pattern.amountRangeMax) }}</span>
                </div>
                <div class="pattern-detail">
                  <span>对应科目:</span>
                  <span>{{ pattern.voucherSubjectCode || '-' }}</span>
                </div>
                <div v-if="pattern.lastMatchedAt" class="pattern-detail">
                  <span>最近匹配:</span>
                  <span>{{ pattern.lastMatchedAt }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="匹配建议详情" width="640px">
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="流水ID">{{ currentDetail.transactionId }}</el-descriptions-item>
        <el-descriptions-item label="凭证ID">{{ currentDetail.voucherId }}</el-descriptions-item>
        <el-descriptions-item label="匹配分数">{{ currentDetail.score }}</el-descriptions-item>
        <el-descriptions-item label="匹配类型">{{ currentDetail.matchTypeName }}</el-descriptions-item>
        <el-descriptions-item label="流水日期">{{ currentDetail.transactionDate }}</el-descriptions-item>
        <el-descriptions-item label="凭证日期">{{ currentDetail.voucherDate }}</el-descriptions-item>
        <el-descriptions-item label="流水金额">{{ formatAmount(currentDetail.transactionAmount) }}</el-descriptions-item>
        <el-descriptions-item label="凭证金额">{{ formatAmount(currentDetail.voucherAmount) }}</el-descriptions-item>
        <el-descriptions-item label="流水摘要" :span="2">{{ currentDetail.transactionSummary }}</el-descriptions-item>
        <el-descriptions-item label="凭证摘要" :span="2">{{ currentDetail.voucherSummary }}</el-descriptions-item>
        <el-descriptions-item label="交易对方" :span="2">{{ currentDetail.counterparty }}</el-descriptions-item>
        <el-descriptions-item label="匹配原因" :span="2">
          <el-tag
            v-for="reason in currentDetail.reasons"
            :key="reason"
            type="success"
            effect="plain"
            size="small"
            class="reason-tag"
          >
            {{ reason }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Document, MagicStick, Money, Sort } from '@element-plus/icons-vue'
import { smartReconciliationApi } from '@/api/bank'
import { useAppStore } from '@/stores/app'
import type { MatchSuggestionVO, MatchResultVO, MatchHistoryPattern } from '@/types/bank'

const appStore = useAppStore()

// 顶部筛选表单
const filterForm = reactive({
  accountSetId: appStore.currentAccountSetId ?? 0,
  bankAccount: '',
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

// 状态
const matchLoading = ref(false)
const applyLoading = ref(false)
const patternsLoading = ref(false)
const learnLoading = ref(false)

// 匹配结果与建议列表
const matchResult = ref<MatchResultVO | null>(null)
const suggestions = ref<MatchSuggestionVO[]>([])
// 用户已接受 / 已拒绝的 transactionId 集合
const acceptedSet = ref<Set<number>>(new Set())
const rejectedSet = ref<Set<number>>(new Set())

// 历史模式列表
const patterns = ref<MatchHistoryPattern[]>([])

// 详情对话框
const detailDialogVisible = ref(false)
const currentDetail = ref<MatchSuggestionVO | null>(null)

// 计算属性
/** 已拒绝的隐藏后,可见建议列表 */
const visibleSuggestions = computed(() =>
  suggestions.value.filter(s => !rejectedSet.value.has(s.transactionId))
)

/** 强建议(分数 ≥80)且尚未接受/拒绝的列表 */
const strongSuggestions = computed(() =>
  suggestions.value.filter(
    s => s.score >= 80 && !acceptedSet.value.has(s.transactionId) && !rejectedSet.value.has(s.transactionId)
  )
)

/** 已接受的建议项(用于应用) */
const acceptedItems = computed(() =>
  suggestions.value
    .filter(s => acceptedSet.value.has(s.transactionId))
    .map(s => ({ transactionId: s.transactionId, voucherId: s.voucherId }))
)

// ===== 工具方法 =====

function formatAmount(val: number | undefined | null): string {
  if (val === undefined || val === null) return '-'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 分数颜色: 80+绿/60-79黄/<60红 */
function getScoreColor(score: number): string {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

/** 建议项卡片样式类(根据分数段) */
function getSuggestionClass(suggestion: MatchSuggestionVO): string {
  if (suggestion.score >= 80) return 'suggestion-strong'
  if (suggestion.score >= 60) return 'suggestion-normal'
  return 'suggestion-weak'
}

// ===== 业务方法 =====

function handleAccountSetChange() {
  // 切换账套时清空已加载的数据
  matchResult.value = null
  suggestions.value = []
  acceptedSet.value.clear()
  rejectedSet.value.clear()
  patterns.value = []
}

function handleResetFilters() {
  filterForm.bankAccount = ''
  filterForm.year = new Date().getFullYear()
  filterForm.month = new Date().getMonth() + 1
  matchResult.value = null
  suggestions.value = []
  acceptedSet.value.clear()
  rejectedSet.value.clear()
}

async function handleSmartMatch() {
  if (!filterForm.accountSetId) {
    ElMessage.warning('请选择账套')
    return
  }
  if (!filterForm.bankAccount) {
    ElMessage.warning('请输入银行账号')
    return
  }

  matchLoading.value = true
  try {
    const res = await smartReconciliationApi.smartMatch({
      accountSetId: filterForm.accountSetId,
      bankAccount: filterForm.bankAccount,
      year: filterForm.year,
      month: filterForm.month
    })
    matchResult.value = res.data
    suggestions.value = res.data.matched || []
    acceptedSet.value.clear()
    rejectedSet.value.clear()
    ElMessage.success(`智能匹配完成,共生成 ${suggestions.value.length} 条建议`)
    // 同时加载历史模式(若已切换账套)
    void loadPatterns(filterForm.accountSetId)
  } catch {
    // handled by interceptor
  } finally {
    matchLoading.value = false
  }
}

function handleAccept(suggestion: MatchSuggestionVO) {
  acceptedSet.value.add(suggestion.transactionId)
  // 若之前在拒绝集合中,移除
  rejectedSet.value.delete(suggestion.transactionId)
  ElMessage.success(`已接受建议(流水 ${suggestion.transactionId})`)
}

function handleReject(suggestion: MatchSuggestionVO) {
  rejectedSet.value.add(suggestion.transactionId)
  acceptedSet.value.delete(suggestion.transactionId)
  ElMessage.info(`已拒绝建议(流水 ${suggestion.transactionId})`)
}

function handleAcceptAllStrong() {
  if (strongSuggestions.value.length === 0) {
    ElMessage.info('没有可接受的强建议')
    return
  }
  strongSuggestions.value.forEach(s => acceptedSet.value.add(s.transactionId))
  ElMessage.success(`已接受 ${strongSuggestions.value.length} 条强建议`)
}

async function handleApplyAccepted() {
  if (acceptedItems.value.length === 0) {
    ElMessage.warning('请先接受至少一条建议')
    return
  }
  if (!filterForm.accountSetId) {
    ElMessage.warning('请选择账套')
    return
  }

  try {
    await ElMessageBox.confirm(
      `即将应用 ${acceptedItems.value.length} 条已接受的匹配建议,是否继续?`,
      '确认应用',
      { confirmButtonText: '应用', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    // 用户取消
    return
  }

  applyLoading.value = true
  try {
    const res = await smartReconciliationApi.applySuggestions({
      accountSetId: filterForm.accountSetId,
      items: acceptedItems.value
    })
    ElMessage.success(`成功应用 ${res.data} 条匹配建议`)
    // 应用后重新执行智能匹配以刷新建议列表(已匹配流水将不再出现)
    await handleSmartMatch()
  } catch {
    // handled by interceptor
  } finally {
    applyLoading.value = false
  }
}

function handleViewDetail(suggestion: MatchSuggestionVO) {
  currentDetail.value = suggestion
  detailDialogVisible.value = true
}

async function loadPatterns(accountSetId: number) {
  patternsLoading.value = true
  try {
    const res = await smartReconciliationApi.getMatchPatterns(accountSetId)
    patterns.value = res.data || []
  } catch {
    // handled by interceptor
  } finally {
    patternsLoading.value = false
  }
}

async function handleLearnPatterns() {
  if (!filterForm.accountSetId) {
    ElMessage.warning('请选择账套')
    return
  }
  learnLoading.value = true
  try {
    const res = await smartReconciliationApi.learnPatterns(filterForm.accountSetId)
    ElMessage.success(`学习完成,共处理 ${res.data} 条历史匹配`)
    await loadPatterns(filterForm.accountSetId)
  } catch {
    // handled by interceptor
  } finally {
    learnLoading.value = false
  }
}

// ===== 初始化 =====
onMounted(async () => {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !filterForm.accountSetId) {
      filterForm.accountSetId = appStore.currentAccountSetId ?? list[0].id
    }
    // 若已有账套,预加载历史模式
    if (filterForm.accountSetId) {
      void loadPatterns(filterForm.accountSetId)
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.smart-reconciliation-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.ml-8 {
  margin-left: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestions-card,
.stats-card,
.patterns-card {
  margin-bottom: 16px;
}

// 建议项卡片
.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  border-left-width: 4px !important;
  border-left-style: solid !important;
}

.suggestion-strong {
  border-left-color: #67c23a !important;
}

.suggestion-normal {
  border-left-color: #e6a23c !important;
}

.suggestion-weak {
  border-left-color: #f56c6c !important;
}

.suggestion-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.score-block {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.action-block {
  display: flex;
  gap: 4px;
}

.suggestion-middle {
  margin-bottom: 12px;
}

.info-block {
  background: #fafafa;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  height: 100%;
}

.transaction-block {
  border-left: 2px solid #409eff;
}

.voucher-block {
  border-left: 2px solid #67c23a;
}

.info-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.info-row {
  display: flex;
  margin-bottom: 4px;
  line-height: 1.6;
}

.info-label {
  color: #909399;
  width: 50px;
  flex-shrink: 0;
}

.info-text {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount-in {
  color: #67c23a;
  font-weight: 600;
}

.amount-out {
  color: #f56c6c;
  font-weight: 600;
}

.amount-neutral {
  color: #303133;
  font-weight: 600;
}

.link-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.link-icon {
  font-size: 20px;
  color: #909399;
  transform: rotate(90deg);
}

.suggestion-bottom {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;
}

.reasons-label {
  color: #909399;
  margin-right: 4px;
}

.reason-tag {
  margin-right: 4px;
}

.no-reason {
  color: #c0c4cc;
  font-style: italic;
}

// 右侧统计
.stats-block {
  display: flex;
  justify-content: space-around;
  margin-bottom: 8px;
}

.stats-detail {
  font-size: 13px;
}

.stats-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.stats-icon {
  margin-right: 6px;
  color: #409eff;
}

.stats-label {
  color: #606266;
  flex: 1;
}

.stats-value {
  color: #303133;
  font-weight: 600;
}

// 历史模式
.pattern-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pattern-item {
  background: #fafafa;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 13px;
  border-left: 2px solid #e6a23c;
}

.pattern-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.pattern-counterparty {
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}

.pattern-detail {
  color: #606266;
  line-height: 1.6;
  display: flex;
  gap: 4px;
}
</style>
