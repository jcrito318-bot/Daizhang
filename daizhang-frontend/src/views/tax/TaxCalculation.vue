<template>
  <div class="tax-calculation-container">
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
        <el-form-item label="计算项目">
          <el-input v-model="queryForm.calculationItem" placeholder="请输入计算项目" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAutoCalculate">
            <el-icon><MagicStick /></el-icon>自动计算
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>税务计算明细</span>
          <div>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>新增计算项
            </el-button>
            <el-button type="info" @click="handleExport">
              <el-icon><Download /></el-icon>导出
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe show-summary :summary-method="getSummary">
        <el-table-column prop="year" label="年度" width="80" align="center" />
        <el-table-column prop="month" label="月份" width="70" align="center" />
        <el-table-column prop="taxType" label="税种" width="140" />
        <el-table-column prop="calculationItem" label="计算项目" min-width="180" />
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="rate" label="税率(%)" width="100" align="right">
          <template #default="{ row }">
            {{ row.rate != null ? row.rate : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="taxAmount" label="税额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.taxAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createByName" label="创建人" width="100" />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="计算期间" required>
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
        <el-form-item label="计算项目" prop="calculationItem">
          <el-input v-model="form.calculationItem" placeholder="请输入计算项目" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0"
            :precision="2"
            :step="1000"
            style="width: 100%"
            controls-position="right"
            @change="calcTaxAmount"
          />
        </el-form-item>
        <el-form-item label="税率(%)" prop="rate">
          <el-input-number
            v-model="form.rate"
            :min="0"
            :max="100"
            :precision="4"
            :step="0.1"
            style="width: 100%"
            controls-position="right"
            @change="calcTaxAmount"
          />
        </el-form-item>
        <el-form-item label="税额">
          <el-input-number
            v-model="form.taxAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            controls-position="right"
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

    <!-- 自动计算弹窗 -->
    <el-dialog
      v-model="autoCalcVisible"
      title="自动计算税额"
      width="500px"
      destroy-on-close
    >
      <el-form ref="autoCalcFormRef" :model="autoCalcForm" :rules="autoCalcRules" label-width="110px">
        <el-form-item label="计算期间" required>
          <div style="display: flex; gap: 8px;">
            <el-input-number
              v-model="autoCalcForm.year"
              :min="2000"
              :max="2100"
              placeholder="年度"
              style="flex: 1"
              controls-position="right"
            />
            <el-input-number
              v-model="autoCalcForm.month"
              :min="1"
              :max="12"
              placeholder="月份"
              style="flex: 1"
              controls-position="right"
            />
          </div>
        </el-form-item>
        <el-form-item label="税种" prop="taxType">
          <el-select v-model="autoCalcForm.taxType" placeholder="请选择税种" style="width: 100%">
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
      </el-form>
      <template #footer>
        <el-button @click="autoCalcVisible = false">取消</el-button>
        <el-button type="primary" :loading="autoCalcLoading" @click="doAutoCalculate">开始计算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, TableColumnCtx } from 'element-plus'
import { taxApi } from '@/api/tax'
import { useAppStore } from '@/stores/app'
import type { TaxCalculationVO, TaxCalculationCreateRequest, TaxCalculationUpdateRequest, TaxCalculationQueryRequest } from '@/types/tax'

const appStore = useAppStore()
const formRef = ref<FormInstance>()
const autoCalcFormRef = ref<FormInstance>()

const loading = ref(false)
const total = ref(0)
const tableData = ref<TaxCalculationVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增计算项')
const isEdit = ref(false)
const editId = ref<number>(0)
const submitLoading = ref(false)

const autoCalcVisible = ref(false)
const autoCalcLoading = ref(false)

const now = new Date()
const queryForm = reactive<TaxCalculationQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  taxType: undefined,
  calculationItem: '',
  pageNum: 1,
  pageSize: 10
})

const form = reactive<TaxCalculationCreateRequest>({
  accountSetId: 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  taxType: '',
  calculationItem: '',
  amount: 0,
  rate: 0,
  taxAmount: 0,
  remark: ''
})

