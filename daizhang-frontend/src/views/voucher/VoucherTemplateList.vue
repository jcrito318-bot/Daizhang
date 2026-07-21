<template>
  <div class="template-list-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="模板编码">
          <el-input
            v-model="queryForm.templateCode"
            placeholder="请输入模板编码"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input
            v-model="queryForm.templateName"
            placeholder="请输入模板名称"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="queryForm.templateCategory"
            placeholder="请选择分类"
            clearable
            style="width: 140px"
          >
            <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>凭证模板列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增模板
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="templateCode" label="模板编码" width="140" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.templateCategory" type="info">{{ row.templateCategory }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" fixed="right" width="240" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleApply(row)">应用</el-button>
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
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑凭证模板' : '新增凭证模板'"
      width="1000px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="如 SALARY-001" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="如 计提工资" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" prop="templateCategory">
              <el-select v-model="form.templateCategory" placeholder="请选择分类" clearable style="width: 100%">
                <el-option v-for="cat in categoryOptions" :key="cat" :label="cat" :value="cat" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="凭证摘要">
              <el-input v-model="form.summary" placeholder="凭证摘要(可留空,使用时取分录摘要)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="分录明细" prop="details">
          <el-table :data="form.details" border class="detail-table">
            <el-table-column label="行号" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="科目" min-width="280">
              <template #default="{ row }">
                <el-select
                  v-model="row.subjectCode"
                  placeholder="请选择科目"
                  filterable
                  style="width: 100%"
                  @change="(val: string) => handleSubjectChange(row, val)"
                >
                  <el-option
                    v-for="subject in flatSubjects"
                    :key="subject.id"
                    :label="`${subject.subjectCode} ${subject.subjectName}`"
                    :value="subject.subjectCode"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="摘要" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.summary" placeholder="分录摘要" />
              </template>
            </el-table-column>
            <el-table-column label="借方金额" width="150" align="right">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.debitAmount"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  style="width: 100%"
                  @change="() => handleDebitChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="贷方金额" width="150" align="right">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.creditAmount"
                  :min="0"
                  :precision="2"
                  :controls="false"
                  style="width: 100%"
                  @change="() => handleCreditChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="handleRemoveDetail($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="add-row-btn">
            <el-button type="primary" link @click="handleAddDetail">
              <el-icon><Plus /></el-icon>增加一行
            </el-button>
            <span class="balance-info">
              <span :class="isBalanced ? 'balanced' : 'unbalanced'">
                {{ isBalanced ? '借贷平衡' : '借贷不平衡' }}
              </span>
              <span class="balance-detail">
                借方合计: {{ formatAmount(totalDebit) }} | 贷方合计: {{ formatAmount(totalCredit) }}
              </span>
            </span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { templateApi } from '@/api/voucher'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import type {
  VoucherTemplateVO,
  VoucherTemplateRequest,
  VoucherTemplateDetailRequest,
  VoucherTemplateQueryRequest
} from '@/types/voucher'
import type { SubjectVO } from '@/types/subject'

const router = useRouter()
const appStore = useAppStore()

// 模板分类下拉选项
const categoryOptions = ['工资', '折旧', '社保', '税金', '结转', '其他']

const loading = ref(false)
const total = ref(0)
const tableData = ref<VoucherTemplateVO[]>([])
const subjectTree = ref<SubjectVO[]>([])

const queryForm = reactive<VoucherTemplateQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  templateCode: '',
  templateName: '',
  templateCategory: '',
  pageNum: 1,
  pageSize: 10
})

// 将科目树扁平化(用于 el-select 选项)
const flatSubjects = computed(() => {
  const result: SubjectVO[] = []
  function flatten(list: SubjectVO[]) {
    for (const item of list) {
      result.push(item)
      if (item.children && item.children.length > 0) {
        flatten(item.children)
      }
    }
  }
  flatten(subjectTree.value)
  return result
})

// ==================== 列表查询 ====================

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  loading.value = true
  try {
    const res = await templateApi.getPage(queryForm)
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
  queryForm.templateCode = ''
  queryForm.templateName = ''
  queryForm.templateCategory = ''
  queryForm.pageNum = 1
  loadData()
}

// ==================== 新增/编辑对话框 ====================

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<VoucherTemplateRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  templateCode: '',
  templateName: '',
  templateCategory: '',
  summary: '',
  remark: '',
  details: [createEmptyDetail()]
})

