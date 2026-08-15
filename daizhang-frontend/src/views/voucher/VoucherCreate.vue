<template>
  <div class="voucher-create-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑凭证' : '新增凭证' }}</span>
          <div>
            <el-button @click="handlePrint">
              <el-icon><Printer /></el-icon>打印
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>导出
            </el-button>
            <el-button v-if="!isEdit" type="success" @click="handleOpenTemplateDialog">
              <el-icon><DocumentCopy /></el-icon>从模板创建
            </el-button>
            <el-button @click="handleBack">返回</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
            <el-tooltip content="Ctrl+S 保存 | Ctrl+Enter 新增行 | Alt+T 模板 | Esc 返回" placement="bottom">
              <el-icon class="shortcut-hint-icon no-print"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 凭证表头信息 -->
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px" class="voucher-header">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="日期" prop="voucherDate">
              <el-date-picker
                v-model="form.voucherDate"
                type="date"
                placeholder="请选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                class="screen-only"
              />
              <span class="print-only">{{ form.voucherDate }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="凭证字" prop="voucherWordId">
              <el-select v-model="form.voucherWordId" placeholder="请选择凭证字" style="width: 100%" class="screen-only">
                <el-option
                  v-for="word in voucherWordList"
                  :key="word.id"
                  :label="word.name"
                  :value="word.id"
                />
              </el-select>
              <span class="print-only">{{ getVoucherWordName(form.voucherWordId) }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="附件数">
              <el-input-number v-model="form.attachmentCount" :min="0" style="width: 100%" class="screen-only" />
              <span class="print-only">{{ form.attachmentCount || 0 }} 张</span>
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

      <!-- 记账凭证表格 -->
      <div class="voucher-table-wrapper">
        <table class="voucher-table">
          <thead>
            <tr>
              <th rowspan="2" class="col-summary">摘要</th>
              <th rowspan="2" class="col-subject">总账科目</th>
              <th rowspan="2" class="col-subject">明细科目</th>
              <th :colspan="AMOUNT_LABELS.length" class="amount-header debit-header">借方金额</th>
              <th :colspan="AMOUNT_LABELS.length" class="amount-header credit-header">贷方金额</th>
              <th rowspan="2" class="col-action no-print">操作</th>
            </tr>
            <tr>
              <th v-for="(label, i) in AMOUNT_LABELS" :key="'dh' + i" class="digit-th">{{ label }}</th>
              <th v-for="(label, i) in AMOUNT_LABELS" :key="'ch' + i" class="digit-th">{{ label }}</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(row, index) in form.details" :key="index">
              <!-- 数据行 -->
              <tr class="data-row">
                <td class="summary-cell">
                  <div class="screen-only">
                    <el-autocomplete
                      v-model="row.summary"
                      :fetch-suggestions="fetchAbstractSuggestions"
                      placeholder="请输入摘要"
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
                  </div>
                  <span class="print-only">{{ row.summary }}</span>
                </td>
                <td class="subject-cell">
                  <div class="screen-only">
                    <el-select
                      v-model="row.generalSubjectId"
                      placeholder="选择总账科目"
                      filterable
                      style="width: 100%"
                      :class="{ 'field-error': detailTouched && !row.generalSubjectId }"
                      @change="handleGeneralSubjectChange(row)"
                      @blur="markDetailTouched"
                    >
                      <el-option
                        v-for="subject in generalSubjects"
                        :key="subject.id"
                        :label="`${subject.subjectCode} ${subject.subjectName}`"
                        :value="subject.id"
                      />
                    </el-select>
                  </div>
                  <span class="print-only">{{ getSubjectName(row.generalSubjectId) }}</span>
                </td>
                <td class="subject-cell">
                  <div class="screen-only">
                    <el-select
                      v-model="row.detailSubjectId"
                      placeholder="选择明细科目"
                      filterable
                      clearable
                      style="width: 100%"
                      @change="handleDetailSubjectChange(row)"
                    >
                      <el-option
                        v-for="subject in getDetailSubjectOptions(row.generalSubjectId)"
                        :key="subject.id"
                        :label="`${subject.subjectCode} ${subject.subjectName}`"
                        :value="subject.id"
                      />
                    </el-select>
                  </div>
                  <span class="print-only">{{ row.detailSubjectId ? getSubjectName(row.detailSubjectId) : '' }}</span>
                </td>
                <!-- 借方金额 11 位格 -->
                <td v-for="(d, i) in getDebitDigits(row)" :key="'dd' + i" class="digit-cell">{{ d }}</td>
                <!-- 贷方金额 11 位格 -->
                <td v-for="(d, i) in getCreditDigits(row)" :key="'cd' + i" class="digit-cell">{{ d }}</td>
                <td class="action-cell no-print">
                  <el-button link type="danger" @click="handleRemoveRow(index)">删除</el-button>
                </td>
              </tr>
              <!-- 金额输入行（打印时隐藏） -->
              <tr class="amount-input-row no-print">
                <td colspan="3" class="input-row-label">
                  <el-icon><EditPen /></el-icon>
                  第 {{ index + 1 }} 行 金额输入
                </td>
                <td :colspan="AMOUNT_LABELS.length" class="amount-input-cell">
                  <el-input
                    v-model="row.debitInput"
                    placeholder="借方金额（如 5000 或 5000.56）"
                    size="small"
                    @input="handleDebitInput(row)"
                  >
                    <template #prepend>借</template>
                  </el-input>
                </td>
                <td :colspan="AMOUNT_LABELS.length" class="amount-input-cell">
                  <el-input
                    v-model="row.creditInput"
                    placeholder="贷方金额（如 5000 或 5000.56）"
                    size="small"
                    @input="handleCreditInput(row)"
                  >
                    <template #prepend>贷</template>
                  </el-input>
                </td>
                <td></td>
              </tr>
            </template>
            <!-- 合计行 -->
            <tr class="total-row">
              <td :colspan="3" class="total-label">合 计</td>
              <td v-for="(d, i) in totalDebitDigits" :key="'td' + i" class="digit-cell total-digit">{{ d }}</td>
              <td v-for="(d, i) in totalCreditDigits" :key="'tc' + i" class="digit-cell total-digit">{{ d }}</td>
              <td class="no-print"></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="add-row-btn no-print">
        <el-button type="primary" link @click="handleAddRow">
          <el-icon><Plus /></el-icon>增加一行
        </el-button>
      </div>

      <!-- 内联错误提示 -->
      <div v-if="detailTouched" class="inline-error no-print" style="text-align: center; padding: 4px 0;">
        <span v-if="form.details.some(d => !d.summary && (d.generalSubjectId || d.debit > 0 || d.credit > 0))">
          请填写所有有效分录的摘要
        </span>
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
import { DocumentCopy, Plus, QuestionFilled, Printer, Download, EditPen } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { voucherApi, templateApi, abstractApi } from '@/api/voucher'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import { useShortcut } from '@/composables/useShortcut'
import {
  AMOUNT_LABELS,
  parseAmountToDigits,
  numberToInput,
  numberToDigits,
  sanitizeAmountInput
} from '@/utils/amountDigits'
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

/**
 * 凭证分录行:在 VoucherDetailRequest 基础上扩展总账/明细科目选择和金额输入文本
 */
interface VoucherDetailRow extends VoucherDetailRequest {
  /** 总账科目 ID (一级科目) */
  generalSubjectId: number
  /** 明细科目 ID (子科目, 可为 0 表示无明细) */
  detailSubjectId: number
  /** 借方金额输入文本(保留原始输入以判断是否有小数点) */
  debitInput: string
  /** 贷方金额输入文本 */
  creditInput: string
}

type VoucherForm = Omit<VoucherCreateRequest, 'details'> & {
  details: VoucherDetailRow[]
  attachmentCount?: number
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
const detailTouched = ref(false)
const formDirty = ref(false)

const templateDialogVisible = ref(false)
const templateList = ref<VoucherTemplateVO[]>([])
const templateLoading = ref(false)
const selectedTemplateId = ref<number>(0)

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

/** 总账科目:科目树顶层(一级科目) */
const generalSubjects = computed(() => subjectTree.value)

const form = reactive<VoucherForm>({
  accountSetId: appStore.currentAccountSetId || 0,
  voucherWordId: undefined,
  voucherDate: new Date().toISOString().slice(0, 10),
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  attachmentCount: 0,
  details: [createEmptyDetail()]
})

const formRules: FormRules = {
  voucherDate: [{ required: true, message: '请选择凭证日期', trigger: 'change' }],
  voucherWordId: [{ required: true, message: '请选择凭证字', trigger: 'change' }]
}

const totalDebit = computed(() => form.details.reduce((sum, d) => sum + (d.debit || 0), 0))
const totalCredit = computed(() => form.details.reduce((sum, d) => sum + (d.credit || 0), 0))
const isBalanced = computed(() => Math.abs(totalDebit.value - totalCredit.value) < 0.01 && totalDebit.value > 0)

// ==================== 金额位格(使用 @/utils/amountDigits) ====================

/** 获取行借方金额位格 */
function getDebitDigits(row: VoucherDetailRow): string[] {
  return parseAmountToDigits(row.debitInput)
}

/** 获取行贷方金额位格 */
function getCreditDigits(row: VoucherDetailRow): string[] {
  return parseAmountToDigits(row.creditInput)
}

/** 合计行借方位格 */
const totalDebitDigits = computed(() => numberToDigits(totalDebit.value))
/** 合计行贷方位格 */
const totalCreditDigits = computed(() => numberToDigits(totalCredit.value))

function createEmptyDetail(): VoucherDetailRow {
  return {
    lineNo: 0,
    summary: '',
    subjectId: 0,
    generalSubjectId: 0,
    detailSubjectId: 0,
    debit: 0,
    credit: 0,
    debitInput: '',
    creditInput: ''
  }
}

function formatAmount(val: number): string {
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getSubjectName(id: number): string {
  if (!id) return ''
  const subject = flatSubjects.value.find(s => s.id === id)
  return subject ? `${subject.subjectCode} ${subject.subjectName}` : ''
}

function getVoucherWordName(id?: number): string {
  if (!id) return ''
  const word = voucherWordList.value.find(w => w.id === id)
  return word ? word.name : ''
}

/**
 * 获取总账科目下的所有后代科目(用于明细科目下拉)
 */
function getDetailSubjectOptions(generalSubjectId: number): SubjectVO[] {
  if (!generalSubjectId) return []
  const subject = flatSubjects.value.find(s => s.id === generalSubjectId)
  if (!subject) return []
  const result: SubjectVO[] = []
  function collect(items?: SubjectVO[]) {
    if (!items) return
    for (const item of items) {
      result.push(item)
      if (item.children) collect(item.children)
    }
  }
  collect(subject.children)
  return result
}

// ==================== 科目选择 ====================

function handleGeneralSubjectChange(row: VoucherDetailRow) {
  // 切换总账科目时清空明细科目
  row.detailSubjectId = 0
  row.subjectId = row.generalSubjectId || 0
  formDirty.value = true
}

function handleDetailSubjectChange(row: VoucherDetailRow) {
  row.subjectId = row.detailSubjectId || row.generalSubjectId || 0
  formDirty.value = true
}

/**
 * 根据已加载的 subjectId 反查总账/明细科目
 */
function setSubjectFromId(row: VoucherDetailRow, subjectId: number) {
  const subject = flatSubjects.value.find(s => s.id === subjectId)
  if (!subject) {
    row.generalSubjectId = 0
    row.detailSubjectId = 0
    return
  }
  // 一级科目:直接作为总账科目
  if (subject.level === 1 || subject.parentId === 0) {
    row.generalSubjectId = subject.id
    row.detailSubjectId = 0
    return
  }
  // 子科目:向上查找一级祖先
  let current = subject
  while (current.parentId && current.parentId !== 0) {
    const parent = flatSubjects.value.find(s => s.id === current.parentId)
    if (!parent) break
    current = parent
  }
  row.generalSubjectId = current.id
  row.detailSubjectId = subject.id
}

// ==================== 金额输入 ====================

function handleDebitInput(row: VoucherDetailRow) {
  row.debitInput = sanitizeAmountInput(row.debitInput || '')
  const num = parseFloat(row.debitInput) || 0
  row.debit = num
  if (num > 0) {
    row.credit = 0
    row.creditInput = ''
  }
  formDirty.value = true
}

function handleCreditInput(row: VoucherDetailRow) {
  row.creditInput = sanitizeAmountInput(row.creditInput || '')
  const num = parseFloat(row.creditInput) || 0
  row.credit = num
  if (num > 0) {
    row.debit = 0
    row.debitInput = ''
  }
  formDirty.value = true
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

// ==================== 打印 / 导出 ====================

function handlePrint() {
  // CSS @media print 自动隐藏 .no-print / .screen-only 元素,显示 .print-only 元素
  window.print()
}

function handleExport() {
  const validDetails = form.details.filter(d => d.subjectId && (d.debit > 0 || d.credit > 0))
  if (validDetails.length === 0) {
    ElMessage.warning('暂无可导出的分录数据')
    return
  }

  const exportData: Array<Record<string, string | number>> = validDetails.map((d, i) => ({
    '行号': i + 1,
    '摘要': d.summary || '',
    '总账科目': getSubjectName(d.generalSubjectId),
    '明细科目': d.detailSubjectId ? getSubjectName(d.detailSubjectId) : '',
    '借方金额': d.debit || 0,
    '贷方金额': d.credit || 0
  }))

  exportData.push({
    '行号': '',
    '摘要': '合计',
    '总账科目': '',
    '明细科目': '',
    '借方金额': totalDebit.value,
    '贷方金额': totalCredit.value
  })

  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '凭证')
  const fileName = `凭证_${form.voucherDate || 'export'}.xlsx`
  XLSX.writeFile(wb, fileName)
}

// ==================== 凭证模板 ====================

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

function handleTemplateCurrentChange(row: VoucherTemplateVO | null) {
  selectedTemplateId.value = row ? row.id : 0
}

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

    const unresolvedCodes: string[] = []
    const newDetails: VoucherDetailRow[] = template.details.map((d, idx) => {
      const subject = flatSubjects.value.find(s => s.subjectCode === d.subjectCode)
      if (!subject) {
        unresolvedCodes.push(d.subjectCode)
      }
      const row = createEmptyDetail()
      row.lineNo = idx + 1
      row.summary = d.summary || template.summary || ''
      row.debit = d.debitAmount || 0
      row.credit = d.creditAmount || 0
      row.debitInput = numberToInput(d.debitAmount || 0)
      row.creditInput = numberToInput(d.creditAmount || 0)
      if (subject) {
        setSubjectFromId(row, subject.id)
        row.subjectId = subject.id
      }
      return row
    })

    form.details = newDetails
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

async function loadTemplateFromQuery(templateId: number) {
  try {
    const res = await templateApi.apply(templateId)
    const template = res.data
    if (!template.details || template.details.length === 0) {
      ElMessage.warning('模板没有分录明细,无法应用')
      return
    }
    const unresolvedCodes: string[] = []
    const newDetails: VoucherDetailRow[] = template.details.map((d, idx) => {
      const subject = flatSubjects.value.find(s => s.subjectCode === d.subjectCode)
      if (!subject) {
        unresolvedCodes.push(d.subjectCode)
      }
      const row = createEmptyDetail()
      row.lineNo = idx + 1
      row.summary = d.summary || template.summary || ''
      row.debit = d.debitAmount || 0
      row.credit = d.creditAmount || 0
      row.debitInput = numberToInput(d.debitAmount || 0)
      row.creditInput = numberToInput(d.creditAmount || 0)
      if (subject) {
        setSubjectFromId(row, subject.id)
        row.subjectId = subject.id
      }
      return row
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

function handleAbstractSelect(_row: VoucherDetailRow, item: AbstractSuggestionItem) {
  if (item.id) {
    abstractIdMap.set(item.abstractText, item.id)
  }
}

function handleBack() {
  router.push('/voucher')
}

function hasAnyDetailFilled(): boolean {
  return form.details.some(d => d.summary || d.subjectId || (d.debit && d.debit > 0) || (d.credit && d.credit > 0))
}

async function handleSubmit() {
  detailTouched.value = true
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
    if (!isEdit.value) {
      form.accountSetId = appStore.currentAccountSetId || 0
    }

    submitLoading.value = true
    try {
      // 提交时剥离前端扩展字段,仅保留后端需要的 VoucherDetailRequest
      const submitData = {
        ...form,
        details: validDetails.map((d, i) => ({
          lineNo: i + 1,
          summary: d.summary,
          subjectId: d.subjectId,
          debit: d.debit,
          credit: d.credit
        }))
      }
      if (isEdit.value) {
        await voucherApi.update(editId.value, submitData)
        ElMessage.success('更新成功')
      } else {
        await voucherApi.create(submitData)
        ElMessage.success('创建成功')
      }

      const usedAbstractIds = new Set<number>()
      for (const detail of validDetails) {
        const abstractId = abstractIdMap.get(detail.summary)
        if (abstractId) {
          usedAbstractIds.add(abstractId)
        }
      }
      usedAbstractIds.forEach(id => {
        abstractApi.incrementUse(id).catch(() => {
          // 使用次数累计失败不影响主流程
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
    form.details = voucher.details.map(d => {
      const row = createEmptyDetail()
      row.lineNo = d.lineNo
      row.summary = d.summary
      row.subjectId = d.subjectId
      row.debit = d.debit
      row.credit = d.credit
      row.debitInput = numberToInput(d.debit)
      row.creditInput = numberToInput(d.credit)
      // 根据已加载的 subjectId 反查总账/明细科目
      setSubjectFromId(row, d.subjectId)
      return row
    })
    formDirty.value = false
  } catch {
    // handled by interceptor
  }
}

// 凭证录入快捷键
useShortcut([
  { ctrl: true, key: 's', handler: () => handleSubmit() },
  { ctrl: true, key: 'enter', handler: () => handleAddRow() },
  { alt: true, key: 't', handler: () => handleOpenTemplateDialog() },
  { key: 'escape', handler: () => handleBack() }
])

// 离开页面提示
onBeforeRouteLeave(async (_to, _from, next) => {
  if (!formDirty.value) {
    next()
    return
  }
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
  // 编辑模式:先加载凭证详情获取正确的 accountSetId,再加载科目树和凭证字
  // (避免编辑其他账套的凭证时科目树从当前账套加载,导致 setSubjectFromId 找不到科目)
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
      // 科目树加载完成后,对编辑模式下的分录重新解析总账/明细科目
      if (isEdit.value) {
        for (const row of form.details) {
          if (row.subjectId) {
            setSubjectFromId(row, row.subjectId)
          }
        }
      }
    } catch {
      // handled by interceptor
    }
  }

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

// ==================== 凭证表格 ====================

.voucher-table-wrapper {
  overflow-x: auto;
  margin-bottom: 8px;
}

.voucher-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  background: #fff;

  th,
  td {
    border: 1px solid #dcdfe6;
    padding: 4px 6px;
    text-align: center;
    vertical-align: middle;
  }

  thead th {
    background: #f5f7fa;
    font-weight: 600;
    color: #303133;
    white-space: nowrap;
  }

  .col-summary {
    min-width: 140px;
    width: 140px;
  }

  .col-subject {
    min-width: 160px;
    width: 160px;
  }

  .col-action {
    width: 60px;
  }

  // 金额列组表头
  .amount-header {
    background: #ecf5ff;
    color: #409eff;

    &.credit-header {
      background: #f0f9eb;
      color: #67c23a;
    }
  }

  // 位格表头(亿千百十万千百十元角分)
  .digit-th {
    width: 28px;
    min-width: 28px;
    max-width: 28px;
    padding: 4px 2px;
    font-size: 12px;
    font-weight: 600;
  }

  // 位格单元格
  .digit-cell {
    width: 28px;
    min-width: 28px;
    max-width: 28px;
    padding: 6px 2px;
    text-align: center;
    font-family: 'Courier New', monospace;
    font-size: 14px;
    font-weight: 500;
  }

  .summary-cell {
    text-align: left;
    padding: 4px;
  }

  .subject-cell {
    padding: 4px;
  }

  .action-cell {
    text-align: center;
  }
}

// 金额输入行
.amount-input-row {
  background: #fafafa;

  .input-row-label {
    text-align: right;
    padding-right: 12px;
    font-size: 12px;
    color: #909399;
    white-space: nowrap;

    .el-icon {
      vertical-align: middle;
      margin-right: 4px;
    }
  }

  .amount-input-cell {
    padding: 4px 6px;
  }
}

// 合计行
.total-row {
  background: #f5f7fa;
  font-weight: 600;

  .total-label {
    text-align: center;
    font-size: 14px;
    letter-spacing: 4px;
  }

  .total-digit {
    background: #fdf6ec;
    color: #e6a23c;
  }
}

.add-row-btn {
  text-align: center;
  padding: 8px 0;
}

.inline-error {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
}

:deep(.field-error) {
  .el-input__wrapper,
  .el-select__wrapper {
    box-shadow: 0 0 0 1px #f56c6c inset !important;
  }
}

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

// ==================== 打印/屏幕切换 ====================

.print-only {
  display: none;
}

// ==================== 打印样式 ====================

@media print {
  .no-print {
    display: none !important;
  }

  .screen-only {
    display: none !important;
  }

  .print-only {
    display: inline !important;
  }

  .voucher-create-container {
    padding: 0;
  }

  :deep(.el-card) {
    border: none;
    box-shadow: none;
  }

  :deep(.el-card__header) {
    border-bottom: 1px solid #333;
  }

  .voucher-header {
    margin-bottom: 8px;
  }

  .voucher-table-wrapper {
    overflow: visible;
  }

  .voucher-table {
    width: 100%;
    font-size: 11px;

    th,
    td {
      border: 1px solid #333 !important;
      padding: 3px 2px;
    }

    thead th {
      background: #f0f0f0 !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    .digit-th {
      width: 24px;
      min-width: 24px;
      max-width: 24px;
    }

    .digit-cell {
      width: 24px;
      min-width: 24px;
      max-width: 24px;
      font-size: 12px;
    }

    .total-row .total-digit {
      background: #f9f9f9 !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
  }
}
</style>
