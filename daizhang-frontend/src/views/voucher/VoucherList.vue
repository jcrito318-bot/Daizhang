<template>
  <div class="voucher-list-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="年度">
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="queryForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="未审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已过账" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证号">
          <el-input v-model="queryForm.voucherNo" placeholder="请输入凭证号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>凭证列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新增凭证
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
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
        <el-table-column prop="createByName" label="制单人" width="100" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="success" @click="handleAudit(row)" v-if="row.status === 0">审核</el-button>
            <el-button link type="warning" @click="handleUnaudit(row)" v-if="row.status === 1">反审核</el-button>
            <el-button link type="info" @click="handlePost(row)" v-if="row.status === 1">过账</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="row.status === 0">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { voucherApi } from '@/api/voucher'
import { useAppStore } from '@/stores/app'
import type { VoucherVO, VoucherQueryRequest } from '@/types/voucher'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const total = ref(0)
const tableData = ref<VoucherVO[]>([])

const now = new Date()
const queryForm = reactive<VoucherQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  status: undefined,
  voucherNo: '',
  pageNum: 1,
  pageSize: 10
})

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '未审核', 1: '已审核', 2: '已过账' }
  return map[status] || '未知'
}

function statusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

function formatAmount(val: number): string {
  if (val === 0) return ''
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await voucherApi.getPage(queryForm)
    tableData.value = res.data.list
    total.value = res.data.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  loadData()
}

function handleSizeChange(size: number) {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  loadData()
}

function handleReset() {
  queryForm.year = now.getFullYear()
  queryForm.month = now.getMonth() + 1
  queryForm.status = undefined
  queryForm.voucherNo = ''
  queryForm.pageNum = 1
  loadData()
}

function handleCreate() {
  router.push('/voucher/create')
}

function handleView(row: VoucherVO) {
  router.push(`/voucher/${row.id}`)
}

async function handleAudit(row: VoucherVO) {
  try {
    await ElMessageBox.confirm(`确定要审核凭证"${row.voucherNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消，不执行后续操作
    return
  }
  try {
    await voucherApi.audit(row.id)
    ElMessage.success('审核成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleUnaudit(row: VoucherVO) {
  try {
    await ElMessageBox.confirm(`确定要反审核凭证"${row.voucherNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消，不执行后续操作
    return
  }
  try {
    await voucherApi.unaudit(row.id)
    ElMessage.success('反审核成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handlePost(row: VoucherVO) {
  try {
    await ElMessageBox.confirm(`确定要过账凭证"${row.voucherNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消，不执行后续操作
    return
  }
  try {
    await voucherApi.post(row.id)
    ElMessage.success('过账成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(row: VoucherVO) {
  try {
    await ElMessageBox.confirm(`确定要删除凭证"${row.voucherNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消，不执行后续操作
    return
  }
  try {
    await voucherApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.voucher-list-container {
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
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
