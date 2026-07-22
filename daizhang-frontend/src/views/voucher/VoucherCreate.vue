<template>
  <div class="voucher-create-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑凭证' : '新增凭证' }}</span>
          <div>
            <el-button v-if="!isEdit" type="success" @click="handleOpenTemplateDialog">
              <el-icon><DocumentCopy /></el-icon>从模板创建
            </el-button>
            <el-button @click="handleBack">返回</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
            <el-tooltip content="Ctrl+S 保存 | Ctrl+Enter 新增行 | Alt+T 模板 | Esc 返回" placement="bottom">
              <el-icon class="shortcut-hint-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
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
            <el-autocomplete
              v-model="row.summary"
              :fetch-suggestions="fetchAbstractSuggestions"
              placeholder="请输入摘要(支持常用摘要搜索)"
              :class="{ 'field-error': detailTouched && !row.summary }"
              clearable
              style="width: 100%"
              @blur="markDetailTouched"
              @select="(item: AbstractSuggestionItem) => handleAbstractSelect(row, item)"
            >
              <template #default="{ item }">
                <div class="abstract-suggestion">
                  <span class="abstract-text">{{ item.value }}</span>
                  <span v-if="item.useCount > 0" class="abstract-count">使用 {{ item.useCount }} 次</span>
                </div>
              </template>
            </el-autocomplete>
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

    <!-- 模板选择对话框 -->
    <el-dialog
      v-model="templateDialogVisible"
      title="从模板创建"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-table
        :data="templateList"
        v-loading="templateLoading"
        border
        stripe
        highlight-current-row
        @current-change="handleTemplateCurrentChange"
      >
        <el-table-column prop="templateCode" label="编码" width="140" />
        <el-table-column prop="templateName" label="名称" min-width="180" />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.templateCategory" type="info">{{ row.templateCategory }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedTemplateId" @click="handleApplyTemplate">
          应用模板
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { DocumentCopy, Plus, QuestionFilled } from '@element-plus/icons-vue'
import { voucherApi, templateApi, abstractApi } from '@/api/voucher'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import { useShortcut } from '@/composables/useShortcut'
import type {
  VoucherCreateRequest,
  VoucherDetailRequest,
  VoucherWordVO,
  VoucherTemplateVO,
  AbstractLibraryVO
} from '@/types/voucher'
import type { SubjectVO } from '@/types/subject'

/**
 * el-autocomplete 摘要建议项(继承 AbstractLibraryVO 并附加 value 字段供 autocomplete 显示)
 */
interface AbstractSuggestionItem extends AbstractLibraryVO {
  value: string
}

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

// 凭证模板选择对话框状态
const templateDialogVisible = ref(false)
const templateList = ref<VoucherTemplateVO[]>([])
const templateLoading = ref(false)
const selectedTemplateId = ref<number>(0)

// 常用摘要追踪:记录"摘要文本 -> 摘要库ID"的映射,
// 用于凭证保存时调用 incrementUse 累计使用次数。
// key 为摘要文本,value 为摘要库 ID。手动输入(非从下拉选择)的摘要不会出现在此映射中。
const abstractIdMap = new Map<string, number>()

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

// ==================== 凭证模板 ====================

/**
 * 打开"从模板创建"对话框,加载当前账套的模板列表
 */
async function handleOpenTemplateDialog() {
  const accountSetId = appStore.currentAccountSetId || 0
  if (!accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  templateDialogVisible.value = true
  selectedTemplateId.value = 0
  templateLoading.value = true
  try {
    const res = await templateApi.getList(accountSetId)
    templateList.value = res.data
    if (templateList.value.length === 0) {
      ElMessage.info('当前账套暂无凭证模板,请先在"凭证模板"页面创建')
    }
  } catch {
    // handled by interceptor
  } finally {
    templateLoading.value = false
  }
}

/**
 * 模板列表行选中变化时记录选中ID
 */
function handleTemplateCurrentChange(row: VoucherTemplateVO | null) {
  selectedTemplateId.value = row ? row.id : 0
}

/**
 * 应用选中的模板:调用 apply 接口获取模板明细,
 * 根据 subjectCode 在已加载的科目树中解析 subjectId,填充表单。
 */
async function handleApplyTemplate() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择一个模板')
    return
  }
  try {
    const res = await templateApi.apply(selectedTemplateId.value)
    const template = res.data
    if (!template.details || template.details.length === 0) {
      ElMessage.warning('该模板没有分录明细,无法应用')
      return
    }

    // 根据 subjectCode 解析 subjectId(从已加载的科目树中查找)
    const unresolvedCodes: string[] = []
    const newDetails: VoucherDetailRequest[] = template.details.map((d, idx) => {
      const subject = flatSubjects.value.find(s => s.subjectCode === d.subjectCode)
      if (!subject) {
        unresolvedCodes.push(d.subjectCode)
      }
      // 同步追踪摘要(若模板摘要与摘要库中某条文本一致,后续保存时会自动累计使用次数;
      // 这里不做强匹配,以用户在 autocomplete 中的选择为准)
      return {
        lineNo: idx + 1,
        summary: d.summary || template.summary || '',
        subjectId: subject ? subject.id : 0,
        debit: d.debitAmount || 0,
        credit: d.creditAmount || 0
      }
    })

    form.details = newDetails
    // 若模板有摘要,同时填充到分录摘要(已在上方 map 中处理)
    formDirty.value = true
    templateDialogVisible.value = false

    if (unresolvedCodes.length > 0) {
      ElMessage.warning(`已应用模板,但以下科目编码在当前账套中不存在,请手动选择科目: ${unresolvedCodes.join(', ')}`)
    } else {
      ElMessage.success('已应用模板,请核对并补充日期、凭证字后保存')
    }
  } catch {
    // handled by interceptor
  }
}

