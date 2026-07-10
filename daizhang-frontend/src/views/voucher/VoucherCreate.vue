<template>
  <div class="voucher-create-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑凭证' : '新增凭证' }}</span>
          <div>
            <el-button @click="handleBack">返回</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" class="voucher-header">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="凭证日期" prop="voucherDate">
              <el-date-picker
                v-model="form.voucherDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="凭证字" prop="voucherWordId">
              <el-select v-model="form.voucherWordId" placeholder="请选择凭证字" style="width: 100%">
                <el-option
                  v-for="word in voucherWordList"
                  :key="word.id"
                  :label="word.name"
                  :value="word.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="附件数">
              <el-input-number v-model="form.attachmentCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <div class="balance-check">
              <span :class="isBalanced ? 'balanced' : 'unbalanced'">
                {{ isBalanced ? '借贷平衡' : '借贷不平衡' }}
              </span>
              <span class="balance-detail">
                借方合计: {{ formatAmount(totalDebit) }} | 贷方合计: {{ formatAmount(totalCredit) }}
              </span>
            </div>
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="form.details" border class="detail-table">
        <el-table-column label="行号" width="60" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="摘要" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.summary" placeholder="请输入摘要" :class="{ 'field-error': detailTouched && !row.summary }" @blur="markDetailTouched" />
            <div v-if="detailTouched && !row.summary" class="inline-error">请输入摘要</div>
          </template>
        </el-table-column>
        <el-table-column label="科目" min-width="250">
          <template #default="{ row }">
            <el-select
              v-model="row.subjectId"
              placeholder="请选择科目"
              filterable
              style="width: 100%"
              :class="{ 'field-error': detailTouched && !row.subjectId }"
              @blur="markDetailTouched"
            >
              <el-option
                v-for="subject in flatSubjects"
                :key="subject.id"
                :label="`${subject.subjectCode} ${subject.subjectName}`"
                :value="subject.id"
              />
            </el-select>
            <div v-if="detailTouched && !row.subjectId" class="inline-error">请选择科目</div>
          </template>
        </el-table-column>
        <el-table-column label="借方金额" width="150" align="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.debit"
              :min="0"
              :precision="2"
              :controls="false"
              style="width: 100%"
              @change="handleDebitChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="贷方金额" width="150" align="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.credit"
              :min="0"
              :precision="2"
              :controls="false"
              style="width: 100%"
              @change="handleCreditChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleRemoveRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-row-btn">
        <el-button type="primary" link @click="handleAddRow">
          <el-icon><Plus /></el-icon>增加一行
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { voucherApi } from '@/api/voucher'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import type { VoucherCreateRequest, VoucherDetailRequest, VoucherWordVO } from '@/types/voucher'
import type { SubjectVO } from '@/types/subject'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const voucherWordList = ref<VoucherWordVO[]>([])
const subjectTree = ref<SubjectVO[]>([])
// 分录校验标记:用户尝试提交后置为true,显示字段级错误
const detailTouched = ref(false)
// 表单是否有未保存的改动(用于离开提示)
const formDirty = ref(false)

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

const form = reactive<VoucherCreateRequest & { attachmentCount?: number }>({
  accountSetId: appStore.currentAccountSetId || 0,
  voucherWordId: undefined,
  voucherDate: new Date().toISOString().slice(0, 10),
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  attachmentCount: 0,
  details: [createEmptyDetail()]
})

// 表头字段校验规则:Element Plus 表单内联红色错误提示
const formRules: FormRules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }],
  voucherWordId: [{ required: true, message: '请选择凭证字', trigger: 'change' }]
}

const totalDebit = computed(() => form.details.reduce((sum, d) => sum + (d.debit || 0), 0))
const totalCredit = computed(() => form.details.reduce((sum, d) => sum + (d.credit || 0), 0))
const isBalanced = computed(() => Math.abs(totalDebit.value - totalCredit.value) < 0.01 && totalDebit.value > 0)

function createEmptyDetail(): VoucherDetailRequest {
  return {
    lineNo: 0,
    summary: '',
    subjectId: 0,
    debit: 0,
    credit: 0
  }
}

