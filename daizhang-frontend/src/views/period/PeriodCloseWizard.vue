<template>
  <div class="period-close-wizard-container">
    <!-- 顶部:账套与期间选择 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <el-icon><MagicStick /></el-icon>
          <span>期末结账向导</span>
          <span class="header-hint">一键完成"结转损益 + 结账 + 下月开启"</span>
        </div>
      </template>
      <el-form :model="configForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="configForm.accountSetId"
            placeholder="请选择账套"
            style="width: 240px"
            :disabled="executing"
          >
            <el-option
              v-for="item in appStore.accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="期间">
          <el-date-picker
            v-model="configForm.period"
            type="month"
            placeholder="选择月份"
            format="YYYY年MM月"
            value-format="YYYY-MM"
            style="width: 180px"
            :disabled="executing"
          />
        </el-form-item>
        <el-form-item>
          <el-tooltip
            content="勾选后跳过期末调汇、结转成本、计提折旧等可选步骤,适合商业账套快速结账"
            placement="top"
          >
            <el-checkbox v-model="configForm.skipOptionalSteps" :disabled="executing">
              跳过可选步骤
            </el-checkbox>
          </el-tooltip>
        </el-form-item>
        <el-form-item>
          <el-tooltip
            content="勾选后数据完整性检查失败时中止后续步骤并回滚;取消则强制继续(慎用)"
            placement="top"
          >
            <el-checkbox v-model="configForm.autoCloseIfNoErrors" :disabled="executing">
              完整性检查失败时中止
            </el-checkbox>
          </el-tooltip>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 中部:向导步骤与结果 -->
    <el-card class="steps-card">
      <!-- el-steps 进度条 -->
      <el-steps :active="activeStepIndex" align-center finish-status="success">
        <el-step
          v-for="(stepDef, index) in STEP_DEFINITIONS"
          :key="stepDef.no"
          :title="stepDef.name"
          :description="stepDef.desc"
          :status="getElStepStatus(index)"
        />
      </el-steps>

      <!-- 步骤结果列表 -->
      <div class="step-results">
        <div
          v-for="step in visibleSteps"
          :key="step.stepNo"
          class="step-result-item"
          :class="stepStatusClass(step.status)"
        >
          <div class="step-result-header">
            <el-icon class="step-icon">
              <component :is="stepIconComponent(step.status)" />
            </el-icon>
            <span class="step-no">步骤 {{ step.stepNo }}</span>
            <span class="step-name">{{ step.stepName }}</span>
            <el-tag :type="stepTagType(step.status)" size="small" effect="light">
              {{ stepStatusText(step.status) }}
            </el-tag>
            <el-button
              v-if="step.status === 'failed' && step.errorDetail"
              link
              type="danger"
              size="small"
              @click="toggleErrorExpand(step.stepNo)"
            >
              {{ expandedErrors.has(step.stepNo) ? '收起详情' : '查看详情' }}
            </el-button>
            <el-button
              v-if="step.voucherId"
              link
              type="primary"
              size="small"
              @click="viewVoucher(step.voucherId!)"
            >
              查看凭证
            </el-button>
          </div>
          <div class="step-message">{{ step.message }}</div>
          <el-collapse-transition>
            <div
              v-if="expandedErrors.has(step.stepNo) && step.errorDetail"
              class="step-error-detail"
            >
              <pre>{{ step.errorDetail }}</pre>
            </div>
          </el-collapse-transition>
        </div>

        <!-- 执行中的加载提示 -->
        <div v-if="executing && apiCallInProgress" class="step-result-item executing">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在调用后端执行结账向导,请稍候...</span>
        </div>
        <div
          v-else-if="executing && !apiCallInProgress && visibleSteps.length < STEP_DEFINITIONS.length"
          class="step-result-item executing"
        >
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在执行步骤 {{ visibleSteps.length + 1 }}: {{ STEP_DEFINITIONS[visibleSteps.length]?.name }}...</span>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-if="!executing && visibleSteps.length === 0"
          description="选择账套与期间后,点击"开始执行"启动结账向导"
          :image-size="80"
        />
      </div>

      <!-- 完成汇总 -->
      <div v-if="wizardResult && !executing" class="wizard-summary">
        <el-alert
          :title="summaryTitle"
          :type="summaryAlertType"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="summary-content">
              <span>成功 {{ wizardResult.successCount }} 步</span>
              <el-divider direction="vertical" />
              <span>失败 {{ wizardResult.failedCount }} 步</span>
              <el-divider direction="vertical" />
              <span>跳过 {{ wizardResult.skippedCount }} 步</span>
              <el-divider direction="vertical" />
              <span :class="wizardResult.nextPeriodOpened ? 'next-opened' : 'next-closed'">
                下月期间: {{ wizardResult.nextPeriodOpened ? '已开启' : '未开启' }}
              </span>
            </div>
          </template>
        </el-alert>
      </div>

      <!-- 底部操作按钮 -->
      <div class="wizard-actions">
        <el-button
          type="primary"
          size="large"
          :loading="executing"
          :disabled="!canExecute"
          @click="handleExecute"
        >
          <el-icon v-if="!executing"><VideoPlay /></el-icon>
          {{ executing ? '正在执行...' : '开始执行' }}
        </el-button>
        <el-button
          v-if="hasFailedStep && !executing"
          type="warning"
          size="large"
          @click="handleExecute"
        >
          <el-icon><RefreshRight /></el-icon>
          重新执行
        </el-button>
        <el-button size="large" :disabled="executing" @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  Loading,
  VideoPlay,
  RefreshRight,
  RefreshLeft,
  Check,
  Close,
  Minus
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { periodApi } from '@/api/period'
import type { PeriodCloseWizardVO, WizardStep, WizardStepStatus } from '@/types/period'

