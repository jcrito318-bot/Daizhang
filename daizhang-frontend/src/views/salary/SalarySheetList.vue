<template>
  <div class="salary-sheet-list-container">
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
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>工资表列表</span>
          <el-button type="primary" @click="handleAdd">新增工资表</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="year" label="年度" width="100" />
        <el-table-column prop="month" label="月份" width="100">
          <template #default="{ row }">
            {{ row.month }}月
          </template>
        </el-table-column>
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column prop="baseSalary" label="基本工资" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.baseSalary) }}
          </template>
        </el-table-column>
        <el-table-column prop="netSalary" label="实发工资" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.netSalary) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已确认' : '待确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleConfirm(row)" v-if="row.status !== 1">确认</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
import { salaryApi } from '@/api/salary'
import { useAppStore } from '@/stores/app'
import type { SalarySheetVO } from '@/types/salary'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<SalarySheetVO[]>([])

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

// BUG-05 修复:后端 SalarySheetQueryRequest 包含 accountSetId 字段,前端必须传入,
// 否则后端会按 mybatis-plus eq(null) 忽略该条件,可能返回所有账套数据(IDOR 风险)。
const loadData = async () => {
  // 未选择账套时不发起查询,避免越权返回所有账套数据
  if (!appStore.currentAccountSetId) {
    tableData.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const res = await salaryApi.getSalarySheetPage({
      accountSetId: appStore.currentAccountSetId,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      year: searchForm.year ? Number(searchForm.year) : undefined,
      month: searchForm.month ?? undefined
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
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

const handleAdd = () => {
  ElMessage.info('新增工资表功能开发中')
}

const handleConfirm = (row: SalarySheetVO) => {
  ElMessageBox.confirm('确定要确认该工资表吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await salaryApi.confirmSalarySheet(row.id)
    ElMessage.success('确认成功')
    loadData()
  })
}

const handleDelete = (row: SalarySheetVO) => {
  ElMessageBox.confirm('确定要删除该工资表吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await salaryApi.deleteSalarySheet(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.salary-sheet-list-container {
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