/**
 * 从 URL query 参数加载模板(由凭证模板页"应用"按钮跳转过来时携带 templateId)
 */
async function loadTemplateFromQuery(templateId: number) {
  try {
    const res = await templateApi.apply(templateId)
    const template = res.data
    if (!template.details || template.details.length === 0) {
      ElMessage.warning('模板没有分录明细,无法应用')
      return
    }
    const unresolvedCodes: string[] = []
    // 等待科目树加载后再解析 subjectId(由调用方保证 subjectTree 已加载)
    const newDetails: VoucherDetailRequest[] = template.details.map((d, idx) => {
      const subject = flatSubjects.value.find(s => s.subjectCode === d.subjectCode)
      if (!subject) {
        unresolvedCodes.push(d.subjectCode)
      }
      return {
        lineNo: idx + 1,
        summary: d.summary || template.summary || '',
        subjectId: subject ? subject.id : 0,
        debit: d.debitAmount || 0,
        credit: d.creditAmount || 0
      }
    })
    form.details = newDetails
    formDirty.value = true
    if (unresolvedCodes.length > 0) {
      ElMessage.warning(`已应用模板,但以下科目编码在当前账套中不存在,请手动选择科目: ${unresolvedCodes.join(', ')}`)
    } else {
      ElMessage.success('已应用模板,请核对并补充日期、凭证字后保存')
    }
  } catch {
    // handled by interceptor
  }
}

// ==================== 常用摘要库 ====================

/**
 * el-autocomplete fetch-suggestions 回调:模糊搜索常用摘要,按使用次数 DESC 排序
 */
async function fetchAbstractSuggestions(queryString: string, cb: (items: AbstractSuggestionItem[]) => void) {
  const accountSetId = appStore.currentAccountSetId || 0
  if (!accountSetId) {
    cb([])
    return
  }
  try {
    const keyword = queryString || ''
    const res = await abstractApi.search(accountSetId, keyword, 10)
    const items: AbstractSuggestionItem[] = res.data.map(item => ({
      ...item,
      value: item.abstractText
    }))
    cb(items)
  } catch {
    cb([])
  }
}

/**
 * 用户从摘要下拉中选择一条时,记录摘要文本 -> 摘要库ID 的映射,
 * 用于凭证保存时调用 incrementUse 累计使用次数
 */
function handleAbstractSelect(_row: VoucherDetailRequest, item: AbstractSuggestionItem) {
  if (item.id) {
    abstractIdMap.set(item.abstractText, item.id)
  }
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
    // 仅新建模式下用当前账套覆盖;编辑模式保留 loadVoucherDetail 已设置的原始账套归属
    if (!isEdit.value) {
      form.accountSetId = appStore.currentAccountSetId || 0
    }

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

      // 凭证保存成功后,对使用了摘要库的摘要累计使用次数(异步执行,不阻塞跳转)
      // 仅对从下拉选择的摘要(存在于 abstractIdMap 中)调用 incrementUse,
      // 同一摘要文本只计一次。
      const usedAbstractIds = new Set<number>()
      for (const detail of validDetails) {
        const abstractId = abstractIdMap.get(detail.summary)
        if (abstractId) {
          usedAbstractIds.add(abstractId)
        }
      }
      usedAbstractIds.forEach(id => {
        abstractApi.incrementUse(id).catch(() => {
          // 使用次数累计失败不影响主流程,静默忽略
        })
      })

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

// 凭证录入快捷键(P5.3.1):Ctrl+S 保存 / Ctrl+Enter 新增分录行 / Alt+T 打开模板 / Esc 返回
// 仅绑定带修饰键的组合或 Escape,避免与 el-select/el-input-number 的 Enter/方向键行为冲突
useShortcut([
  { ctrl: true, key: 's', handler: () => handleSubmit() },
  { ctrl: true, key: 'enter', handler: () => handleAddRow() },
  { alt: true, key: 't', handler: () => handleOpenTemplateDialog() },
  { key: 'escape', handler: () => handleBack() }
])

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
    await loadVoucherDetail(Number(id))
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

  // 从 URL query 加载模板(由凭证模板页"应用"按钮跳转过来时携带 templateId)
  // 必须在科目树加载完成后执行,以便根据 subjectCode 解析 subjectId
  const templateId = route.query.templateId as string
  if (templateId) {
    await loadTemplateFromQuery(Number(templateId))
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

// 快捷键提示图标:与保存按钮垂直对齐
.shortcut-hint-icon {
  margin-left: 8px;
  font-size: 16px;
  color: #909399;
  cursor: help;
  vertical-align: middle;
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

// 摘要下拉建议项样式
.abstract-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .abstract-text {
    flex: 1;
  }

  .abstract-count {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
}
</style>
