<template>
  <div class="tax-declaration-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="税种">
          <el-select v-model="queryForm.taxType" placeholder="请选择税种" clearable style="width: 160px">
            <el-option label="增值税" value="增值税" />
            <el-option label="企业所得税" value="企业所得税" />
            <el-option label="个人所得税" value="个人所得税" />
            <el-option label="城建税" value="城建税" />
            <el-option label="教育费附加" value="教育费附加" />
            <el-option label="地方教育附加" value="地方教育附加" />
            <el-option label="印花税" value="印花税" />
            <el-option label="房产税" value="房产税" />
            <el-option label="城镇土地使用税" value="城镇土地使用税" />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="queryForm.month" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="未申报" :value="0" />
            <el-option label="已申报" :value="1" />
            <el-option label="已缴纳" :value="2" />
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
          <span>税务申报列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新增申报
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="year" label="年度" width="80" align="center" />
        <el-table-column prop="month" label="月份" width="70" align="center" />
        <el-table-column prop="taxType" label="税种" width="140" />
        <el-table-column prop="taxableAmount" label="应税金额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.taxableAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="taxRate" label="税率(%)" width="100" align="right">
          <template #default="{ row }">
            {{ row.taxRate != null ? row.taxRate : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="taxAmount" label="应纳税额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.taxAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="declaredAmount" label="申报金额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.declaredAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="实缴金额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.actualAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="declarationDate" label="申报日期" width="120" />
        <el-table-column prop="paymentDate" label="缴纳日期" width="120" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleDeclare(row)" v-if="row.status === 0">申报</el-button>
            <el-button link type="warning" @click="handlePay(row)" v-if="row.status === 1">缴纳</el-button>
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
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="税种" prop="taxType">
          <el-select v-model="form.taxType" placeholder="请选择税种" style="width: 100%">
            <el-option label="增值税" value="增值税" />
            <el-option label="企业所得税" value="企业所得税" />
            <el-option label="个人所得税" value="个人所得税" />
            <el-option label="城建税" value="城建税" />
            <el-option label="教育费附加" value="教育费附加" />
            <el-option label="地方教育附加" value="地方教育附加" />
            <el-option label="印花税" value="印花税" />
            <el-option label="房产税" value="房产税" />
            <el-option label="城镇土地使用税" value="城镇土地使用税" />
          </el-select>
        </el-form-item>
        <el-form-item label="申报期间" required>
          <div style="display: flex; gap: 8px;">
            <el-input-number
              v-model="form.year"
              :min="2000"
              :max="2100"
              placeholder="年度"
              style="flex: 1"
              controls-position="right"
            />
            <el-input-number
              v-model="form.month"
              :min="1"
              :max="12"
              placeholder="月份"
              style="flex: 1"
              controls-position="right"
            />
          </div>
        </el-form-item>
        <el-form-item label="应税金额" prop="taxableAmount">
          <el-input-number
            v-model="form.taxableAmount"
            :min="0"
            :precision="2"
            :step="1000"
            style="width: 100%"
            controls-position="right"
            @change="calcTaxAmount"
          />
        </el-form-item>
        <el-form-item label="税率(%)" prop="taxRate">
          <el-input-number
            v-model="form.taxRate"
            :min="0"
            :max="100"
            :precision="4"
            :step="0.1"
            style="width: 100%"
            controls-position="right"
            @change="calcTaxAmount"
          />
        </el-form-item>
        <el-form-item label="应纳税额">
          <el-input-number
            v-model="form.taxAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="申报金额" prop="declaredAmount">
          <el-input-number
            v-model="form.declaredAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="实缴金额" prop="actualAmount">
          <el-input-number
            v-model="form.actualAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="申报日期" prop="declarationDate">
          <el-date-picker
            v-model="form.declarationDate"
            type="date"
            placeholder="请选择申报日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="缴纳日期" prop="paymentDate">
          <el-date-picker
            v-model="form.paymentDate"
            type="date"
            placeholder="请选择缴纳日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
import { taxApi } from '@/api/tax'
import { useAppStore } from '@/stores/app'
import type { TaxDeclarationVO, TaxDeclarationCreateRequest, TaxDeclarationUpdateRequest, TaxDeclarationQueryRequest } from '@/types/tax'

const appStore = useAppStore()
const formRef = ref<FormInstance>()

const loading = ref(false)
const total = ref(0)
const tableData = ref<TaxDeclarationVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增申报')
const isEdit = ref(false)
const editId = ref<number>(0)
const submitLoading = ref(false)

const now = new Date()
const queryForm = reactive<TaxDeclarationQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  taxType: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const form = reactive<TaxDeclarationCreateRequest>({
  accountSetId: 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  taxType: '',
  taxableAmount: 0,
  taxRate: 0,
  taxAmount: 0,
  declaredAmount: 0,
  actualAmount: 0,
  declarationDate: '',
  paymentDate: '',
  remark: ''
})

const formRules: FormRules = {
  taxType: [{ required: true, message: '请选择税种', trigger: 'change' }],
  year: [{ required: true, message: '请输入年度', trigger: 'blur' }],
  month: [{ required: true, message: '请输入月份', trigger: 'blur' }]
}

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '未申报', 1: '已申报', 2: '已缴纳' }
  return map[status] || '未知'
}

