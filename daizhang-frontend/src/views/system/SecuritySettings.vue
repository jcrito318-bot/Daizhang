<template>
  <div class="security-settings-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>安全设置</span>
        </div>
      </template>

      <!-- 双因素认证 (2FA) 区块 -->
      <el-divider content-position="left">双因素认证 (2FA TOTP)</el-divider>

      <div v-loading="loading" class="totp-section">
        <!-- 当前状态 -->
        <div class="status-row">
          <span class="status-label">当前状态:</span>
          <el-tag v-if="totpStatus.enabled" type="success">已启用</el-tag>
          <el-tag v-else type="info">未启用</el-tag>
        </div>

        <el-alert
          v-if="totpStatus.enabled"
          type="success"
          :closable="false"
          show-icon
          style="margin: 12px 0"
          title="双因素认证已启用"
          description="登录时除密码外还需输入 Authenticator 应用中的验证码,显著提升账户安全性。"
        />
        <el-alert
          v-else
          type="warning"
          :closable="false"
          show-icon
          style="margin: 12px 0"
          title="双因素认证未启用"
          description="启用后,登录时需输入 Authenticator 应用(如 Google Authenticator / 微信 / 火狐)中的 6 位验证码。"
        />

        <!-- 操作按钮 -->
        <div class="action-row">
          <el-button v-if="!totpStatus.enabled" type="primary" @click="handleSetup">启用 2FA</el-button>
          <el-button v-else type="danger" @click="disableDialogVisible = true">禁用 2FA</el-button>
        </div>
      </div>
    </el-card>

    <!-- 启用 2FA 向导对话框 -->
    <el-dialog v-model="setupDialogVisible" title="启用双因素认证" width="520px" :close-on-click-modal="false" destroy-on-close>
      <!-- 步骤1: 显示二维码 -->
      <div v-if="setupStep === 'qr'" class="setup-qr">
        <p class="setup-tip">1. 用 Authenticator 应用扫描下方二维码,或手动输入密钥</p>
        <div class="qr-wrapper">
          <img v-if="qrDataUrl" :src="qrDataUrl" alt="TOTP QR Code" class="qr-img" />
          <el-skeleton-item v-else variant="image" style="width: 200px; height: 200px" />
        </div>
        <div class="secret-row">
          <span class="secret-label">手动输入密钥:</span>
          <el-input :model-value="setupData?.secret" readonly size="small" style="width: 240px" />
          <el-button size="small" @click="copySecret">复制</el-button>
        </div>
        <p class="setup-tip" style="margin-top: 16px">2. 输入 Authenticator 应用中显示的 6 位验证码完成绑定</p>
        <el-input
          v-model="enableCode"
          placeholder="请输入 6 位验证码"
          size="large"
          maxlength="6"
          style="margin-top: 8px"
          @keyup.enter="handleEnable"
        />
      </div>

      <!-- 步骤2: 显示备用码 -->
      <div v-else-if="setupStep === 'backup'" class="setup-backup">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
          <template #title>2FA 启用成功!请妥善保存以下备用恢复码</template>
          每个备用码可一次性使用(替代验证码登录),无法访问设备时使用。此为唯一一次展示,丢失后无法找回。
        </el-alert>
        <div class="backup-codes">
          <div v-for="code in backupCodes" :key="code" class="backup-code-item">{{ code }}</div>
        </div>
        <div class="backup-actions">
          <el-button size="small" @click="copyBackupCodes">复制全部</el-button>
        </div>
      </div>

      <template #footer>
        <el-button v-if="setupStep === 'qr'" @click="setupDialogVisible = false">取消</el-button>
        <el-button v-if="setupStep === 'qr'" type="primary" :loading="enableLoading" @click="handleEnable">确认启用</el-button>
        <el-button v-if="setupStep === 'backup'" type="primary" @click="finishSetup">完成</el-button>
      </template>
    </el-dialog>

    <!-- 禁用 2FA 对话框 -->
    <el-dialog v-model="disableDialogVisible" title="禁用双因素认证" width="420px" :close-on-click-modal="false" destroy-on-close>
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px">
        禁用后登录将不再需要验证码,安全性降低。请输入当前验证码或备用码确认。
      </el-alert>
      <el-input
        v-model="disableCode"
        placeholder="请输入验证码或备用码"
        size="large"
        maxlength="32"
        @keyup.enter="handleDisable"
      />
      <template #footer>
        <el-button @click="disableDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="disableLoading" @click="handleDisable">确认禁用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { totpApi } from '@/api/auth'
