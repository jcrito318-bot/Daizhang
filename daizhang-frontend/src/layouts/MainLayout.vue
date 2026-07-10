<template>
  <el-container class="main-layout">
    <!-- 移动端遮罩 -->
    <div v-if="isMobile && mobileSidebarOpen" class="mobile-overlay" @click="closeMobileSidebar"></div>
    <el-aside
      :width="sidebarWidth"
      :class="['sidebar', { 'sidebar-mobile': isMobile, 'sidebar-mobile-open': isMobile && mobileSidebarOpen }]"
    >
      <div class="logo">
        <img src="@/assets/vite.svg" alt="logo" class="logo-img" />
        <span v-show="!isSidebarCollapsed" class="logo-text">代账系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isSidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="closeMobileSidebar"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-sub-menu index="account">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>账套管理</span>
          </template>
          <el-menu-item index="/accountset">账套列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="subject">
          <template #title>
            <el-icon><Collection /></el-icon>
            <span>科目管理</span>
          </template>
          <el-menu-item index="/subject">科目列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="voucher">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>凭证管理</span>
          </template>
          <el-menu-item index="/voucher">凭证列表</el-menu-item>
          <el-menu-item index="/voucher/create">新增凭证</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ledger">
          <template #title>
            <el-icon><Notebook /></el-icon>
            <span>账簿管理</span>
          </template>
          <el-menu-item index="/ledger/detail">明细账</el-menu-item>
          <el-menu-item index="/ledger/general">总账</el-menu-item>
          <el-menu-item index="/ledger/subject-balance">科目余额表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="report">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>财务报表</span>
          </template>
          <el-menu-item index="/report/balance-sheet">资产负债表</el-menu-item>
          <el-menu-item index="/report/income-statement">利润表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="period">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>会计期间</span>
          </template>
          <el-menu-item index="/period">期间管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="bank">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>银行对账</span>
          </template>
          <el-menu-item index="/bank/transaction">银行流水</el-menu-item>
          <el-menu-item index="/bank/reconciliation">银行对账</el-menu-item>
          <el-menu-item index="/bank/balance-adjustment">余额调节表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="document">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>票据管理</span>
          </template>
          <el-menu-item index="/document">票据列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="customer">
          <template #title>
            <el-icon><User /></el-icon>
            <span>客户管理</span>
          </template>
          <el-menu-item index="/customer/list">客户列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="asset">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>固定资产</span>
          </template>
          <el-menu-item index="/asset/category">资产分类</el-menu-item>
          <el-menu-item index="/asset/list">资产列表</el-menu-item>
          <el-menu-item index="/asset/depreciation">资产折旧</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="salary">
          <template #title>
            <el-icon><Coin /></el-icon>
            <span>工资管理</span>
          </template>
          <el-menu-item index="/salary/employee">员工管理</el-menu-item>
          <el-menu-item index="/salary/sheet">工资表</el-menu-item>
          <el-menu-item index="/salary/calculation">工资计算</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="tax">
          <template #title>
            <el-icon><Tickets /></el-icon>
            <span>税务管理</span>
          </template>
          <el-menu-item index="/tax/declaration">税务申报</el-menu-item>
          <el-menu-item index="/tax/calculation">税务计算</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
          <el-menu-item index="/system/setting">系统设置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleMobileSidebar">
            <Fold v-if="!isSidebarCollapsed && !isMobile" />
            <Expand v-else-if="appStore.sidebarCollapsed && !isMobile" />
            <Menu v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title && currentRoute.name !== 'Dashboard'">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-select
            v-model="appStore.currentAccountSetId"
            placeholder="请选择账套"
            class="account-set-select"
            @change="handleAccountSetChange"
          >
            <el-option
              v-for="item in appStore.accountSetList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><UserFilled /></el-icon>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

// 移动端响应式:小屏(<768px)下侧边栏改为抽屉模式
const isMobile = ref(false)
const mobileSidebarOpen = ref(false)

// 移动端:抽屉模式下展开=false(用mobileSidebarOpen控制),收起模式恒为收起
// 桌面端:沿用 sidebarCollapsed
const isSidebarCollapsed = computed(() => {
  if (isMobile.value) return false  // 移动端抽屉打开时显示完整菜单,关闭时由CSS隐藏
  return appStore.sidebarCollapsed
})
const sidebarWidth = computed(() => {
  if (isMobile.value) return '220px'  // 移动端抽屉固定宽度
  return appStore.sidebarCollapsed ? '64px' : '220px'
})

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  // 进入移动端时关闭抽屉
  if (isMobile.value) {
    mobileSidebarOpen.value = false
  }
}

function toggleMobileSidebar() {
  if (isMobile.value) {
    mobileSidebarOpen.value = !mobileSidebarOpen.value
  } else {
    appStore.toggleSidebar()
  }
}

function closeMobileSidebar() {
  if (isMobile.value) {
    mobileSidebarOpen.value = false
  }
}

function handleAccountSetChange(id: number) {
  appStore.setCurrentAccountSet(id)
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    // TODO: navigate to profile page
  }
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  try {
    if (!userStore.userInfo) {
      await userStore.getUserInfo()
    }
    // 通过 store 加载账套列表(带缓存),避免每次进入页面重复请求
    await appStore.loadAccountSetList()
  } catch {
    // ignore
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})

// 路由切换时,移动端自动关闭抽屉
watch(() => route.path, () => {
  closeMobileSidebar()
})
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

// 移动端遮罩
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;

  // 移动端:抽屉模式,默认隐藏,通过 translateX 滑入
  &.sidebar-mobile {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &.sidebar-mobile-open {
      transform: translateX(0);
    }
  }

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    background-color: #263445;

    .logo-img {
      width: 32px;
      height: 32px;
    }

    .logo-text {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      margin-left: 10px;
      white-space: nowrap;
    }
  }

  .el-menu {
    border-right: none;
  }
}

.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 1;

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 16px;
      color: #606266;

      &:hover {
        color: #409eff;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;

    .account-set-select {
      width: 200px;
      margin-right: 20px;
    }

    .user-dropdown {
      display: flex;
      align-items: center;
      cursor: pointer;
      color: #606266;

      .username {
        margin: 0 6px;
        font-size: 14px;
      }

      &:hover {
        color: #409eff;
      }
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  overflow-y: auto;
}
</style>