const router = useRouter()
const appStore = useAppStore()

/**
 * 步骤静态定义(与后端 7 步对应)。
 * 用于 el-steps 标题与动画过程中的占位展示。
 */
interface StepDefinition {
  no: number
  name: string
  desc: string
}
const STEP_DEFINITIONS: ReadonlyArray<StepDefinition> = [
  { no: 1, name: '数据完整性检查', desc: '检查未审核/借贷不平凭证' },
  { no: 2, name: '期末调汇', desc: '外币科目调汇(可选)' },
  { no: 3, name: '结转损益', desc: '收入费用结转本年利润' },
  { no: 4, name: '结转成本', desc: '销售成本结转(可选)' },
  { no: 5, name: '计提折旧', desc: '固定资产折旧(可选)' },
  { no: 6, name: '结账', desc: '关闭本期会计期间' },
  { no: 7, name: '下月开启', desc: '创建下月会计期间' }
]

/** 单步动画揭示间隔(ms),后端一次返回,前端模拟逐步执行 */
const STEP_ANIMATION_INTERVAL = 500

const configForm = reactive({
  accountSetId: appStore.currentAccountSetId ?? 0,
  /** 期间,格式 YYYY-MM(el-date-picker value-format) */
  period: defaultPeriod(),
  skipOptionalSteps: true,
  autoCloseIfNoErrors: true
})

