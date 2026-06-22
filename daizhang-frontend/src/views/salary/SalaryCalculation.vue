<template>
  <div class="salary-calculation-container">
    <el-card class="calculation-card">
      <template #header>
        <div class="card-header">
          <span>工资计算</span>
        </div>
      </template>

      <el-form :model="form" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择年度" required>
              <el-date-picker v-model="form.year" type="year" placeholder="请选择年度" value-format="YYYY" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="选择月份" required>
              <el-select v-model="form.month" placeholder="请选择月份">
                <el-option v-for="i in 12" :key="i" :label="`${i}月`" :value="i" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleCalculate" :loading="calculating">开始计算</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { salaryApi } from '@/api/salary'

const calculating = ref(false)

const form = reactive({
  year: '' as string | null,
  month: null as number | null
})

const handleCalculate = async () => {
  if (!form.year || !form.month) {
    ElMessage.warning('请选择年度和月份')
    return
  }

  calculating.value = true
  try {
    await salaryApi.calculateSalary({
      accountSetId: 1,
      year: Number(form.year),
      month: form.month
    })
    ElMessage.success('工资计算完成')
  } catch (error) {
    console.error(error)
    ElMessage.error('计算失败')
  } finally {
    calculating.value = false
  }
}

const handleReset = () => {
  form.year = ''
  form.month = null
}
</script>

<style scoped lang="scss">
.salary-calculation-container {
  padding: 20px;
}

.calculation-card {
  margin-bottom: 20px;
}
</style>