const formRules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  details: [
    {
      validator: (_rule, value: VoucherTemplateDetailRequest[], callback) => {
        if (!value || value.length === 0) {
          callback(new Error('至少需要一行分录明细'))
          return
        }
        const invalid = value.find(d => !d.subjectCode)
        if (invalid) {
          callback(new Error('每行分录必须选择科目'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

// 借贷合计与平衡校验
const totalDebit = computed(() =>
  form.details.reduce((sum, d) => sum + (d.debitAmount || 0), 0)
)
const totalCredit = computed(() =>
  form.details.reduce((sum, d) => sum + (d.creditAmount || 0), 0)
)
const isBalanced = computed(
  () => Math.abs(totalDebit.value - totalCredit.value) < 0.01 && totalDebit.value > 0
)

function createEmptyDetail(): VoucherTemplateDetailRequest {
  return {
    subjectCode: '',
    subjectName: '',
    debitAmount: 0,
    creditAmount: 0,
    summary: ''
  }
}

function formatAmount(val: number): string {
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleSubjectChange(row: VoucherTemplateDetailRequest, code: string) {
  const subject = flatSubjects.value.find(s => s.subjectCode === code)
  if (subject) {
    row.subjectName = subject.subjectName
    // 若分录摘要为空,默认填充模板摘要
    if (!row.summary && form.summary) {
      row.summary = form.summary
    }
  }
}

function handleDebitChange(row: VoucherTemplateDetailRequest) {
  if (row.debitAmount && row.debitAmount > 0) {
    row.creditAmount = 0
  }
}

function handleCreditChange(row: VoucherTemplateDetailRequest) {
  if (row.creditAmount && row.creditAmount > 0) {
    row.debitAmount = 0
  }
}

function handleAddDetail() {
  form.details.push(createEmptyDetail())
}

function handleRemoveDetail(index: number) {
  if (form.details.length <= 1) {
    ElMessage.warning('至少保留一行分录')
    return
  }
  form.details.splice(index, 1)
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

async function handleEdit(row: VoucherTemplateVO) {
  resetForm()
  isEdit.value = true
  editId.value = row.id
  try {
    // 调用详情接口获取明细
    const res = await templateApi.getById(row.id)
    const data = res.data
    form.accountSetId = data.accountSetId
    form.templateCode = data.templateCode
    form.templateName = data.templateName
    form.templateCategory = data.templateCategory || ''
    form.summary = data.summary || ''
    form.remark = data.remark || ''
    form.details = (data.details && data.details.length > 0)
      ? data.details.map(d => ({
          subjectCode: d.subjectCode,
          subjectName: d.subjectName,
          debitAmount: d.debitAmount || 0,
          creditAmount: d.creditAmount || 0,
          summary: d.summary || ''
        }))
      : [createEmptyDetail()]
    dialogVisible.value = true
  } catch {
    // handled by interceptor
  }
}

function resetForm() {
  form.accountSetId = appStore.currentAccountSetId || 0
  form.templateCode = ''
  form.templateName = ''
  form.templateCategory = ''
  form.summary = ''
  form.remark = ''
  form.details = [createEmptyDetail()]
  formRef.value?.clearValidate()
}

function handleDialogClosed() {
  resetForm()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    // 过滤掉没有科目或金额全为0的分录
    const validDetails = form.details.filter(
      d => d.subjectCode && ((d.debitAmount && d.debitAmount > 0) || (d.creditAmount && d.creditAmount > 0))
    )
    if (validDetails.length === 0) {
      ElMessage.warning('请至少填写一行有效分录(含科目和金额)')
      return
    }

    if (!isBalanced.value) {
      ElMessage.warning('借贷不平衡,请检查金额')
      return
    }

    form.accountSetId = appStore.currentAccountSetId || 0
    submitLoading.value = true
    try {
      const payload: VoucherTemplateRequest = {
        accountSetId: form.accountSetId,
        templateCode: form.templateCode,
        templateName: form.templateName,
        templateCategory: form.templateCategory || undefined,
        summary: form.summary || undefined,
        remark: form.remark || undefined,
        details: validDetails
      }
      if (isEdit.value) {
        await templateApi.update(editId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await templateApi.create(payload)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      // handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

// ==================== 删除 ====================

async function handleDelete(row: VoucherTemplateVO) {
  try {
    await ElMessageBox.confirm(`确定要删除模板"${row.templateName}"吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await templateApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

// ==================== 应用模板 ====================

async function handleApply(row: VoucherTemplateVO) {
  // 跳转到凭证创建页,带模板ID参数,VoucherCreate 在 onMounted 中读取并加载
  router.push({ path: '/voucher/create', query: { templateId: String(row.id) } })
}

// ==================== 初始化 ====================

async function loadSubjects() {
  const accountSetId = appStore.currentAccountSetId || 0
  if (!accountSetId) return
  try {
    const res = await subjectApi.getTree(accountSetId)
    subjectTree.value = res.data
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  loadSubjects()
  loadData()
})
</script>

<style scoped lang="scss">
.template-list-container {
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

.detail-table {
  width: 100%;
}

.add-row-btn {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.balance-info {
  .balanced {
    color: #67c23a;
    font-weight: 600;
  }

  .unbalanced {
    color: #f56c6c;
    font-weight: 600;
  }

  .balance-detail {
    margin-left: 12px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