/** 当前默认期间:本月 */
function defaultPeriod(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

/** 是否正在执行(API 调用 + 动画揭示) */
const executing = ref(false)
/** 是否处于后端 API 调用阶段(尚未开始动画) */
const apiCallInProgress = ref(false)
/** 后端返回的完整结果 */
const wizardResult = ref<PeriodCloseWizardVO | null>(null)
/** 已揭示的步骤(动画逐步追加) */
const visibleSteps = ref<WizardStep[]>([])
/** 展开错误详情的步骤序号集合 */
const expandedErrors = ref<Set<number>>(new Set())

/** el-steps 的 active 索引:执行中为已揭示步数,完成后为总步数 */
const activeStepIndex = computed(() => {
  if (executing.value) return visibleSteps.value.length
  if (wizardResult.value) return STEP_DEFINITIONS.length
  return 0
})

/** 是否可执行:账套与期间已选且未在执行中 */
const canExecute = computed(() => {
  return (
    !executing.value &&
    configForm.accountSetId > 0 &&
    !!configForm.period
  )
})

/** 是否存在失败步骤 */
const hasFailedStep = computed(() => {
  return wizardResult.value != null && wizardResult.value.failedCount > 0
})

/** 汇总标题 */
const summaryTitle = computed(() => {
  if (!wizardResult.value) return ''
  switch (wizardResult.value.overallStatus) {
    case 'success':
      return '期末结账向导执行成功'
    case 'failed':
      return '期末结账向导执行失败(已回滚)'
    case 'partial':
      return '期末结账向导执行完成(含跳过步骤)'
    default:
      return '期末结账向导执行完成'
  }
})

/** 汇总 Alert 类型 */
const summaryAlertType = computed(() => {
  if (!wizardResult.value) return 'info'
  switch (wizardResult.value.overallStatus) {
    case 'success':
      return 'success'
    case 'failed':
      return 'error'
    case 'partial':
      return 'warning'
    default:
      return 'info'
  }
})

/** el-step 状态映射 */
function getElStepStatus(
  index: number
): 'wait' | 'process' | 'finish' | 'error' | 'success' {
  if (index < visibleSteps.value.length) {
    const step = visibleSteps.value[index]
    switch (step.status) {
      case 'success':
        return 'success'
      case 'failed':
        return 'error'
      case 'skipped':
        // el-steps 无 skipped 状态,使用 finish(灰色)近似
        return 'finish'
    }
  }
  if (index === visibleSteps.value.length && executing.value) {
    return 'process'
  }
  return 'wait'
}

/** 步骤结果项的 CSS class */
function stepStatusClass(status: WizardStepStatus): string {
  return `step-${status}`
}

/** 步骤图标组件 */
function stepIconComponent(status: WizardStepStatus) {
  switch (status) {
    case 'success':
      return Check
    case 'failed':
      return Close
    case 'skipped':
      return Minus
  }
}

/** 步骤状态文本 */
function stepStatusText(status: WizardStepStatus): string {
  switch (status) {
    case 'success':
      return '成功'
    case 'failed':
      return '失败'
    case 'skipped':
      return '跳过'
  }
}

/** 步骤状态对应的 el-tag 类型 */
function stepTagType(status: WizardStepStatus): 'success' | 'danger' | 'info' {
  switch (status) {
    case 'success':
      return 'success'
    case 'failed':
      return 'danger'
    case 'skipped':
      return 'info'
  }
}

/** 展开/收起错误详情 */
function toggleErrorExpand(stepNo: number): void {
  if (expandedErrors.value.has(stepNo)) {
    expandedErrors.value.delete(stepNo)
  } else {
    expandedErrors.value.add(stepNo)
  }
}

/** 跳转凭证详情 */
function viewVoucher(voucherId: number): void {
  router.push(`/voucher/${voucherId}`)
}

/** 执行结账向导 */
async function handleExecute(): Promise<void> {
  if (!configForm.accountSetId || configForm.accountSetId <= 0) {
    ElMessage.warning('请先选择账套')
    return
  }
  if (!configForm.period) {
    ElMessage.warning('请先选择期间')
    return
  }
  const [yearStr, monthStr] = configForm.period.split('-')
  const year = Number(yearStr)
  const month = Number(monthStr)
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) {
    ElMessage.warning('期间格式不合法')
    return
  }

  // 二次确认:结账为高危操作
  try {
    await ElMessageBox.confirm(
      `确定要对账套执行 ${year}年${month}月 期末结账向导吗?\n该操作将依次执行:数据完整性检查 → 结转损益 → 结账 → 下月开启。\n若任一必选步骤失败,整个流程将回滚。`,
      '期末结账向导确认',
      {
        confirmButtonText: '确定执行',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    // 用户取消
    return
  }

  // 重置状态
  wizardResult.value = null
  visibleSteps.value = []
  expandedErrors.value = new Set()
  executing.value = true
  apiCallInProgress.value = true

  try {
    const res = await periodApi.executeCloseWizard(
      configForm.accountSetId,
      year,
      month,
      {
        skipOptionalSteps: configForm.skipOptionalSteps,
        autoCloseIfNoErrors: configForm.autoCloseIfNoErrors
      }
    )
    apiCallInProgress.value = false
    // 后端一次返回所有步骤,前端模拟逐步执行展示动画
    animateSteps(res.data, 0)
  } catch {
    apiCallInProgress.value = false
    executing.value = false
    // 拦截器已提示错误
  }
}

/** 递归揭示步骤,模拟逐步执行动画 */
function animateSteps(result: PeriodCloseWizardVO, index: number): void {
  if (index >= result.steps.length) {
    // 所有步骤揭示完毕
    wizardResult.value = result
    executing.value = false
    // 根据结果给出全局提示
    if (result.overallStatus === 'success') {
      ElMessage.success('期末结账向导执行成功')
    } else if (result.overallStatus === 'failed') {
      ElMessage.error(`期末结账向导执行失败,${result.failedCount} 个步骤失败,已回滚`)
    } else {
      ElMessage.warning(`期末结账向导执行完成,含 ${result.skippedCount} 个跳过步骤`)
    }
    return
  }
  visibleSteps.value.push(result.steps[index])
  setTimeout(() => animateSteps(result, index + 1), STEP_ANIMATION_INTERVAL)
}

/** 重置向导状态 */
function handleReset(): void {
  wizardResult.value = null
  visibleSteps.value = []
  expandedErrors.value = new Set()
  executing.value = false
  apiCallInProgress.value = false
}

onMounted(async () => {
  // 加载账套列表(带缓存),若当前账套为空则回退到第一个
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && (!configForm.accountSetId || configForm.accountSetId <= 0)) {
      configForm.accountSetId = appStore.currentAccountSetId ?? list[0].id
    }
  } catch {
    // 拦截器已提示
  }
})
</script>

