<template>
  <div class="account-set-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="编码">
          <el-input v-model="queryForm.code" placeholder="请输入账套编码" clearable />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="queryForm.name" placeholder="请输入账套名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
          <span>账套列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新增账套
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="companyName" label="公司名称" width="200" />
        <el-table-column prop="industryType" label="行业类型" width="120" />
        <el-table-column label="启用期间" width="120">
          <template #default="{ row }">
            {{ row.startYear }}年{{ String(row.startMonth).padStart(2, '0') }}月
          </template>
        </el-table-column>
        <el-table-column prop="taxpayerType" label="纳税人类型" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleInit(row)" v-if="row.status === 0">初始化</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入账套编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入账套名称" />
        </el-form-item>
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="行业类型" prop="industryType">
          <el-select v-model="form.industryType" placeholder="请选择行业类型" style="width: 100%">
            <el-option label="工业" value="工业" />
            <el-option label="商业" value="商业" />
            <el-option label="服务业" value="服务业" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="会计准则" prop="accountingStandard">
          <el-select v-model="form.accountingStandard" placeholder="请选择会计准则" style="width: 100%">
            <el-option label="企业会计准则" value="企业会计准则" />
            <el-option label="小企业会计准则" value="小企业会计准则" />
            <el-option label="民间非营利组织会计制度" value="民间非营利组织会计制度" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用年份" prop="startYear">
          <el-input-number v-model="form.startYear" :min="2000" :max="2100" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="启用月份" prop="startMonth">
          <el-input-number v-model="form.startMonth" :min="1" :max="12" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="币种" prop="currencyCode">
          <el-select v-model="form.currencyCode" placeholder="请选择币种" style="width: 100%">
            <el-option label="人民币" value="CNY" />
            <el-option label="美元" value="USD" />
            <el-option label="欧元" value="EUR" />
          </el-select>
        </el-form-item>
        <el-form-item label="纳税人类型" prop="taxpayerType">
          <el-select v-model="form.taxpayerType" placeholder="请选择纳税人类型" style="width: 100%">
            <el-option label="一般纳税人" value="一般纳税人" />
            <el-option label="小规模纳税人" value="小规模纳税人" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
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
import { accountSetApi } from '@/api/accountset'
import type { AccountSetVO, AccountSetCreateRequest, AccountSetQueryRequest } from '@/types/accountset'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const total = ref(0)
const tableData = ref<AccountSetVO[]>([])
const formRef = ref<FormInstance>()

const queryForm = reactive<AccountSetQueryRequest>({
  code: '',
  name: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const form = reactive<AccountSetCreateRequest>({
  code: '',
  name: '',
  companyName: '',
  industryType: '',
  accountingStandard: '',
  startYear: new Date().getFullYear(),
  startMonth: 1,
  currencyCode: 'CNY',
  taxpayerType: '',
  contactPerson: '',
  contactPhone: '',
  address: ''
})

const formRules: FormRules = {
  code: [{ required: true, message: '请输入账套编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入账套名称', trigger: 'blur' }],
  startYear: [{ required: true, message: '请输入启用年份', trigger: 'blur' }],
  startMonth: [{ required: true, message: '请输入启用月份', trigger: 'blur' }]
}

const dialogTitle = ref('新增账套')

async function loadData() {
  loading.value = true
  try {
    const res = await accountSetApi.getPage(queryForm)
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

function handleReset() {
  queryForm.code = ''
  queryForm.name = ''
  queryForm.status = undefined
  queryForm.pageNum = 1
  loadData()
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增账套'
  Object.assign(form, {
    code: '',
    name: '',
    companyName: '',
    industryType: '',
    accountingStandard: '',
    startYear: new Date().getFullYear(),
    startMonth: 1,
    currencyCode: 'CNY',
    taxpayerType: '',
    contactPerson: '',
    contactPhone: '',
    address: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: AccountSetVO) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑账套'
  Object.assign(form, {
    code: row.code,
    name: row.name,
    companyName: row.companyName,
    industryType: row.industryType,
    accountingStandard: row.accountingStandard,
    startYear: row.startYear,
    startMonth: row.startMonth,
    currencyCode: row.currencyCode,
    taxpayerType: row.taxpayerType,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    address: row.address
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await accountSetApi.update(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await accountSetApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: AccountSetVO) {
  await ElMessageBox.confirm(`确定要删除账套"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await accountSetApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleInit(row: AccountSetVO) {
  await ElMessageBox.confirm(`确定要初始化账套"${row.name}"吗？初始化后将生成默认科目体系。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await accountSetApi.init(row.id)
    ElMessage.success('初始化成功')
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
.account-set-container {
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
