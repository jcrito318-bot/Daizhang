<template>
  <div class="asset-depreciation-container">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="年度">
          <el-date-picker v-model="searchForm.year" type="year" placeholder="请选择年度" value-format="YYYY" />
        </el-form-item>
        <el-form-item label="月份">
          <el-select v-model="searchForm.month" placeholder="请选择月份" clearable>
            <el-option v-for="i in 12" :key="i" :label="`${i}月`" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleCalculateDepreciation">计提折旧</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>折旧记录</span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="assetCode" label="资产编号" width="120" />
        <el-table-column prop="assetName" label="资产名称" width="150" />
        <el-table-column prop="year" label="年度" width="80" />
        <el-table-column prop="month" label="月份" width="80">
          <template #default="{ row }">
            {{ row.month }}月
          </template>
        </el-table-column>
        <el-table-column prop="depreciationAmount" label="本月折旧额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.depreciationAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="accumulatedDepreciation" label="累计折旧" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.accumulatedDepreciation) }}
          </template>
        </el-table-column>
        <el-table-column prop="netValue" label="净值" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.netValue) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assetApi } from '@/api/asset'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  year: '' as string | null,
  month: null as number | null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatAmount = (amount: number) => {
  return amount ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await assetApi.getDepreciationRecordPage({
      accountSetId: appStore.currentAccountSetId ?? undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      year: searchForm.year ? Number(searchForm.year) : undefined,
      month: searchForm.month ?? undefined
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
    ElMessage.error('加载折旧记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.year = ''
  searchForm.month = null
  handleSearch()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handleCalculateDepreciation = () => {
  if (!searchForm.year || !searchForm.month) {
    ElMessage.warning('请选择年度和月份')
    return
  }
  const accountSetId = appStore.currentAccountSetId
  if (!accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  ElMessageBox.confirm(`确定要对${searchForm.year}年${searchForm.month}月进行计提折旧吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await assetApi.calculateDepreciation({
        accountSetId,
        year: Number(searchForm.year),
        month: searchForm.month!
      })
      ElMessage.success('计提折旧成功')
      loadData()
    } catch (error) {
      console.error(error)
      ElMessage.error('操作失败')
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.asset-depreciation-container {
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

.amount {
  font-weight: bold;
  color: #67c23a;
}
</style>
