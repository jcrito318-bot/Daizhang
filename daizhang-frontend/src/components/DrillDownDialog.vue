<template>
  <el-dialog
    :model-value="visible"
    title="钻取凭证"
    width="920px"
    :close-on-click-modal="false"
    append-to-body
    @update:model-value="handleVisibleChange"
    @open="handleOpen"
  >
    <div v-loading="loading">
      <!-- 钻取条件信息 -->
      <el-descriptions :column="4" border size="small" class="drill-info">
        <el-descriptions-item label="科目编码">{{ subjectCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标金额">
          <span class="amount-text">{{ formatAmount(amount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="期间">{{ year }}年{{ month }}月</el-descriptions-item>
        <el-descriptions-item label="方向">
          <el-radio-group v-model="currentDirection" size="small" @change="handleConditionChange">
            <el-radio-button value="debit">借方</el-radio-button>
            <el-radio-button value="credit">贷方</el-radio-button>
          </el-radio-group>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 模糊匹配开关 -->
      <div class="options-bar">
        <el-checkbox v-model="fuzzy" @change="handleConditionChange">模糊匹配(±0.01 容差)</el-checkbox>
        <span class="result-count" v-if="!loading">共 {{ vouchers.length }} 条记录</span>
      </div>

      <!-- 凭证列表 -->
      <el-table
        :data="vouchers"
        border
        stripe
        empty-text="未找到匹配的凭证,可尝试切换方向或开启模糊匹配"
        :row-class-name="rowClassName"
        @row-dblclick="handleViewVoucher"
      >
        <el-table-column prop="voucherNo" label="凭证号" width="130" />
        <el-table-column prop="voucherDate" label="日期" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="debitAmount" label="借方金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="{ 'hit-amount': currentDirection === 'debit' }">
              {{ row.debitAmount ? formatAmount(row.debitAmount) : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="creditAmount" label="贷方金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="{ 'hit-amount': currentDirection === 'credit' }">
              {{ row.creditAmount ? formatAmount(row.creditAmount) : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleViewVoucher(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="vouchers.length > 0" class="hint-text">
        提示:双击任意行可跳转至该凭证详情
      </div>
    </div>

    <template #footer>
      <el-button @click="handleViewInLedger" :disabled="!subjectCode">
        在明细账中查看
      </el-button>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ledgerApi } from '@/api/ledger'
import type { DrillDownDirection, DrillDownVoucher } from '@/types/ledger'

/**
 * 报表钻取对话框
 * 代账会计双击报表金额后弹出,展示该金额对应的凭证分录列表,
 * 单击/双击凭证可跳转至凭证详情,底部按钮可跳转至明细账。
 */
interface Props {
  /** 是否显示 */
  visible: boolean
  /** 账套ID */
  accountSetId: number
  /** 科目编码(支持前缀匹配) */
  subjectCode: string
  /** 年度 */
  year: number
  /** 月份 */
  month: number
  /** 目标金额 */
  amount: number
  /** 钻取方向:debit-借方 / credit-贷方 */
  direction: DrillDownDirection
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  accountSetId: 0,
  subjectCode: '',
  year: 0,
  month: 0,
  amount: 0,
  direction: 'debit' as DrillDownDirection
})

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const router = useRouter()

const loading = ref(false)
const vouchers = ref<DrillDownVoucher[]>([])
const currentDirection = ref<DrillDownDirection>(props.direction)
const fuzzy = ref(false)

/**
 * 监听方向 prop 变化(同一对话框被不同单元格打开时同步)
 */
watch(
  () => props.direction,
  (val) => {
    currentDirection.value = val
  }
)

/**
 * 监听 visible / 关键参数变化,在弹窗打开时触发查询
 */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      // 每次打开时重置方向为外部传入值,并触发查询
      currentDirection.value = props.direction
      fuzzy.value = false
      loadData()
    }
  }
)

/**
 * 弹窗 open 事件回调(用于初次打开时确保数据加载)
 */
function handleOpen() {
  currentDirection.value = props.direction
  loadData()
}

/**
 * 条件变化(方向/模糊匹配)时重新查询
 */
function handleConditionChange() {
  if (props.visible) {
    loadData()
  }
}

/**
 * 加载钻取结果
 */
async function loadData() {
  if (!props.accountSetId || !props.subjectCode || !props.year || !props.month) {
    vouchers.value = []
    return
  }
  if (!props.amount || props.amount <= 0) {
    vouchers.value = []
    return
  }
  loading.value = true
  try {
    const res = await ledgerApi.drillDown({
      accountSetId: props.accountSetId,
      subjectCode: props.subjectCode,
      year: props.year,
      month: props.month,
      amount: props.amount,
      direction: currentDirection.value,
      fuzzy: fuzzy.value
    })
    vouchers.value = res.data.vouchers || []
  } catch {
    vouchers.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 格式化金额
 */
function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) {
    return '0.00'
  }
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 行样式:鼠标悬停时显示可点击效果
 */
function rowClassName(): string {
  return 'drill-row'
}

/**
 * 查看凭证详情
 */
function handleViewVoucher(row: DrillDownVoucher) {
  if (!row || !row.voucherId) {
    ElMessage.warning('凭证ID缺失,无法跳转')
    return
  }
  // 关闭弹窗后跳转,避免弹窗遮挡凭证详情页
  emit('update:visible', false)
  router.push(`/voucher/${row.voucherId}`)
}

/**
 * 跳转至明细账
 */
function handleViewInLedger() {
  if (!props.subjectCode) {
    ElMessage.warning('科目编码缺失,无法跳转')
    return
  }
  emit('update:visible', false)
  router.push({
    path: '/ledger/detail',
    query: {
      subjectCode: props.subjectCode,
      year: String(props.year),
      month: String(props.month),
      accountSetId: String(props.accountSetId)
    }
  })
}

/**
 * 关闭弹窗
 */
function handleClose() {
  emit('update:visible', false)
}

/**
 * 同步 visible 状态
 */
function handleVisibleChange(val: boolean) {
  emit('update:visible', val)
}
</script>

<style scoped lang="scss">
.drill-info {
  margin-bottom: 12px;
}

.amount-text {
  font-weight: 600;
  color: #f56c6c;
}

.options-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 4px;

  .result-count {
    color: #909399;
    font-size: 13px;
  }
}

.hint-text {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

// 命中金额高亮
:deep(.hit-amount) {
  color: #f56c6c;
  font-weight: 600;
}

// 行可点击效果
:deep(.drill-row) {
  cursor: pointer;
}
</style>
