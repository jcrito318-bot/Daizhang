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

        <el-menu-item
          v-if="userStore.hasAnyRole(['ADMIN', 'ACCOUNTANT'])"
          index="/batch"
        >
          <el-icon><Operation /></el-icon>
          <template #title>批量操作</template>
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
          <el-menu-item index="/voucher/template">凭证模板</el-menu-item>
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
          <el-menu-item index="/report/cash-flow">现金流量表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="period">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>会计期间</span>
          </template>
          <el-menu-item index="/period">期间管理</el-menu-item>
          <el-menu-item
            v-if="userStore.hasAnyRole(['ADMIN', 'ACCOUNTANT'])"
            index="/period/close-wizard"
          >
            期末结账向导
          </el-menu-item>
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

        <el-sub-menu v-if="userStore.hasAnyRole(['ADMIN'])" index="system">
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
          <el-dropdown
            ref="accountSetDropdownRef"
            trigger="click"
            :hide-on-click="false"
            popper-class="account-set-dropdown-popper"
            class="account-set-dropdown"
          >
            <span class="account-set-trigger">
              <el-icon class="trigger-icon"><Wallet /></el-icon>
              <span class="current-name">{{ currentAccountSetName }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <div class="account-set-panel">
                <div class="search-wrap" @click.stop>
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索账套名称"
                    prefix-icon="Search"
                    clearable
                    size="small"
                  />
                </div>
                <div class="groups-scroll">
                  <!-- 收藏 -->
                  <template v-if="favoriteAccountSets.length">
                    <div class="group-title">⭐ 收藏</div>
                    <div
                      v-for="item in favoriteAccountSets"
                      :key="`fav-${item.id}`"
                      class="acct-item"
                      :class="{ 'is-current': item.id === appStore.currentAccountSetId }"
                      @click="selectAccountSet(item.id)"
                    >
                      <span class="acct-name" :title="item.name">{{ item.name }}</span>
                      <el-icon class="star-icon starred" @click.stop="toggleFav(item.id)"><StarFilled /></el-icon>
                    </div>
                  </template>
                  <!-- 最近访问 -->
                  <template v-if="recentAccountSetsView.length">
                    <div class="group-title">🕒 最近访问</div>
                    <div
                      v-for="item in recentAccountSetsView"
                      :key="`recent-${item.id}`"
                      class="acct-item"
                      :class="{ 'is-current': item.id === appStore.currentAccountSetId }"
                      @click="selectAccountSet(item.id)"
                    >
                      <span class="acct-name" :title="item.name">{{ item.name }}</span>
                      <el-icon
                        class="star-icon"
                        :class="{ starred: appStore.isFavorite(item.id) }"
                        @click.stop="toggleFav(item.id)"
                      >
                        <StarFilled v-if="appStore.isFavorite(item.id)" />
                        <Star v-else />
                      </el-icon>
                    </div>
                  </template>
                  <!-- 全部账套 -->
                  <div class="group-title">📋 全部账套</div>
                  <div
                    v-for="item in allAccountSetsView"
                    :key="`all-${item.id}`"
                    class="acct-item"
                    :class="{ 'is-current': item.id === appStore.currentAccountSetId }"
                    @click="selectAccountSet(item.id)"
                  >
                    <span class="acct-name" :title="item.name">{{ item.name }}</span>
                    <el-icon
                      class="star-icon"
                      :class="{ starred: appStore.isFavorite(item.id) }"
                      @click.stop="toggleFav(item.id)"
                    >
                      <StarFilled v-if="appStore.isFavorite(item.id)" />
                      <Star v-else />
                    </el-icon>
                  </div>
                  <div v-if="!allAccountSetsView.length" class="empty-tip">无匹配账套</div>
                </div>
              </div>
            </template>
          </el-dropdown>
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
import type { DropdownInstance } from 'element-plus'

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

// ===== 账套切换器优化:收藏置顶 + 最近访问 + 搜索 =====
const accountSetDropdownRef = ref<DropdownInstance>()
const searchKeyword = ref('')

const currentAccountSetName = computed(() => {
  const id = appStore.currentAccountSetId
  if (id == null) return '请选择账套'
  return appStore.accountSetList.find(a => a.id === id)?.name ?? '请选择账套'
})

/** 按关键词过滤账套(空关键词返回原列表) */
function filterByKeyword<T extends { name: string }>(list: T[]): T[] {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter(a => a.name.toLowerCase().includes(kw))
}

/** 收藏区:当前用户收藏且仍存在的账套 */
const favoriteAccountSets = computed(() => {
  const list = appStore.accountSetList.filter(a => appStore.favorites.includes(a.id))
  return filterByKeyword(list)
})

/** 最近访问区:仅展示仍存在于账套列表中的项 */
const recentAccountSetsView = computed(() => {
  const list = appStore.recentAccountSets.filter(r =>
    appStore.accountSetList.some(a => a.id === r.id)
  )
  return filterByKeyword(list)
})

/** 全部账套区:按名称排序 */
const allAccountSetsView = computed(() => {
  const list = [...appStore.accountSetList].sort((a, b) =>
    a.name.localeCompare(b.name, 'zh-CN')
  )
  return filterByKeyword(list)
})

function selectAccountSet(id: number) {
  appStore.setCurrentAccountSet(id)
  // 切换后自动关闭下拉
  accountSetDropdownRef.value?.handleClose()
}

async function toggleFav(id: number) {
  await appStore.toggleFavorite(id)
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
    // 加载账套偏好(收藏 + 最近访问),失败不阻塞应用(内部已捕获)
    await appStore.loadPreferences()
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

    .account-set-dropdown {
      margin-right: 20px;
    }

    .account-set-trigger {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 32px;
      padding: 0 12px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      background-color: #fff;
      color: #303133;
      font-size: 14px;
      cursor: pointer;
      max-width: 240px;
      transition: border-color 0.2s;
      outline: none;

      &:hover {
        border-color: #c0c4cc;
      }

      .trigger-icon {
        color: #909399;
      }

      .current-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .arrow-icon {
        color: #909399;
        font-size: 12px;
      }
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

<!-- 账套切换器下拉面板样式(非 scoped:el-dropdown 弹层被 teleport 到 body) -->
<style lang="scss">
.account-set-dropdown-popper {
  // 弹层最小宽度,保证搜索框与列表可读
  min-width: 260px;
  padding: 0;

  .account-set-panel {
    width: 260px;
    display: flex;
    flex-direction: column;
  }

  .search-wrap {
    padding: 8px 10px 6px;
    border-bottom: 1px solid #f0f0f0;
    position: sticky;
    top: 0;
    background: #fff;
    z-index: 1;
  }

  .groups-scroll {
    max-height: 340px;
    overflow-y: auto;
    padding: 4px 0;
  }

  .group-title {
    padding: 6px 14px;
    font-size: 12px;
    color: #909399;
    line-height: 18px;
    user-select: none;
  }

  .acct-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 14px;
    cursor: pointer;
    font-size: 14px;
    color: #303133;

    &:hover {
      background-color: #f5f7fa;
    }

    &.is-current {
      color: #409eff;
      background-color: #ecf5ff;
    }

    .acct-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      margin-right: 8px;
    }

    .star-icon {
      color: #c0c4cc;
      font-size: 16px;
      flex-shrink: 0;

      &:hover {
        color: #f7ba2a;
      }

      &.starred {
        color: #f7ba2a;
      }
    }
  }

  .empty-tip {
    padding: 16px;
    text-align: center;
    color: #c0c4cc;
    font-size: 13px;
  }
}
</style>
