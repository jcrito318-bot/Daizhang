<template>
  <div class="employee-list-container">
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="员工姓名">
          <el-input v-model="searchForm.employeeName" placeholder="请输入员工姓名" clearable />
        </el-form-item>
        <el-form-item label="员工编号">
          <el-input v-model="searchForm.employeeCode" placeholder="请输入员工编号" clearable />
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
          <span>员工列表</span>
          <el-button type="primary" @click="handleAdd">新增员工</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="employeeCode" label="员工编号" width="120" />
        <el-table-column prop="employeeName" label="员工姓名" width="120" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="position" label="职位" width="120" />
        <el-table-column prop="entryDate" label="入职日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="员工编号" prop="employeeCode">
          <el-input v-model="form.employeeCode" placeholder="请输入员工编号" />
        </el-form-item>
        <el-form-item label="员工姓名" prop="employeeName">
          <el-input v-model="form.employeeName" placeholder="请输入员工姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="form.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="入职日期" prop="entryDate">
          <el-date-picker v-model="form.entryDate" type="date" placeholder="请选择入职日期" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { salaryApi } from '@/api/salary'

const loading = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  employeeName: '',
  employeeCode: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null as number | null,
  employeeCode: '',
  employeeName: '',
  idCard: '',
  phone: '',
  department: '',
  position: '',
  entryDate: ''
})

const rules: FormRules = {
  employeeCode: [{ required: true, message: '请输入员工编号', trigger: 'blur' }],
  employeeName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await salaryApi.getEmployeePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data.list
    pagination.total = res.data.total
  } catch (error) {
    console.error(error)
    ElMessage.error('加载员工列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.employeeName = ''
  searchForm.employeeCode = ''
  handleSearch()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增员工'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑员工'
  Object.assign(form, {
    id: row.id,
    employeeCode: row.employeeCode,
    employeeName: row.employeeName,
    idCard: row.idCard,
    phone: row.phone,
    department: row.department,
    position: row.position,
    entryDate: row.entryDate
  })
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该员工吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await salaryApi.deleteEmployee(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await salaryApi.updateEmployee(form.id, {
      employeeCode: form.employeeCode,
      employeeName: form.employeeName,
      idCard: form.idCard,
      phone: form.phone,
      department: form.department,
      position: form.position,
      entryDate: form.entryDate
    })
  } else {
    await salaryApi.createEmployee({
      accountSetId: 1,
      employeeCode: form.employeeCode,
      employeeName: form.employeeName,
      idCard: form.idCard,
      phone: form.phone,
      department: form.department,
      position: form.position,
      entryDate: form.entryDate
    })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    employeeCode: '',
    employeeName: '',
    idCard: '',
    phone: '',
    department: '',
    position: '',
    entryDate: ''
  })
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.employee-list-container {
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
