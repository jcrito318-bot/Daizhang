<template>
  <div class="bank-transaction-list-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="银行账户">
          <el-input v-model="queryForm.bankAccount" placeholder="请输入银行账户" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="交易日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select v-model="queryForm.transactionType" placeholder="请选择类型" clearable style="width: 120px">
            <el-option label="收入" :value="1" />
            <el-option label="支出" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配状态">
          <el-select v-model="queryForm.matchedStatus" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="未匹配" :value="0" />
            <el-option label="已匹配" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="对方单位">
          <el-input v-model="queryForm.counterparty" placeholder="请输入对方单位" clearable style="width: 150px" />
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
          <span>银行流水列表</span>
          <div>
            <el-button type="success" @click="handleDownloadTemplate">
              <el-icon><Download /></el-icon>下载模板
            </el-button>
            <el-button type="warning" @click="showImportDialog">
              <el-icon><Upload /></el-icon>导入流水
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column prop="transactionNo" label="交易流水号" width="160" show-overflow-tooltip />
        <el-table-column label="交易类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.transactionType === 1 ? 'success' : 'danger'">
              {{ row.transactionTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="交易金额" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.transactionType === 1 ? '#67c23a' : '#f56c6c' }">
              {{ formatAmount(row.amount, row.transactionType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="130" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.balance, 1) }}
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="对方单位" min-width="180" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column label="匹配状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matchedStatus === 0 ? 'warning' : 'success'">
              {{ row.matchedStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="voucherNo" label="关联凭证号" width="140" />
        <el-table-column prop="createByName" label="创建人" width="100" />
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
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

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入银行流水" width="600px">
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="银行账户" required>
          <el-input v-model="importForm.bankAccount" placeholder="请输入银行账户" />
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary">选择Excel文件</el-button>
            <template #tip>
              <div class="el-upload__tip">只能上传 .xlsx / .xls 文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance, UploadFile } from 'element-plus'
import * as XLSX from 'xlsx'
import { bankApi } from '@/api/bank'
import { useAppStore } from '@/stores/app'
import type { BankTransactionVO, BankTransactionQueryRequest, BankTransactionImportRequest, BankTransactionItem } from '@/types/bank'

const appStore = useAppStore()

const loading = ref(false)
const total = ref(0)
const tableData = ref<BankTransactionVO[]>([])
const dateRange = ref<[string, string] | null>(null)

const queryForm = reactive<BankTransactionQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  bankAccount: '',
  transactionType: undefined,
  matchedStatus: undefined,
  startDate: undefined,
  endDate: undefined,
  counterparty: '',
  pageNum: 1,
  pageSize: 10
})

// 导入相关
const importDialogVisible = ref(false)
const importLoading = ref(false)
const uploadRef = ref<UploadInstance>()
const importFile = ref<File | null>(null)
const importForm = reactive({
  bankAccount: ''
})

