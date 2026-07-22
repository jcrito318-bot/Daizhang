<template>
  <div class="customer-detail-container">
    <el-page-header @back="handleBack" title="返回客户列表" />

    <el-card v-loading="loading" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>客户详情</span>
          <el-button type="primary" @click="handleEdit">编辑</el-button>
        </div>
      </template>

      <el-descriptions :column="3" border v-if="customer">
        <el-descriptions-item label="客户编号">{{ customer.customerCode }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ customer.customerName }}</el-descriptions-item>
        <el-descriptions-item label="客户类型">{{ customer.customerType }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ customer.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ customer.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ customer.email }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="3">{{ customer.address }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="customer.status === 1 ? 'success' : 'danger'">
            {{ customer.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ customer.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ customer.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="contract-card">
      <template #header>
        <div class="card-header">
          <span>关联合同</span>
        </div>
      </template>

      <el-table :data="contracts" border>
        <el-table-column prop="contractNo" label="合同编号" width="150" />
        <el-table-column prop="contractName" label="合同名称" width="200" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="amount" label="合同金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '执行中' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customerApi, contractApi } from '@/api/customer'
import type { CustomerVO, ContractVO } from '@/types/customer'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const customer = ref<CustomerVO | null>(null)
const contracts = ref<ContractVO[]>([])

const formatAmount = (amount: number) => {
  return amount ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const loadDetail = async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await customerApi.getById(id)
    customer.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadContracts = async () => {
  const id = Number(route.params.id)
  if (!id) return

  try {
    const res = await contractApi.getByCustomerId(id)
    contracts.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const handleBack = () => {
  router.push('/customer/list')
}

const handleEdit = () => {
  // 编辑弹窗在列表页内联实现,跳回列表页并通过 query 触发编辑
  router.push(`/customer/list?edit=${route.params.id}`)
}

onMounted(() => {
  loadDetail()
  loadContracts()
})
</script>

<style scoped lang="scss">
.customer-detail-container {
  padding: 20px;
}

.detail-card {
  margin-top: 20px;
  margin-bottom: 20px;
}

.contract-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
