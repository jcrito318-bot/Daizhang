<template>
  <div class="asset-list-container">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="资产名称">
          <el-input v-model="searchForm.assetName" placeholder="请输入资产名称" clearable />
        </el-form-item>
        <el-form-item label="资产编号">
          <el-input v-model="searchForm.assetCode" placeholder="请输入资产编号" clearable />
        </el-form-item>
        <el-form-item label="资产状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="在用" :value="1" />
            <el-option label="闲置" :value="2" />
            <el-option label="报废" :value="3" />
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
          <span>固定资产列表</span>
          <el-button type="primary" @click="handleAdd">新增资产</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border empty-text="暂无资产数据,可点击「新增资产」或调整筛选条件">
        <el-table-column prop="assetCode" label="资产编号" width="120" />
        <el-table-column prop="assetName" label="资产名称" width="150" />
        <el-table-column prop="categoryName" label="资产分类" width="120" />
        <el-table-column prop="purchaseDate" label="购入日期" width="120" />
        <el-table-column prop="purchaseAmount" label="原值" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.purchaseAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="accumulatedDeprecation" label="累计折旧" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.accumulatedDeprecation) }}
          </template>
        </el-table-column>
        <el-table-column prop="netValue" label="净值" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.netValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产编号" prop="assetCode">
              <el-input v-model="form.assetCode" placeholder="请输入资产编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="请输入资产名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购入日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="请选择" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="原值" prop="purchaseAmount">
              <el-input-number v-model="form.purchaseAmount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折旧方法">
              <el-select v-model="form.depreciationMethod" style="width: 100%">
                <el-option label="直线法" value="straight_line" />
                <el-option label="双倍余额递减法" value="double_declining" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用年限(月)">
              <el-input-number v-model="form.usefulLife" :min="1" :max="600" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="残值">
              <el-input-number v-model="form.residualValue" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { assetApi } from '@/api/asset'
import { useAppStore } from '@/stores/app'
import type { FixedAssetVO, AssetCategoryVO } from '@/types/asset'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<FixedAssetVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const categories = ref<AssetCategoryVO[]>([])
const submitLoading = ref(false)

const searchForm = reactive({
  assetName: '',
  assetCode: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null as number | null,
  assetCode: '',
  assetName: '',
  categoryId: null as number | null,
  purchaseDate: '',
  purchaseAmount: 0,
  depreciationMethod: 'straight_line',
  usefulLife: 60,
  residualValue: 0,
  remark: ''
})

const rules: FormRules = {
  assetCode: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  purchaseAmount: [{ required: true, message: '请输入原值', trigger: 'blur' }]
}

const formatAmount = (amount: number) => {
  return amount ? `¥${amount.toFixed(2)}` : '¥0.00'
}

// BUG-05 修复:后端 FixedAssetQueryRequest 包含 accountSetId 字段,前端必须传入,
// 否则后端 mybatis-plus eq(null) 会忽略该条件,返回所有账套的资产数据(IDOR 风险)。
const loadData = async () => {
  if (!appStore.currentAccountSetId) {
    tableData.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const res = await assetApi.getAssetPage({
      accountSetId: appStore.currentAccountSetId,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  if (!appStore.currentAccountSetId) {
    categories.value = []
    return
  }
  try {
    // BUG-05 修复:资产分类查询同样需要 accountSetId 隔离
    const res = await assetApi.getCategoryPage({
      accountSetId: appStore.currentAccountSetId,
      pageNum: 1,
      pageSize: 100
    })
    categories.value = res.data.list
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.assetName = ''
  searchForm.assetCode = ''
  searchForm.status = undefined
  handleSearch()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增资产'
  resetForm()
  loadCategories()
  dialogVisible.value = true
}

const handleEdit = (row: FixedAssetVO) => {
  dialogTitle.value = '编辑资产'
  Object.assign(form, {
    id: row.id,
    assetCode: row.assetCode,
    assetName: row.assetName,
    categoryId: row.categoryId,
    purchaseDate: row.purchaseDate,
    purchaseAmount: row.purchaseAmount,
    depreciationMethod: row.depreciationMethod,
    usefulLife: row.usefulLife,
    residualValue: row.residualValue,
    remark: row.remark
  })
  loadCategories()
  dialogVisible.value = true
}

const handleDelete = (row: FixedAssetVO) => {
  ElMessageBox.confirm('确定要删除该资产吗？', '提示', { type: 'warning' }).then(async () => {
    await assetApi.deleteAsset(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!appStore.currentAccountSetId) {
    ElMessage.warning('请先在右上角选择账套')
    return
  }
  submitLoading.value = true
  try {
    if (form.id) {
      await assetApi.updateAsset(form.id, {
        assetName: form.assetName,
        categoryId: form.categoryId ?? undefined,
        depreciationMethod: form.depreciationMethod,
        usefulLife: form.usefulLife,
        residualValue: form.residualValue,
        remark: form.remark
      })
    } else {
      await assetApi.createAsset({
        accountSetId: appStore.currentAccountSetId,
        assetCode: form.assetCode,
        assetName: form.assetName,
        categoryId: form.categoryId!,
        purchaseDate: form.purchaseDate,
        purchaseAmount: form.purchaseAmount,
        depreciationMethod: form.depreciationMethod,
        usefulLife: form.usefulLife,
        residualValue: form.residualValue,
        remark: form.remark
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    assetCode: '',
    assetName: '',
    categoryId: null,
    purchaseDate: '',
    purchaseAmount: 0,
    depreciationMethod: 'straight_line',
    usefulLife: 60,
    residualValue: 0,
    remark: ''
  })
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.asset-list-container {
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
