<template>
  <div class="admin-layout">
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
            <span class="breadcrumb-item">管理后台</span>
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
                <span class="user-role">管理员</span>
              </div>
              <el-icon :size="12" class="dropdown-arrow"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><Setting /></el-icon>系统设置
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
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
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
import { Expand, Fold, ArrowRight, User, Setting, SwitchButton, ArrowDown, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const sidebarCollapsed = ref(false)

const currentTitle = computed(() => route.meta?.title || route.name || '')

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
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
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
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
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
  color: var(--gray-700);
  transform: scale(1.05);
}
.collapse-btn:active {
  transform: scale(0.95);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.breadcrumb-item {
  color: var(--gray-500);
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
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--gray-900) 0%, var(--gray-700) 100%);
  color: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  box-shadow: 0 2px 6px rgba(26, 24, 20, 0.15);
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
  background: var(--color-bg);
  overflow-y: auto;
  padding: var(--space-6);
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
