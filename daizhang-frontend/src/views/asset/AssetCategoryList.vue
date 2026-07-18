<template>
  <div class="asset-category-list-container">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>资产分类</span>
          <el-button type="primary" @click="handleAdd">新增分类</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="categoryCode" label="分类编码" width="150" />
        <el-table-column prop="categoryName" label="分类名称" width="200" />
        <el-table-column prop="depreciationMethod" label="折旧方法" width="120" />
        <el-table-column prop="usefulLife" label="使用年限(月)" width="120" />
        <el-table-column prop="residualRate" label="残值率(%)" width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="折旧方法" prop="depreciationMethod">
          <el-select v-model="form.depreciationMethod" placeholder="请选择">
            <el-option label="直线法" value="straight_line" />
            <el-option label="双倍余额递减法" value="double_declining" />
            <el-option label="年数总和法" value="sum_of_years" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用年限(月)" prop="usefulLife">
          <el-input-number v-model="form.usefulLife" :min="1" :max="600" />
        </el-form-item>
        <el-form-item label="残值率(%)" prop="residualRate">
          <el-input-number v-model="form.residualRate" :min="0" :max="100" :precision="2" />
        </el-form-item>
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

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null as number | null,
  categoryCode: '',
  categoryName: '',
  depreciationMethod: 'straight_line',
  usefulLife: 60,
  residualRate: 5,
  remark: ''
})

const rules: FormRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await assetApi.getCategoryPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑分类'
  Object.assign(form, {
    id: row.id,
    categoryCode: row.categoryCode,
    categoryName: row.categoryName,
    depreciationMethod: row.depreciationMethod,
    usefulLife: row.usefulLife,
    residualRate: row.residualRate,
    remark: row.remark
  })
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' }).then(async () => {
    await assetApi.deleteCategory(row.id)
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
      await assetApi.updateCategory(form.id, {
        categoryName: form.categoryName,
        depreciationMethod: form.depreciationMethod,
        usefulLife: form.usefulLife,
        residualRate: form.residualRate,
        remark: form.remark
      })
    } else {
      await assetApi.createCategory({
        accountSetId: appStore.currentAccountSetId,
        categoryCode: form.categoryCode,
        categoryName: form.categoryName,
        depreciationMethod: form.depreciationMethod,
        usefulLife: form.usefulLife,
        residualRate: form.residualRate,
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
    categoryCode: '',
    categoryName: '',
    depreciationMethod: 'straight_line',
    usefulLife: 60,
    residualRate: 5,
    remark: ''
  })
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.asset-category-list-container {
  padding: 20px;
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