function statusTagType(status: number): string {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] || 'info'
}

function formatAmount(val: number | null | undefined): string {
  if (val == null || val === 0) return ''
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcTaxAmount() {
  if (form.taxableAmount != null && form.taxRate != null) {
    form.taxAmount = Math.round((form.taxableAmount * form.taxRate / 100) * 100) / 100
  }
}

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await taxApi.getDeclarationPage(queryForm)
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
  queryForm.year = now.getFullYear()
  queryForm.month = now.getMonth() + 1
  queryForm.taxType = undefined
  queryForm.status = undefined
  queryForm.pageNum = 1
  loadData()
}

function resetForm() {
  Object.assign(form, {
    accountSetId: appStore.currentAccountSetId || 0,
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    taxType: '',
    taxableAmount: 0,
    taxRate: 0,
    taxAmount: 0,
    declaredAmount: 0,
    actualAmount: 0,
    declarationDate: '',
    paymentDate: '',
    remark: ''
  })
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增申报'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: TaxDeclarationVO) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑申报'
  Object.assign(form, {
    accountSetId: row.accountSetId,
    year: row.year,
    month: row.month,
    taxType: row.taxType,
    taxableAmount: row.taxableAmount,
    taxRate: row.taxRate,
    taxAmount: row.taxAmount,
    declaredAmount: row.declaredAmount,
    actualAmount: row.actualAmount,
    declarationDate: row.declarationDate,
    paymentDate: row.paymentDate,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  form.accountSetId = appStore.currentAccountSetId || 0
  submitLoading.value = true
  try {
    if (isEdit.value) {
      const updateData: TaxDeclarationUpdateRequest = {
        taxType: form.taxType,
        taxableAmount: form.taxableAmount,
        taxRate: form.taxRate,
        taxAmount: form.taxAmount,
        declaredAmount: form.declaredAmount,
        actualAmount: form.actualAmount,
        declarationDate: form.declarationDate || undefined,
        paymentDate: form.paymentDate || undefined,
        remark: form.remark || undefined
      }
      await taxApi.updateDeclaration(editId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      await taxApi.createDeclaration(form)
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

async function handleDeclare(row: TaxDeclarationVO) {
  await ElMessageBox.confirm(`确定要将"${row.taxType} ${row.year}年${row.month}月"标记为已申报吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await taxApi.declare(row.id)
    ElMessage.success('申报成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handlePay(row: TaxDeclarationVO) {
  await ElMessageBox.confirm(`确定要将"${row.taxType} ${row.year}年${row.month}月"标记为已缴纳吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await taxApi.pay(row.id)
    ElMessage.success('缴纳确认成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(row: TaxDeclarationVO) {
  await ElMessageBox.confirm(`确定要删除"${row.taxType} ${row.year}年${row.month}月"的申报记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await taxApi.deleteDeclaration(row.id)
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
.tax-declaration-container {
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
