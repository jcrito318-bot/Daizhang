<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <h2>欢迎回来，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</h2>
      <p>今天是 {{ currentDate }}，祝您工作顺利！</p>
    </div>

    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">账套数量</div>
              <el-statistic :value="stats.accountSetCount" />
            </div>
            <el-icon class="stat-icon" style="color: #409eff"><Wallet /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">本月凭证</div>
              <el-statistic :value="stats.monthVoucherCount" />
            </div>
            <el-icon class="stat-icon" style="color: #67c23a"><Document /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">待审核</div>
              <el-statistic :value="stats.pendingAuditCount" />
            </div>
            <el-icon class="stat-icon" style="color: #e6a23c"><Warning /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">待报税</div>
              <el-statistic :value="stats.pendingTaxCount" />
            </div>
            <el-icon class="stat-icon" style="color: #f56c6c"><Calendar /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="recent-vouchers-card">
      <template #header>
        <div class="card-header">
          <span>最近凭证</span>
          <el-button type="primary" link @click="goToVoucherList">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentVouchers" v-loading="voucherLoading" border stripe>
        <el-table-column prop="voucherNo" label="凭证号" width="140" />
        <el-table-column prop="voucherDate" label="凭证日期" width="120" />
        <el-table-column label="摘要" min-width="200">
          <template #default="{ row }">
            {{ row.details && row.details.length > 0 ? row.details[0].summary : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalDebit" label="借方金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalDebit) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalCredit" label="贷方金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalCredit) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { dashboardApi } from '@/api/system'
import { voucherApi } from '@/api/voucher'
import { formatAmount as formatAmountUtil } from '@/utils/format'
import type { DashboardStatsVO } from '@/types/system'
import type { VoucherVO } from '@/types/voucher'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const currentDate = computed(() => dayjs().format('YYYY年MM月DD日'))

const loading = ref(false)
const voucherLoading = ref(false)
const stats = reactive<DashboardStatsVO>({
  accountSetCount: 0,
  monthVoucherCount: 0,
  pendingAuditCount: 0,
  pendingTaxCount: 0,
  totalAssets: 0,
  totalRevenue: 0,
  totalProfit: 0,
  cashBalance: 0
})
const recentVouchers = ref<VoucherVO[]>([])

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '未审核', 1: '已审核', 2: '已过账' }
  return map[status] || '未知'
}

function statusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

function formatAmount(val: number | string | null | undefined): string {
  return formatAmountUtil(val)
}

function goToVoucherList() {
  router.push('/voucher')
}

async function loadStats() {
  loading.value = true
  try {
    const res = await dashboardApi.stats(appStore.currentAccountSetId || undefined)
    Object.assign(stats, res.data)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadRecentVouchers() {
  // 等待账套列表加载完成,避免 accountSetId=0 时查不到数据
  // (修复 Dashboard 最近凭证表恒为空,而凭证列表却有数据的不一致问题)
  if (!appStore.accountSetListLoaded) {
    await appStore.loadAccountSetList()
  }
  const accountSetId = appStore.currentAccountSetId
  if (!accountSetId) {
    recentVouchers.value = []
    return
  }
  voucherLoading.value = true
  try {
    const now = new Date()
    const res = await voucherApi.getPage({
      accountSetId,
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      pageNum: 1,
      pageSize: 5
    })
    recentVouchers.value = res.data.list
  } catch {
    // handled by interceptor
  } finally {
    voucherLoading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadRecentVouchers()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 20px;
}

.welcome-section {
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.stat-cards {
  margin-bottom: 20px;

  .stat-card {
    .stat-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .stat-info {
      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-bottom: 8px;
      }
    }

    .stat-icon {
      font-size: 48px;
      opacity: 0.8;
    }
  }
}

.recent-vouchers-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