const autoCalcForm = reactive({
  accountSetId: 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  taxType: ''
})

const formRules: FormRules = {
  taxType: [{ required: true, message: '请选择税种', trigger: 'change' }],
  year: [{ required: true, message: '请输入年度', trigger: 'blur' }],
  month: [{ required: true, message: '请输入月份', trigger: 'blur' }],
  calculationItem: [{ required: true, message: '请输入计算项目', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  rate: [{ required: true, message: '请输入税率', trigger: 'blur' }]
}

const autoCalcRules: FormRules = {
  taxType: [{ required: true, message: '请选择税种', trigger: 'change' }],
  year: [{ required: true, message: '请输入年度', trigger: 'blur' }],
  month: [{ required: true, message: '请输入月份', trigger: 'blur' }]
}

function formatAmount(val: number | null | undefined): string {
  if (val == null || val === 0) return ''
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcTaxAmount() {
  if (form.amount != null && form.rate != null) {
    form.taxAmount = Math.round((form.amount * form.rate / 100) * 100) / 100
  }
}

function getSummary({ columns, data }: { columns: TableColumnCtx<TaxCalculationVO>[], data: TaxCalculationVO[] }) {
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.property === 'amount' || column.property === 'taxAmount') {
      const prop = column.property as 'amount' | 'taxAmount'
      const values = data.map(item => Number(item[prop]))
      if (!values.every(value => isNaN(value))) {
        const sum = values.reduce((prev, curr) => {
          const value = Number(curr)
          if (!isNaN(value)) {
            return prev + value
          } else {
            return prev
          }
        }, 0)
        sums[index] = formatAmount(sum)
      } else {
        sums[index] = ''
      }
    } else {
      sums[index] = ''
    }
  })
  return sums
}

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await taxApi.getCalculationPage(queryForm)
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
  queryForm.calculationItem = ''
  queryForm.pageNum = 1
  loadData()
}

function resetForm() {
  Object.assign(form, {
    accountSetId: appStore.currentAccountSetId || 0,
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    taxType: '',
    calculationItem: '',
    amount: 0,
    rate: 0,
    taxAmount: 0,
    remark: ''
  })
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增计算项'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: TaxCalculationVO) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑计算项'
  Object.assign(form, {
    accountSetId: row.accountSetId,
    year: row.year,
    month: row.month,
    taxType: row.taxType,
    calculationItem: row.calculationItem,
    amount: row.amount,
    rate: row.rate,
    taxAmount: row.taxAmount,
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
      const updateData: TaxCalculationUpdateRequest = {
        taxType: form.taxType,
        calculationItem: form.calculationItem,
        amount: form.amount,
        rate: form.rate,
        taxAmount: form.taxAmount,
        remark: form.remark || undefined
      }
      await taxApi.updateCalculation(editId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      await taxApi.createCalculation(form)
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

async function handleDelete(row: TaxCalculationVO) {
  await ElMessageBox.confirm(`确定要删除"${row.calculationItem}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await taxApi.deleteCalculation(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

function handleAutoCalculate() {
  autoCalcForm.accountSetId = appStore.currentAccountSetId || 0
  autoCalcForm.year = now.getFullYear()
  autoCalcForm.month = now.getMonth() + 1
  autoCalcForm.taxType = ''
  autoCalcVisible.value = true
}

async function doAutoCalculate() {
  const valid = await autoCalcFormRef.value?.validate().catch(() => false)
  if (!valid) return

  autoCalcForm.accountSetId = appStore.currentAccountSetId || 0
  if (!autoCalcForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }

  autoCalcLoading.value = true
  try {
    const res = await taxApi.calculateTax(
      autoCalcForm.accountSetId,
      autoCalcForm.year,
      autoCalcForm.month,
      autoCalcForm.taxType
    )
    ElMessage.success(`自动计算完成，计算税额：${formatAmount(res.data)}`)
    autoCalcVisible.value = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    autoCalcLoading.value = false
  }
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.tax-calculation-container {
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
