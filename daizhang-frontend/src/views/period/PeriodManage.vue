<template>
  <div class="period-manage-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 200px"
            @change="loadPeriods"
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
        <el-form-item>
          <el-button type="primary" @click="loadPeriods">查询</el-button>
          <el-button type="success" @click="handleInitPeriods">初始化期间</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>会计期间管理</span>
          <el-button type="primary" @click="handleTrialBalance" :disabled="!selectedPeriod">
            试算平衡
          </el-button>
        </div>
      </template>

      <el-table :data="periodList" v-loading="loading" border stripe>
        <el-table-column prop="month" label="月份" width="120" align="center">
          <template #default="{ row }">
            {{ row.month }}月
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已打开' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="300">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleSelectPeriod(row)"
              :class="{ 'is-active': selectedPeriod?.month === row.month }"
            >
              选择
            </el-button>
            <el-button
              type="warning"
              link
              @click="handleClosePeriod(row)"
              v-if="row.status === 1"
            >
              结账
            </el-button>
            <el-button
              type="info"
              link
              @click="handleReopenPeriod(row)"
              v-if="row.status === 0"
            >
              反结账
            </el-button>
            <el-button
              type="success"
              link
              @click="handleCarryForward(row)"
              v-if="row.status === 1 && row.month === 12"
            >
              年末结转
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 试算平衡对话框 -->
    <el-dialog v-model="trialBalanceDialogVisible" title="试算平衡" width="800px" destroy-on-close>
      <el-table :data="trialBalanceData.items" border stripe v-loading="trialBalanceLoading">
        <el-table-column prop="subjectCode" label="科目编码" width="120" />
        <el-table-column prop="subjectName" label="科目名称" min-width="200" />
        <el-table-column prop="debitBalance" label="借方余额" width="140" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.debitBalance) }}
          </template>
        </el-table-column>
        <el-table-column prop="creditBalance" label="贷方余额" width="140" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.creditBalance) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="trial-balance-footer">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="借方合计">
            <span style="font-weight: bold; color: #409eff">
              {{ formatAmount(trialBalanceData.totalDebit) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="贷方合计">
            <span style="font-weight: bold; color: #67c23a">
              {{ formatAmount(trialBalanceData.totalCredit) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="平衡状态">
            <el-tag :type="trialBalanceData.balanced ? 'success' : 'danger'">
              {{ trialBalanceData.balanced ? '平衡' : '不平衡' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { periodApi } from '@/api/period'
import type { TrialBalanceResultVO } from '@/api/period'
import type { PeriodVO } from '@/types/voucher'

const appStore = useAppStore()

const loading = ref(false)
const trialBalanceLoading = ref(false)
const periodList = ref<PeriodVO[]>([])
const selectedPeriod = ref<PeriodVO | null>(null)

const queryForm = reactive({
  accountSetId: appStore.currentAccountSetId || 0,
  year: new Date().getFullYear()
})

const trialBalanceDialogVisible = ref(false)
const trialBalanceData = reactive<TrialBalanceResultVO>({
  items: [],
  totalDebit: 0,
  totalCredit: 0,
  balanced: false
})

function formatAmount(val: number): string {
  if (val === 0) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadAccountSets() {
  try {
    const list = await appStore.loadAccountSetList()
    if (list.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = appStore.currentAccountSetId || list[0].id
      loadPeriods()
    }
  } catch {
    // handled by interceptor
  }
}

async function loadPeriods() {
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await periodApi.listPeriods(queryForm.accountSetId)
    periodList.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleInitPeriods() {
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  await ElMessageBox.confirm(
    `确定要初始化${queryForm.year}年的会计期间吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  try {
    await periodApi.initPeriods(queryForm.accountSetId, queryForm.year)
    ElMessage.success('初始化成功')
    loadPeriods()
  } catch {
    // handled by interceptor
  }
}

function handleSelectPeriod(row: PeriodVO) {
  selectedPeriod.value = row
}

async function handleClosePeriod(row: PeriodVO) {
  await ElMessageBox.confirm(
    `确定要结转${row.month}月吗？结转后将关闭该期间。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  try {
    const res = await periodApi.close(queryForm.accountSetId, queryForm.year, row.month)
    if (res.data.success) {
      ElMessage.success(res.data.message || '结转成功')
      loadPeriods()
    } else {
      ElMessage.warning(res.data.message || '结转失败')
    }
  } catch {
    // handled by interceptor
  }
}

async function handleReopenPeriod(row: PeriodVO) {
  await ElMessageBox.confirm(
    `确定要反结转${row.month}月吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  try {
    await periodApi.reopen(queryForm.accountSetId, queryForm.year, row.month)
    ElMessage.success('反结转成功')
    loadPeriods()
  } catch {
    // handled by interceptor
  }
}

async function handleCarryForward(row: PeriodVO) {
  await ElMessageBox.confirm(
    `确定要进行${queryForm.year}年年末结转吗？这将生成结转凭证。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  try {
    await periodApi.carryForward(queryForm.accountSetId, queryForm.year, row.month)
    ElMessage.success('年末结转成功')
    loadPeriods()
  } catch {
    // handled by interceptor
  }
}

async function handleTrialBalance() {
  if (!selectedPeriod.value) {
    ElMessage.warning('请先选择一个会计期间')
    return
  }
  trialBalanceDialogVisible.value = true
  trialBalanceLoading.value = true
  try {
    const res = await periodApi.trialBalance(
      queryForm.accountSetId,
      queryForm.year,
      selectedPeriod.value.month
    )
    Object.assign(trialBalanceData, res.data)
  } catch {
    // handled by interceptor
  } finally {
    trialBalanceLoading.value = false
  }
}

onMounted(() => {
  loadAccountSets()
})
</script>

<style scoped lang="scss">
.period-manage-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .is-active {
    color: #409eff;
    font-weight: bold;
  }
}

.trial-balance-footer {
  margin-top: 16px;
}
</style>
