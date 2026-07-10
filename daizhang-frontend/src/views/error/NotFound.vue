<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <p class="error-message">抱歉，您访问的页面不存在</p>
      <p class="error-hint">请检查地址是否正确，或返回首页继续操作</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped lang="scss">
.not-found-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.not-found-content {
  text-align: center;

  .error-code {
    font-size: 120px;
    font-weight: 600;
    color: #409eff;
    margin: 0;
    line-height: 1;
  }

  .error-message {
    font-size: 20px;
    color: #303133;
    margin: 16px 0 8px;
  }

  .error-hint {
    font-size: 14px;
    color: #909399;
    margin: 0 0 24px;
  }
}
</style>
