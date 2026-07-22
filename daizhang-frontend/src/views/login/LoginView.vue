<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>代账系统</h2>
        <p>专业代理记账管理平台</p>
      </div>

      <!-- 步骤1: 用户名密码 -->
      <el-form v-if="step === 'login'" ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤2: 2FA 验证码输入 -->
      <div v-else-if="step === 'totp'" class="totp-step">
        <el-alert title="双因素认证" type="info" :closable="false" show-icon style="margin-bottom: 20px">
          请输入 Authenticator 应用中显示的 6 位验证码,或输入备用恢复码
        </el-alert>
        <el-form ref="totpFormRef" :model="totpForm" :rules="totpRules" class="login-form">
          <el-form-item prop="code">
            <el-input
              v-model="totpForm.code"
              placeholder="请输入 6 位验证码"
              prefix-icon="Key"
              size="large"
              maxlength="6"
              @keyup.enter="handleTotpLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleTotpLogin">
              验 证
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button size="large" class="login-btn" @click="backToLogin">返回</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤3: 密码过期强制改密 -->
      <div v-else-if="step === 'changePassword'" class="change-pwd-step">
        <el-alert title="密码已过期" type="warning" :closable="false" show-icon style="margin-bottom: 20px">
          您的密码已超过有效期,请修改密码后继续使用
        </el-alert>
        <el-form ref="changePwdFormRef" :model="changePwdForm" :rules="changePwdRules" class="login-form">
          <el-form-item prop="oldPassword">
            <el-input v-model="changePwdForm.oldPassword" type="password" placeholder="原密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item prop="newPassword">
            <el-input v-model="changePwdForm.newPassword" type="password" placeholder="新密码(至少8位,含大小写+数字)" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="changePwdForm.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleChangePassword" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { passwordApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const totpFormRef = ref<FormInstance>()
const changePwdFormRef = ref<FormInstance>()
const loading = ref(false)

/** 登录步骤:login=账号密码 / totp=2FA验证 / changePassword=密码过期改密 */
const step = ref<'login' | 'totp' | 'changePassword'>('login')

/** 2FA 临时令牌(密码验证通过后下发) */
let tempToken = ''

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 2FA 表单
const totpForm = reactive({
  code: ''
})
const totpRules: FormRules = {
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 改密表单
const changePwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changePwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (!value || value.length < 8) {
          callback(new Error('密码长度不能少于8位'))
        } else if (!/[A-Z]/.test(value) || !/[a-z]/.test(value) || !/\d/.test(value)) {
          callback(new Error('密码必须包含大写字母、小写字母和数字'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== changePwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function safeRedirect() {
  const redirect = (route.query.redirect as string) || '/dashboard'
  // 防 Open Redirect：必须以 / 开头且不以 // 开头，否则回退到 /dashboard
  const safe = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/dashboard'
  router.push(safe)
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await userStore.login({ username: loginForm.username, password: loginForm.password })
    // P4.2: 需要双因素认证 → 进入 2FA 输入步骤
    if (data.requiresTwoFactor && data.tempToken) {
      tempToken = data.tempToken
      step.value = 'totp'
      return
    }
    // P4.3: 密码过期 → 进入改密步骤
    if (data.passwordExpired) {
      step.value = 'changePassword'
      ElMessage.warning('密码已过期,请修改密码')
      return
    }
    ElMessage.success('登录成功')
    safeRedirect()
  } catch {
    // error already handled by request interceptor
  } finally {
    loading.value = false
  }
}

async function handleTotpLogin() {
  const valid = await totpFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await userStore.loginTotp({ tempToken, code: totpForm.code })
    // 2FA 通过后仍可能密码过期
    if (data.passwordExpired) {
      step.value = 'changePassword'
      ElMessage.warning('密码已过期,请修改密码')
      return
    }
    ElMessage.success('登录成功')
    safeRedirect()
  } catch {
    // error already handled by request interceptor
  } finally {
    loading.value = false
  }
}

function backToLogin() {
  step.value = 'login'
  totpForm.code = ''
  tempToken = ''
}

async function handleChangePassword() {
  const valid = await changePwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await passwordApi.changePassword({
      oldPassword: changePwdForm.oldPassword,
      newPassword: changePwdForm.newPassword
    })
    ElMessage.success('密码修改成功,请重新登录')
    // 改密后清除登录态,要求重新登录
    await userStore.logout()
    step.value = 'login'
    changePwdForm.oldPassword = ''
    changePwdForm.newPassword = ''
    changePwdForm.confirmPassword = ''
  } catch {
    // error already handled by request interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  h2 {
    font-size: 28px;
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

.login-btn {
  width: 100%;
}
</style>