function formatAmount(val: number, type: number): string {
  if (val === undefined || val === null) return ''
  const prefix = type === 1 ? '+' : '-'
  return prefix + val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  queryForm.accountSetId = appStore.currentAccountSetId || 0
  if (!queryForm.accountSetId) {
    ElMessage.warning('请先选择账套')
    return
  }
  
  // 处理日期范围
  if (dateRange.value && dateRange.value.length === 2) {
    queryForm.startDate = dateRange.value[0]
    queryForm.endDate = dateRange.value[1]
  } else {
    queryForm.startDate = undefined
    queryForm.endDate = undefined
  }
  
  loading.value = true
  try {
    const res = await bankApi.getTransactionPage(queryForm)
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
  queryForm.bankAccount = ''
  queryForm.transactionType = undefined
  queryForm.matchedStatus = undefined
  queryForm.counterparty = ''
  dateRange.value = null
  queryForm.pageNum = 1
  loadData()
}

async function handleDelete(row: BankTransactionVO) {
  await ElMessageBox.confirm(`确定要删除该银行流水记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await bankApi.deleteTransaction(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // handled by interceptor
  }
}

function showImportDialog() {
  importForm.bankAccount = ''
  importFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  importDialogVisible.value = true
}

function handleFileChange(file: UploadFile) {
  importFile.value = file.raw || null
}

function handleFileRemove() {
  importFile.value = null
}

async function handleImport() {
  if (!importForm.bankAccount) {
    ElMessage.warning('请输入银行账户')
    return
  }
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  importLoading.value = true
  try {
    const data = await readFile(importFile.value)
    const transactions = parseExcelData(data)
    
    if (transactions.length === 0) {
      ElMessage.warning('未解析到有效数据')
      importLoading.value = false
      return
    }
    
    const importData: BankTransactionImportRequest = {
      accountSetId: appStore.currentAccountSetId || 0,
      bankAccount: importForm.bankAccount,
      transactions
    }
    
    const res = await bankApi.importTransactions(importData)
    ElMessage.success(`成功导入 ${res.data} 条记录`)
    importDialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

function readFile(file: File): Promise<any> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const data = e.target?.result
        const workbook = XLSX.read(data, { type: 'array' })
        const sheetName = workbook.SheetNames[0]
        const worksheet = workbook.Sheets[sheetName]
        const json = XLSX.utils.sheet_to_json(worksheet)
        resolve(json)
      } catch (err) {
        reject(new Error('文件解析失败'))
      }
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsArrayBuffer(file)
  })
}

function parseExcelData(data: any[]): BankTransactionItem[] {
  const transactions: BankTransactionItem[] = []
  // BF-15 修复:原 transactionType: row['交易类型'] === '收入' ? 1 : 2,
  // Excel 中任何非"收入"值(空、错别字"收入 "、英文"income")都会被判定为支出,导入后数据错乱。
  // 改为显式映射表,未匹配的行收集到错误列表,不静默赋值。
  const typeMap: Record<string, number> = {
    '收入': 1,
    '支出': 2,
    '转入': 1,
    '转出': 2
  }
  const skippedRows: number[] = []

  data.forEach((row, idx) => {
    const rawType = String(row['交易类型'] ?? '').trim()
    const mapped = typeMap[rawType]
    if (mapped === undefined) {
      // 交易类型未识别:收集行号(Excel 行号从 1 开始,跳过表头 +1)
      skippedRows.push(idx + 2)
      return
    }
    const item: BankTransactionItem = {
      transactionDate: row['交易日期'] || row['日期'] || '',
      transactionType: mapped,
      amount: parseFloat(row['交易金额'] || row['金额'] || '0'),
      balance: row['余额'] ? parseFloat(row['余额']) : undefined,
      counterparty: row['对方单位'] || row['对方'] || '',
      summary: row['摘要'] || '',
      transactionNo: row['交易流水号'] || row['流水号'] || '',
      remark: row['备注'] || ''
    }

    if (item.transactionDate && item.amount) {
      transactions.push(item)
    }
  })

  if (skippedRows.length > 0) {
    ElMessage.warning(`第 ${skippedRows.join(', ')} 行交易类型未识别(应为"收入"或"支出"),已跳过`)
  }

  return transactions
}

function handleDownloadTemplate() {
  const template = [
    {
      '交易日期': '2024-01-01',
      '交易类型': '收入',
      '交易金额': 10000.00,
      '余额': 10000.00,
      '对方单位': '示例公司',
      '摘要': '示例摘要',
      '交易流水号': '123456789',
      '备注': '示例备注'
    }
  ]
  
  const worksheet = XLSX.utils.json_to_sheet(template)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '银行流水')
  
  // 设置列宽
  worksheet['!cols'] = [
    { wch: 12 }, // 交易日期
    { wch: 10 }, // 交易类型
    { wch: 12 }, // 交易金额
    { wch: 12 }, // 余额
    { wch: 20 }, // 对方单位
    { wch: 30 }, // 摘要
    { wch: 20 }, // 交易流水号
    { wch: 30 }  // 备注
  ]
  
  XLSX.writeFile(workbook, '银行流水导入模板.xlsx')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.bank-transaction-list-container {
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