<style scoped lang="scss">
.period-close-wizard-container {
  padding: 20px;
}

.config-card {
  margin-bottom: 16px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-hint {
      margin-left: 12px;
      font-size: 12px;
      color: #909399;
      font-weight: normal;
    }
  }
}

.steps-card {
  .el-steps {
    margin-bottom: 24px;
  }

  .step-results {
    min-height: 200px;
    margin-bottom: 16px;

    .step-result-item {
      padding: 12px 16px;
      margin-bottom: 8px;
      border-radius: 6px;
      border-left: 4px solid #dcdfe6;
      background-color: #fafafa;
      transition: all 0.3s;

      &.step-success {
        border-left-color: #67c23a;
        background-color: #f0f9eb;
      }

      &.step-failed {
        border-left-color: #f56c6c;
        background-color: #fef0f0;
      }

      &.step-skipped {
        border-left-color: #909399;
        background-color: #f4f4f5;
      }

      &.executing {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #409eff;
        border-left-color: #409eff;
        background-color: #ecf5ff;
      }

      .step-result-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .step-icon {
          font-size: 16px;
        }

        .step-no {
          font-weight: 600;
          color: #606266;
          font-size: 13px;
        }

        .step-name {
          font-weight: 600;
          color: #303133;
          flex: 1;
        }
      }

      .step-message {
        margin-left: 24px;
        font-size: 13px;
        color: #606266;
        line-height: 1.5;
      }

      .step-error-detail {
        margin-top: 8px;
        margin-left: 24px;
        padding: 8px 12px;
        background-color: #fff;
        border: 1px solid #fde2e2;
        border-radius: 4px;

        pre {
          margin: 0;
          font-size: 12px;
          color: #f56c6c;
          white-space: pre-wrap;
          word-break: break-all;
          font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
        }
      }
    }
  }

  .wizard-summary {
    margin-bottom: 16px;

    .summary-content {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 4px;

      .next-opened {
        color: #67c23a;
        font-weight: 600;
      }

      .next-closed {
        color: #909399;
        font-weight: 600;
      }
    }
  }

  .wizard-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
}
</style>