function formatAmount(val: number): string {
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleDebitChange(row: VoucherDetailRequest) {
  formDirty.value = true
  if (row.debit && row.debit > 0) {
    row.credit = 0
  }
}

function handleCreditChange(row: VoucherDetailRequest) {
  formDirty.value = true
  if (row.credit && row.credit > 0) {
    row.debit = 0
  }
}

function markDetailTouched() {
  detailTouched.value = true
}

function handleAddRow() {
  formDirty.value = true
  form.details.push(createEmptyDetail())
}

function handleRemoveRow(index: number) {
  if (form.details.length <= 1) {
    ElMessage.warning('至少保留一行分录')
    return
  }
  formDirty.value = true
  form.details.splice(index, 1)
}

function handleBack() {
  router.push('/voucher')
}

// 标记有任意分录填写了内容(用于判断是否需要离开提示)
function hasAnyDetailFilled(): boolean {
  return form.details.some(d => d.summary || d.subjectId || (d.debit && d.debit > 0) || (d.credit && d.credit > 0))
}

async function handleSubmit() {
  detailTouched.value = true
  // 表头校验
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表头信息')
      return
    }

    const validDetails = form.details.filter(d => d.subjectId && (d.debit > 0 || d.credit > 0))
    if (validDetails.length === 0) {
      ElMessage.warning('请至少填写一行有效分录(含科目和金额)')
      return
    }

    // 校验有效分录的摘要
    const missingSummary = validDetails.find(d => !d.summary)
    if (missingSummary) {
      ElMessage.warning('请填写所有分录的摘要')
      return
    }

    if (!isBalanced.value) {
      ElMessage.warning('借贷不平衡，请检查金额')
      return
    }

    const date = new Date(form.voucherDate)
    form.year = date.getFullYear()
    form.month = date.getMonth() + 1
    form.accountSetId = appStore.currentAccountSetId || 0

    submitLoading.value = true
    try {
      const submitData = {
        ...form,
        details: validDetails.map((d, i) => ({ ...d, lineNo: i + 1 }))
      }
      if (isEdit.value) {
        await voucherApi.update(editId.value, submitData)
        ElMessage.success('更新成功')
      } else {
        await voucherApi.create(submitData)
        ElMessage.success('创建成功')
      }
      formDirty.value = false
      router.push('/voucher')
    } catch {
      // handled by interceptor
    } finally {
      submitLoading.value = false
    }
  })
}

async function loadVoucherDetail(id: number) {
  try {
    const res = await voucherApi.getById(id)
    const voucher = res.data
    isEdit.value = true
    editId.value = id
    form.accountSetId = voucher.accountSetId
    form.voucherWordId = voucher.voucherWordId
    form.voucherDate = voucher.voucherDate
    form.year = voucher.year
    form.month = voucher.month
    form.attachmentCount = voucher.attachmentCount
    form.details = voucher.details.map(d => ({
      lineNo: d.lineNo,
      summary: d.summary,
      subjectId: d.subjectId,
      auxiliaryId: d.auxiliaryId || undefined,
      debit: d.debit,
      credit: d.credit,
      quantity: d.quantity || undefined,
      unitPrice: d.unitPrice || undefined
    }))
    // 编辑模式加载完数据后,dirty 重置(尚未改动)
    formDirty.value = false
  } catch {
    // handled by interceptor
  }
}

// 离开页面提示:有未保存改动时弹窗确认
onBeforeRouteLeave(async (_to, _from, next) => {
  if (!formDirty.value) {
    next()
    return
  }
  // 仅在有实际填写内容时才提示,避免空表单也弹窗
  if (!hasAnyDetailFilled() && !isEdit.value) {
    next()
    return
  }
  try {
    await ElMessageBox.confirm('当前凭证尚未保存，离开后数据将丢失，是否继续？', '离开确认', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
      type: 'warning'
    })
    next()
  } catch {
    next(false)
  }
})

onMounted(async () => {
  const id = route.params.id as string
  if (id) {
    await loadVoucherDetail(id)
  }

  const accountSetId = appStore.currentAccountSetId || form.accountSetId
  if (accountSetId) {
    try {
      const [wordRes, subjectRes] = await Promise.all([
        voucherApi.getWordList(accountSetId),
        subjectApi.getTree(accountSetId)
      ])
      voucherWordList.value = wordRes.data
      subjectTree.value = subjectRes.data
    } catch {
      // handled by interceptor
    }
  }
})
</script>

<style scoped lang="scss">
.voucher-create-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.voucher-header {
  margin-bottom: 16px;
}

.balance-check {
  padding-top: 4px;

  .balanced {
    color: #67c23a;
    font-weight: 600;
  }

  .unbalanced {
    color: #f56c6c;
    font-weight: 600;
  }

  .balance-detail {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.detail-table {
  margin-bottom: 8px;
}

.add-row-btn {
  text-align: center;
  padding: 8px 0;
}

// 字段内联错误提示
.inline-error {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
}

// 字段错误时输入框红色边框
:deep(.field-error) {
  .el-input__wrapper,
  .el-select__wrapper {
    box-shadow: 0 0 0 1px #f56c6c inset !important;
  }
}
</style>
