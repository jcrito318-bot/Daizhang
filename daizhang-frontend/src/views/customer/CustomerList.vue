<template>
  <div class="customer-list-container">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="客户名称">
          <el-input v-model="searchForm.customerName" placeholder="请输入客户名称" clearable />
        </el-form-item>
        <el-form-item label="客户编号">
          <el-input v-model="searchForm.customerCode" placeholder="请输入客户编号" clearable />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="searchForm.customerType" placeholder="请选择" clearable>
            <el-option label="企业" value="enterprise" />
            <el-option label="个人" value="individual" />
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
          <span>客户列表</span>
          <el-button type="primary" @click="handleAdd">新增客户</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border empty-text="暂无客户数据,可点击「新增客户」或调整筛选条件">
        <el-table-column prop="customerCode" label="客户编号" width="120" />
        <el-table-column prop="customerName" label="客户名称" width="180" />
        <el-table-column prop="customerType" label="客户类型" width="100" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
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
            <el-form-item label="客户编号" prop="customerCode">
              <el-input v-model="form.customerCode" placeholder="请输入客户编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户类型">
              <el-select v-model="form.customerType" placeholder="请选择" style="width: 100%">
                <el-option label="企业" value="enterprise" />
                <el-option label="个人" value="individual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { customerApi } from '@/api/customer'
import { useAppStore } from '@/stores/app'
import type { CustomerVO } from '@/types/customer'

const router = useRouter()
const appStore = useAppStore()
const loading = ref(false)
// BF-06 修复:将 any[] 替换为 CustomerVO[],提供编译期类型保护
const tableData = ref<CustomerVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const searchForm = reactive({
  customerName: '',
  customerCode: '',
  customerType: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null as number | null,
  customerCode: '',
  customerName: '',
  customerType: 'enterprise',
  contactPerson: '',
  contactPhone: '',
  email: '',
  address: '',
  remark: ''
})

const rules: FormRules = {
  customerCode: [{ required: true, message: '请输入客户编号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
}

// BUG-05 修复:客户查询需要 accountSetId 隔离,避免 IDOR
const loadData = async () => {
  if (!appStore.currentAccountSetId) {
    tableData.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const res = await customerApi.getPage({
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

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.customerName = ''
  searchForm.customerCode = ''
  searchForm.customerType = ''
  handleSearch()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增客户'
  resetForm()
  dialogVisible.value = true
}

const handleView = (row: CustomerVO) => {
  router.push(`/customer/${row.id}`)
}

const handleEdit = (row: CustomerVO) => {
  dialogTitle.value = '编辑客户'
  Object.assign(form, {
    id: row.id,
    customerCode: row.customerCode,
    customerName: row.customerName,
    customerType: row.customerType,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    email: row.email,
    address: row.address,
    remark: row.remark
  })
  dialogVisible.value = true
}

const handleDelete = (row: CustomerVO) => {
  ElMessageBox.confirm('确定要删除该客户吗？', '提示', { type: 'warning' }).then(async () => {
    await customerApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
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
      await customerApi.update(form.id, {
        customerName: form.customerName,
        customerType: form.customerType,
        contactPerson: form.contactPerson,
        contactPhone: form.contactPhone,
        email: form.email,
        address: form.address,
        remark: form.remark
      })
    } else {
      await customerApi.create({
        accountSetId: appStore.currentAccountSetId,
        customerCode: form.customerCode,
        customerName: form.customerName,
        customerType: form.customerType,
        contactPerson: form.contactPerson,
        contactPhone: form.contactPhone,
        email: form.email,
        address: form.address,
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
    customerCode: '',
    customerName: '',
    customerType: 'enterprise',
    contactPerson: '',
    contactPhone: '',
    email: '',
    address: '',
    remark: ''
  })
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.customer-list-container {
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