import type { TotpSetupVO, TotpStatusVO } from '@/types/common'

const loading = ref(false)
const totpStatus = reactive<TotpStatusVO>({ enabled: false, secretGenerated: false })

// 启用向导
const setupDialogVisible = ref(false)
const setupStep = ref<'qr' | 'backup'>('qr')
const setupData = ref<TotpSetupVO | null>(null)
const qrDataUrl = ref('')
const enableCode = ref('')
const enableLoading = ref(false)
const backupCodes = ref<string[]>([])

// 禁用
const disableDialogVisible = ref(false)
const disableCode = ref('')
const disableLoading = ref(false)

async function loadStatus() {
  loading.value = true
  try {
    const res = await totpApi.status()
    totpStatus.enabled = res.data.enabled
    totpStatus.secretGenerated = res.data.secretGenerated
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSetup() {
  enableCode.value = ''
  qrDataUrl.value = ''
  setupData.value = null
  setupStep.value = 'qr'
  setupDialogVisible.value = true
  try {
    const res = await totpApi.setup()
    setupData.value = res.data
    // 用 qrcode 库将 otpauthUrl 渲染为 Data URL
    if (res.data.otpauthUrl) {
      qrDataUrl.value = await QRCode.toDataURL(res.data.otpauthUrl, { width: 200, margin: 1 })
    }
  } catch {
    // handled by interceptor
    setupDialogVisible.value = false
  }
}

async function handleEnable() {
  if (!enableCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  enableLoading.value = true
  try {
    const res = await totpApi.enable(enableCode.value.trim())
    backupCodes.value = res.data.backupCodes
    setupStep.value = 'backup'
    ElMessage.success('2FA 启用成功')
  } catch {
    // handled by interceptor
  } finally {
    enableLoading.value = false
  }
}

function finishSetup() {
  setupDialogVisible.value = false
  loadStatus()
}

async function handleDisable() {
  if (!disableCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  disableLoading.value = true
  try {
    await totpApi.disable(disableCode.value.trim())
    ElMessage.success('2FA 已禁用')
    disableDialogVisible.value = false
    disableCode.value = ''
    loadStatus()
  } catch {
    // handled by interceptor
  } finally {
    disableLoading.value = false
  }
}

function copySecret() {
  if (setupData.value?.secret) {
    navigator.clipboard.writeText(setupData.value.secret).then(() => {
      ElMessage.success('密钥已复制')
    }).catch(() => {
      ElMessage.warning('复制失败,请手动选择复制')
    })
  }
}

function copyBackupCodes() {
  const text = backupCodes.value.join('\n')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('备用码已复制')
  }).catch(() => {
    ElMessage.warning('复制失败,请手动选择复制')
  })
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped lang="scss">
.security-settings-container {
  padding: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: bold;
}

.totp-section {
  padding: 8px 0;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .status-label {
    font-size: 14px;
    color: #606266;
  }
}

.action-row {
  margin-top: 16px;
}

.setup-qr {
  text-align: center;
}

.setup-tip {
  font-size: 13px;
  color: #606266;
  margin: 0 0 8px;
  text-align: left;
}

.qr-wrapper {
  display: flex;
  justify-content: center;
  margin: 12px 0;

  .qr-img {
    width: 200px;
    height: 200px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
  }
}

.secret-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 12px 0;

  .secret-label {
    font-size: 13px;
    color: #909399;
    white-space: nowrap;
  }
}

.backup-codes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.backup-code-item {
  font-family: 'Courier New', monospace;
  font-size: 15px;
  font-weight: bold;
  color: #303133;
  padding: 6px 12px;
  background: #fff;
  border-radius: 4px;
  text-align: center;
  letter-spacing: 1px;
}

.backup-actions {
  text-align: center;
  margin-top: 12px;
}
</style>
