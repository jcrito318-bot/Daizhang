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
          <el-form ref="companyFormRef" :model="companyForm" :rules="companyRules" label-width="120px" style="max-width: 600px" v-loading="loading">
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="companyForm.companyName" placeholder="请输入公司名称" />
            </el-form-item>
            <el-form-item label="公司地址" prop="address">
              <el-input v-model="companyForm.address" placeholder="请输入公司地址" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="companyForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="公司邮箱" prop="email">
              <el-input v-model="companyForm.email" placeholder="请输入公司邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="companySaving" @click="handleSaveCompany">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="系统参数" name="system">
          <el-form ref="systemFormRef" :model="systemForm" :rules="systemRules" label-width="120px" style="max-width: 600px" v-loading="loading">
            <el-form-item label="会计年度起始月" prop="fiscalYearStartMonth">
              <el-select v-model="systemForm.fiscalYearStartMonth" placeholder="请选择" style="width: 100%">
                <el-option v-for="i in 12" :key="i" :label="`${i}月`" :value="i" />
              </el-select>
            </el-form-item>
            <el-form-item label="默认币种" prop="defaultCurrency">
              <el-select v-model="systemForm.defaultCurrency" placeholder="请选择" style="width: 100%">
                <el-option label="人民币 (CNY)" value="CNY" />
                <el-option label="美元 (USD)" value="USD" />
                <el-option label="欧元 (EUR)" value="EUR" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="systemSaving" @click="handleSaveSystem">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { settingApi } from '@/api/system'

// 配置项 key 常量:对应后端 sys_config 表中的 config_key 字段
const CONFIG_KEYS = {
  companyName: 'company.name',
  address: 'company.address',
  phone: 'company.phone',
  email: 'company.email',
  fiscalYearStartMonth: 'system.fiscal_year_start_month',
  defaultCurrency: 'system.default_currency'
} as const

// 各 config_key 对应的中文 config_name(后端表必填字段)
const CONFIG_NAMES: Record<string, string> = {
  [CONFIG_KEYS.companyName]: '公司名称',
  [CONFIG_KEYS.address]: '公司地址',
  [CONFIG_KEYS.phone]: '联系电话',
  [CONFIG_KEYS.email]: '公司邮箱',
  [CONFIG_KEYS.fiscalYearStartMonth]: '会计年度起始月',
  [CONFIG_KEYS.defaultCurrency]: '默认币种'
}

const activeTab = ref('company')
const loading = ref(false)
const companySaving = ref(false)
const systemSaving = ref(false)

const companyFormRef = ref<FormInstance>()
const systemFormRef = ref<FormInstance>()

// 已加载的配置项 id 映射(后端 update 需 id),key 为 config_key
const configIdMap = ref<Record<string, number>>({})

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

const companyRules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const systemRules: FormRules = {
  fiscalYearStartMonth: [{ required: true, message: '请选择会计年度起始月', trigger: 'change' }],
  defaultCurrency: [{ required: true, message: '请选择默认币种', trigger: 'change' }]
}

// 单个配置项的 key -> 表单字段映射,用于回显
function applyConfigValue(key: string, value: string) {
  switch (key) {
    case CONFIG_KEYS.companyName: companyForm.companyName = value; break
    case CONFIG_KEYS.address: companyForm.address = value; break
    case CONFIG_KEYS.phone: companyForm.phone = value; break
    case CONFIG_KEYS.email: companyForm.email = value; break
    case CONFIG_KEYS.fiscalYearStartMonth: {
      const n = Number(value)
      if (Number.isFinite(n)) systemForm.fiscalYearStartMonth = n
      break
    }
    case CONFIG_KEYS.defaultCurrency: systemForm.defaultCurrency = value || 'CNY'; break
  }
}

// 加载所有配置:遍历所有 key 逐个 getValue。后端无批量接口,这里并发拉取。
async function loadAllConfigs() {
  loading.value = true
  try {
    const keys = Object.values(CONFIG_KEYS)
    // 同时拉取 page 接口获取 id(用于后续 update)
    const [pageRes, ...valueResults] = await Promise.all([
      settingApi.page({ pageNum: 1, pageSize: 100 }),
      ...keys.map(k => settingApi.getValue(k).catch(() => null))
    ])
    // 建立 key -> id 映射
    for (const item of pageRes.data.list) {
      configIdMap.value[item.configKey] = item.id
    }
    // 应用值
    keys.forEach((key, idx) => {
      const res = valueResults[idx]
      if (res && typeof res.data === 'string') {
        applyConfigValue(key, res.data)
      }
    })
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

// 保存单个配置项:已存在则 update,不存在则 create
async function saveConfig(key: string, value: string) {
  const configName = CONFIG_NAMES[key] || key
  const id = configIdMap.value[key]
  if (id) {
    await settingApi.update(id, { configKey: key, configName, configValue: value })
  } else {
    const res = await settingApi.create({ configKey: key, configName, configValue: value })
    // create 后重新拉一次 page 以获取新 id(后端 create 接口未返回 id)
    if (res.code === 200) {
      const pageRes = await settingApi.page({ pageNum: 1, pageSize: 100 })
      for (const item of pageRes.data.list) {
        configIdMap.value[item.configKey] = item.id
      }
    }
  }
}

async function handleSaveCompany() {
  const valid = await companyFormRef.value?.validate().catch(() => false)
  if (!valid) return
  companySaving.value = true
  try {
    await saveConfig(CONFIG_KEYS.companyName, companyForm.companyName)
    await saveConfig(CONFIG_KEYS.address, companyForm.address)
    await saveConfig(CONFIG_KEYS.phone, companyForm.phone)
    await saveConfig(CONFIG_KEYS.email, companyForm.email)
    ElMessage.success('公司信息保存成功')
  } catch {
    // 拦截器已提示
  } finally {
    companySaving.value = false
  }
}

async function handleSaveSystem() {
  const valid = await systemFormRef.value?.validate().catch(() => false)
  if (!valid) return
  systemSaving.value = true
  try {
    await saveConfig(CONFIG_KEYS.fiscalYearStartMonth, String(systemForm.fiscalYearStartMonth))
    await saveConfig(CONFIG_KEYS.defaultCurrency, systemForm.defaultCurrency)
    ElMessage.success('系统参数保存成功')
  } catch {
    // 拦截器已提示
  } finally {
    systemSaving.value = false
  }
}

onMounted(() => {
  loadAllConfigs()
})
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
