<template>
  <div class="detail-ledger-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 200px"
            @change="handleAccountSetChange"
          >
            <el-option
              v-for="item in accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-select
            v-model="queryForm.subjectId"
            placeholder="请选择科目"
            filterable
            style="width: 240px"
          >
            <el-option
              v-for="subject in flatSubjects"
              :key="subject.id"
              :label="`${subject.subjectCode} ${subject.subjectName}`"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="起始月份">
          <el-input-number v-model="startMonth" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item label="截止月份">
          <el-input-number v-model="endMonth" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <span>明细账</span>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe show-summary>
        <el-table-column prop="voucherDate" label="日期" width="110" />
        <el-table-column prop="voucherNo" label="凭证号" width="130" />
        <el-table-column prop="summary" label="摘要" min-width="200" />
        <el-table-column prop="subjectCode" label="科目编码" width="110" />
        <el-table-column prop="subjectName" label="科目名称" width="130" />
        <el-table-column prop="debit" label="借方金额" width="130" align="right">
          <template #default="{ row }">
            {{ row.debit ? row.debit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="贷方金额" width="130" align="right">
          <template #default="{ row }">
            {{ row.credit ? row.credit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="60" align="center" />
        <el-table-column prop="balance" label="余额" width="130" align="right">
          <template #default="{ row }">
            {{ row.balance ? row.balance.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ledgerApi } from '@/api/ledger'
import { accountSetApi } from '@/api/accountset'
import { subjectApi } from '@/api/subject'
import { useAppStore } from '@/stores/app'
import type { DetailLedgerVO, LedgerQueryRequest } from '@/types/ledger'
import type { AccountSetVO } from '@/types/accountset'
import type { SubjectVO } from '@/types/subject'

const appStore = useAppStore()
const loading = ref(false)
const total = ref(0)
const tableData = ref<DetailLedgerVO[]>([])
const accountSetList = ref<AccountSetVO[]>([])
const subjectTree = ref<SubjectVO[]>([])
const startMonth = ref(1)
const endMonth = ref(12)

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

const queryForm = reactive<LedgerQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  subjectId: undefined,
  year: new Date().getFullYear(),
  pageNum: 1,
  pageSize: 20
})

async function handleAccountSetChange(id: number) {
  try {
    const res = await subjectApi.getTree(id)
    subjectTree.value = res.data
    queryForm.subjectId = undefined
  } catch {
    // handled by interceptor
  }
}

async function loadData() {
  if (!queryForm.accountSetId) return
  loading.value = true
  try {
    const params = { ...queryForm, startMonth: startMonth.value, endMonth: endMonth.value }
    const res = await ledgerApi.getDetailLedger(params)
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

onMounted(async () => {
  try {
    const res = await accountSetApi.getList()
    accountSetList.value = res.data
    if (accountSetList.value.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = accountSetList.value[0].id
    }
    if (queryForm.accountSetId) {
      const subjectRes = await subjectApi.getTree(queryForm.accountSetId)
      subjectTree.value = subjectRes.data
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.detail-ledger-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
