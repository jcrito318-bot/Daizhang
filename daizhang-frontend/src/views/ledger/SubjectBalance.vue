<template>
  <div class="subject-balance-container">
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="账套">
          <el-select
            v-model="queryForm.accountSetId"
            placeholder="请选择账套"
            style="width: 200px"
          >
            <el-option
              v-for="item in accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-input-number v-model="queryForm.year" :min="2000" :max="2100" style="width: 120px" />
        </el-form-item>
        <el-form-item label="起始月份">
          <el-input-number v-model="queryForm.startMonth" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item label="截止月份">
          <el-input-number v-model="queryForm.endMonth" :min="1" :max="12" style="width: 100px" />
        </el-form-item>
        <el-form-item label="科目级次">
          <el-input-number v-model="queryForm.level" :min="1" :max="5" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <span>科目余额表</span>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe row-key="subjectCode">
        <el-table-column prop="subjectCode" label="科目编码" width="120" />
        <el-table-column prop="subjectName" label="科目名称" width="160" />
        <el-table-column label="期初余额">
          <el-table-column prop="beginDebit" label="借方" width="140" align="right">
            <template #default="{ row }">
              {{ row.beginDebit ? row.beginDebit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="beginCredit" label="贷方" width="140" align="right">
            <template #default="{ row }">
              {{ row.beginCredit ? row.beginCredit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="本期发生额">
          <el-table-column prop="periodDebit" label="借方" width="140" align="right">
            <template #default="{ row }">
              {{ row.periodDebit ? row.periodDebit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="periodCredit" label="贷方" width="140" align="right">
            <template #default="{ row }">
              {{ row.periodCredit ? row.periodCredit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="期末余额">
          <el-table-column prop="endDebit" label="借方" width="140" align="right">
            <template #default="{ row }">
              {{ row.endDebit ? row.endDebit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
          <el-table-column prop="endCredit" label="贷方" width="140" align="right">
            <template #default="{ row }">
              {{ row.endCredit ? row.endCredit.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '' }}
            </template>
          </el-table-column>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ledgerApi } from '@/api/ledger'
import { accountSetApi } from '@/api/accountset'
import { useAppStore } from '@/stores/app'
import type { SubjectBalanceVO, SubjectBalanceQueryRequest } from '@/types/ledger'
import type { AccountSetVO } from '@/types/accountset'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<SubjectBalanceVO[]>([])
const accountSetList = ref<AccountSetVO[]>([])

const queryForm = reactive<SubjectBalanceQueryRequest>({
  accountSetId: appStore.currentAccountSetId || 0,
  year: new Date().getFullYear(),
  startMonth: 1,
  endMonth: 12,
  level: 1
})

async function loadData() {
  if (!queryForm.accountSetId) return
  loading.value = true
  try {
    const res = await ledgerApi.getSubjectBalance(queryForm)
    tableData.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

onMounted(async () => {
  try {
    const res = await accountSetApi.getList()
    accountSetList.value = res.data
    if (accountSetList.value.length > 0 && !queryForm.accountSetId) {
      queryForm.accountSetId = accountSetList.value[0].id
    }
  } catch {
    // handled by interceptor
  }
})
</script>

<style scoped lang="scss">
.subject-balance-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}
</style>
