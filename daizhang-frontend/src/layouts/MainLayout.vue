<template>
  <el-container class="main-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <img src="@/assets/vite.svg" alt="logo" class="logo-img" />
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">代账系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
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
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
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
            v-model="currentAccountSetId"
            placeholder="请选择账套"
            class="account-set-select"
            @change="handleAccountSetChange"
          >
            <el-option
              v-for="item in accountSetList"
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { accountSetApi } from '@/api/accountset'
import type { AccountSetVO } from '@/types/accountset'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const accountSetList = ref<AccountSetVO[]>([])
const currentAccountSetId = ref<number | null>(appStore.currentAccountSetId)

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

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
  try {
    if (!userStore.userInfo) {
      await userStore.getUserInfo()
    }
    const res = await accountSetApi.getList()
    accountSetList.value = res.data
    if (accountSetList.value.length > 0 && !appStore.currentAccountSetId) {
      const firstId = accountSetList.value[0].id
      currentAccountSetId.value = firstId
      appStore.setCurrentAccountSet(firstId)
    } else {
      currentAccountSetId.value = appStore.currentAccountSetId
    }
  } catch {
    // ignore
  }
})
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;

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
