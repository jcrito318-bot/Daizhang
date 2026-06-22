<template>
  <div class="system-setting-container">
    <el-card class="setting-card">
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="公司信息" name="company">
          <el-form ref="companyFormRef" :model="companyForm" label-width="120px" style="max-width: 600px">
            <el-form-item label="公司名称">
              <el-input v-model="companyForm.companyName" placeholder="请输入公司名称" />
            </el-form-item>
            <el-form-item label="公司地址">
              <el-input v-model="companyForm.address" placeholder="请输入公司地址" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="companyForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="公司邮箱">
              <el-input v-model="companyForm.email" placeholder="请输入公司邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveCompany">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="系统参数" name="system">
          <el-form ref="systemFormRef" :model="systemForm" label-width="120px" style="max-width: 600px">
            <el-form-item label="会计年度起始月">
              <el-select v-model="systemForm.fiscalYearStartMonth" placeholder="请选择" style="width: 100%">
                <el-option label="1月" :value="1" />
                <el-option label="2月" :value="2" />
                <el-option label="3月" :value="3" />
                <el-option label="4月" :value="4" />
                <el-option label="5月" :value="5" />
                <el-option label="6月" :value="6" />
                <el-option label="7月" :value="7" />
                <el-option label="8月" :value="8" />
                <el-option label="9月" :value="9" />
                <el-option label="10月" :value="10" />
                <el-option label="11月" :value="11" />
                <el-option label="12月" :value="12" />
              </el-select>
            </el-form-item>
            <el-form-item label="默认币种">
              <el-select v-model="systemForm.defaultCurrency" placeholder="请选择" style="width: 100%">
                <el-option label="人民币 (CNY)" value="CNY" />
                <el-option label="美元 (USD)" value="USD" />
                <el-option label="欧元 (EUR)" value="EUR" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveSystem">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('company')

const companyForm = reactive({
  companyName: '',
  address: '',
  phone: '',
  email: ''
})

const systemForm = reactive({
  fiscalYearStartMonth: 1,
  defaultCurrency: 'CNY'
})

function handleSaveCompany() {
  ElMessage.success('公司信息保存成功')
}

function handleSaveSystem() {
  ElMessage.success('系统参数保存成功')
}
</script>

<style scoped lang="scss">
.system-setting-container {
  padding: 20px;
}

.setting-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
