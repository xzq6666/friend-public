<template>
  <div class="user-layout">
    <Sidebar :collapsed="sidebarCollapsed" @toggle="sidebarCollapsed = !sidebarCollapsed" />
    <div class="main-area" :class="{ expanded: sidebarCollapsed }">
      <header class="top-header">
        <div class="header-left">
          <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
            <el-icon :size="18">
              <Expand v-if="sidebarCollapsed" />
              <Fold v-else />
            </el-icon>
          </button>
          <div class="breadcrumb">
            <span class="breadcrumb-item">{{ layoutTitle }}</span>
            <el-icon :size="12" class="breadcrumb-sep"><ArrowRight /></el-icon>
            <span class="breadcrumb-current">{{ currentTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <NotificationBell />
          <el-dropdown trigger="click" placement="bottom-end">
            <button class="user-info">
              <div class="user-avatar">{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
              <div class="user-meta">
                <span class="user-name">{{ user?.username || '用户' }}</span>
                <span class="user-role">{{ roleLabel }}</span>
              </div>
              <el-icon :size="12" class="dropdown-arrow"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/favorites')">
                  <el-icon><Star /></el-icon>我的收藏
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/')">
                  <el-icon><HomeFilled /></el-icon>返回首页
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content" ref="mainContentRef">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="['UserDashboard', 'BrowseJobs', 'Favorites', 'ChatHistory']">
              <component :is="Component" :key="currentRoute.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import Sidebar from '../components/Sidebar.vue'
import NotificationBell from '../components/NotificationBell.vue'
import { Expand, Fold, ArrowRight, User, Star, SwitchButton, ArrowDown, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const sidebarCollapsed = ref(false)
const mainContentRef = ref(null)

const currentTitle = computed(() => route.meta?.title || route.name || '')
const layoutTitle = computed(() => {
  const map = { EMPLOYEE: '求职者', EMPLOYER: '企业', ADMIN: '管理后台' }
  return map[userStore.user?.userType] || '用户中心'
})
const roleLabel = computed(() => {
  const map = { EMPLOYEE: '求职者', EMPLOYER: '企业', ADMIN: '管理员' }
  return map[userStore.user?.userType] || ''
})

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(async () => {
  if (userStore.token && !userStore.user) {
    await userStore.fetchUserInfo()
  }
})
</script>

<style scoped>
.user-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

/* ── 主区域 ───────────────────────────────── */
.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--duration-normal) var(--ease-out);
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.main-area.expanded {
  margin-left: var(--sidebar-collapsed-width);
}

/* ── 顶部导航 ─────────────────────────────── */
.top-header {
  height: var(--header-height);
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.88);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--gray-500);
  transition: all var(--duration-fast) var(--ease-out);
}
.collapse-btn:hover {
  background: var(--gray-100);
  color: var(--gray-800);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.breadcrumb-item {
  color: var(--gray-400);
}

.breadcrumb-sep {
  color: var(--gray-300);
}

.breadcrumb-current {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
}

/* ── 右侧功能区 ──────────────────────────── */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/* ── 用户信息 ─────────────────────────────── */
.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 4px 8px;
  border: none;
  background: transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.user-info:hover {
  background: var(--gray-100);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-500);
  color: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
}

.user-name {
  font-size: var(--text-sm);
  color: var(--gray-800);
  font-weight: var(--weight-medium);
  line-height: 1.2;
}

.user-role {
  font-size: 10px;
  color: var(--gray-400);
}

.dropdown-arrow {
  color: var(--gray-400);
  transition: transform var(--duration-fast) var(--ease-out);
}

/* ── 内容区 ───────────────────────────────── */
.content {
  flex: 1;
  background: var(--bg-primary);
  overflow-y: auto;
  padding: var(--space-6);
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0 !important;
  }
  .user-meta {
    display: none;
  }
  .breadcrumb {
    display: none;
  }
  .top-header {
    padding: 0 var(--space-4);
  }
  .content {
    padding: var(--space-4);
  }
}
</style>
