<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-header">
      <div class="logo-area">
        <div class="logo-mark">
          <div class="mark-dot"></div>
          <div class="mark-dot delay"></div>
        </div>
        <transition name="fade">
          <span v-show="!collapsed" class="logo-text">智能招聘</span>
        </transition>
      </div>
    </div>

    <nav class="sidebar-nav">
      <div class="nav-section">
        <router-link :to="isAdmin ? '/admin/dashboard' : '/dashboard'" class="nav-item" :class="{ active: isActive(isAdmin ? '/admin/dashboard' : '/dashboard') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          <span v-show="!collapsed">仪表盘</span>
        </router-link>
      </div>

      <!-- 求职者 -->
      <template v-if="isEmployee">
        <div v-show="!collapsed" class="nav-label">求职服务</div>
        <router-link to="/my-resume" class="nav-item" :class="{ active: isActive('/my-resume') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <span v-show="!collapsed">我的简历</span>
        </router-link>
        <router-link to="/browse-jobs" class="nav-item" :class="{ active: isActive('/browse-jobs') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <span v-show="!collapsed">浏览职位</span>
        </router-link>
        <router-link to="/my-applications" class="nav-item" :class="{ active: isActive('/my-applications') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="16" y1="4" x2="20" y2="4"/><line x1="18" y1="2" x2="18" y2="6"/><path d="M4 6h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/></svg>
          <span v-show="!collapsed">我的投递</span>
        </router-link>
        <router-link to="/job-subscription" class="nav-item" :class="{ active: isActive('/job-subscription') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          <span v-show="!collapsed">职位订阅</span>
        </router-link>
        <router-link to="/my-interviews" class="nav-item" :class="{ active: isActive('/my-interviews') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <span v-show="!collapsed">面试安排</span>
        </router-link>
        <router-link to="/skill-graph" class="nav-item" :class="{ active: isActive('/skill-graph') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v2"/><path d="M12 21v2"/><path d="M4.22 4.22l1.42 1.42"/><path d="M18.36 18.36l1.42 1.42"/><path d="M1 12h2"/><path d="M21 12h2"/><path d="M4.22 19.78l1.42-1.42"/><path d="M18.36 5.64l1.42-1.42"/></svg>
          <span v-show="!collapsed">能力图谱</span>
        </router-link>
        <router-link to="/application-board" class="nav-item" :class="{ active: isActive('/application-board') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
          <span v-show="!collapsed">求职看板</span>
        </router-link>
        <router-link to="/visit-history" class="nav-item" :class="{ active: isActive('/visit-history') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          <span v-show="!collapsed">访问记录</span>
        </router-link>
        <router-link to="/report-history" class="nav-item" :class="{ active: isActive('/report-history') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span v-show="!collapsed">举报历史</span>
        </router-link>
      </template>

      <!-- 企业 -->
      <template v-if="isEmployer || isAdmin">
        <div v-show="!collapsed" class="nav-label">{{ isAdmin ? '招聘管理' : '企业管理' }}</div>
        <router-link :to="isAdmin ? '/admin/jobs' : (isEmployer ? '/my-jobs' : '/jobs')" class="nav-item" :class="{ active: isActive(isAdmin ? '/admin/jobs' : (isEmployer ? '/my-jobs' : '/jobs')) }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
          <span v-show="!collapsed">职位管理</span>
        </router-link>
        <router-link :to="isAdmin ? '/admin/resumes' : (isEmployer ? '/my-resumes-pool' : '/resumes')" class="nav-item" :class="{ active: isActive(isAdmin ? '/admin/resumes' : (isEmployer ? '/my-resumes-pool' : '/resumes')) }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span v-show="!collapsed">简历管理</span>
        </router-link>
        <router-link :to="isAdmin ? '/admin/candidates' : '/my-candidates'" class="nav-item" :class="{ active: isActive(isAdmin ? '/admin/candidates' : '/my-candidates') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <span v-show="!collapsed">{{ isAdmin ? '投递记录' : '候选人' }}</span>
        </router-link>
        <router-link v-if="isEmployer" :to="'/talent-market'" class="nav-item" :class="{ active: isActive('/talent-market') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/><path d="M9 21v-6a4 4 0 0 1 4-4h2"/></svg>
          <span v-show="!collapsed">人才市场</span>
        </router-link>
        <router-link v-if="!isAdmin" to="/my-manage-interviews" class="nav-item" :class="{ active: isActive('/my-manage-interviews') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <span v-show="!collapsed">面试管理</span>
        </router-link>
        <router-link v-if="isEmployer" to="/visit-history" class="nav-item" :class="{ active: isActive('/visit-history') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          <span v-show="!collapsed">访问记录</span>
        </router-link>
        <router-link v-if="isEmployer" to="/report-history" class="nav-item" :class="{ active: isActive('/report-history') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span v-show="!collapsed">举报历史</span>
        </router-link>
        <router-link v-if="isEmployer" to="/my-forum" class="nav-item" :class="{ active: isActive('/my-forum') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
          <span v-show="!collapsed">论坛管理</span>
        </router-link>
      </template>

      <!-- 管理员 -->
      <template v-if="isAdmin">
        <div v-show="!collapsed" class="nav-label">系统管理</div>
        <router-link to="/admin/users" class="nav-item" :class="{ active: isActive('/admin/users') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <span v-show="!collapsed">用户管理</span>
        </router-link>
        <router-link to="/admin/manage-keywords" class="nav-item" :class="{ active: isActive('/admin/manage-keywords') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
          <span v-show="!collapsed">关键词</span>
        </router-link>
        <router-link to="/admin/operation-logs" class="nav-item" :class="{ active: isActive('/admin/operation-logs') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <span v-show="!collapsed">操作日志</span>
        </router-link>
        <router-link to="/admin/data-analysis" class="nav-item" :class="{ active: isActive('/admin/data-analysis') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
          <span v-show="!collapsed">数据分析</span>
        </router-link>
        <router-link to="/admin/data-export" class="nav-item" :class="{ active: isActive('/admin/data-export') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          <span v-show="!collapsed">数据导出</span>
        </router-link>
        <router-link to="/admin/company-verify" class="nav-item" :class="{ active: isActive('/admin/company-verify') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/></svg>
          <span v-show="!collapsed">企业认证审核</span>
        </router-link>
        <router-link to="/admin/report-manage" class="nav-item" :class="{ active: isActive('/admin/report-manage') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span v-show="!collapsed">举报管理</span>
        </router-link>
        <router-link to="/admin/announcement-manage" class="nav-item" :class="{ active: isActive('/admin/announcement-manage') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/><line x1="12" y1="2" x2="12" y2="4"/></svg>
          <span v-show="!collapsed">公告管理</span>
        </router-link>
        <router-link to="/admin/forum-manage" class="nav-item" :class="{ active: isActive('/admin/forum-manage') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
          <span v-show="!collapsed">论坛管理</span>
        </router-link>
        <router-link to="/admin/sensitive-word-manage" class="nav-item" :class="{ active: isActive('/admin/sensitive-word-manage') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><line x1="9" y1="9" x2="15" y2="15"/><line x1="15" y1="9" x2="9" y2="15"/></svg>
          <span v-show="!collapsed">敏感词管理</span>
        </router-link>
      </template>

      <!-- 通用工具 -->
      <template v-if="isEmployee || isEmployer">
        <div v-show="!collapsed" class="nav-label">工具箱</div>
        <router-link to="/match" class="nav-item" :class="{ active: isActive('/match') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
          <span v-show="!collapsed">智能匹配</span>
        </router-link>
        <router-link to="/favorites" class="nav-item" :class="{ active: isActive('/favorites') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          <span v-show="!collapsed">我的收藏</span>
        </router-link>
        <router-link to="/chat-history" class="nav-item" :class="{ active: isActive('/chat-history') }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <span v-show="!collapsed">历史聊天</span>
        </router-link>
      </template>

      <!-- 返回首页 -->
      <div v-show="!collapsed" class="nav-label">快捷入口</div>
      <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
        <span v-show="!collapsed">返回首页</span>
      </router-link>

      <div v-show="!collapsed" class="nav-label">设置</div>
      <router-link v-if="isEmployer" to="/company-profile" class="nav-item" :class="{ active: isActive('/company-profile') }">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 21h18M5 21V7l8-4 8 4v14M8 21v-9a4 4 0 0 1 4-4v0a4 4 0 0 1 4 4v9"/></svg>
        <span v-show="!collapsed">企业认证</span>
      </router-link>
      <router-link to="/profile" class="nav-item" :class="{ active: isActive('/profile') }">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        <span v-show="!collapsed">个人中心</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

defineProps({ collapsed: Boolean })

const route = useRoute()
const userStore = useUserStore()

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
const isEmployee = computed(() => userStore.user?.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')
</script>

<style scoped>
/* ── 侧边栏容器 ────────────────────────────── */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  z-index: 200;
  transition: width var(--duration-normal) var(--ease-out);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 1px 0 3px rgba(0, 0, 0, 0.03);
}
.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

/* ── Logo 区 ──────────────────────────────── */
.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 var(--space-5);
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-border);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  overflow: hidden;
}

.logo-mark {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
  position: relative;
}

.mark-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gray-900);
  transition: all var(--duration-normal) var(--ease-out);
}
.mark-dot.delay {
  background: var(--gray-400);
}

.logo-area:hover .mark-dot:first-child {
  transform: translateX(2px);
}
.logo-area:hover .mark-dot.delay {
  transform: translateX(-2px);
}

.logo-text {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  white-space: nowrap;
  letter-spacing: 0.02em;
}

/* ── 导航 ────────────────────────────────── */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3) 0;
}

.nav-section {
  margin-bottom: var(--space-2);
}

.nav-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--gray-400);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: var(--space-5) var(--space-5) var(--space-1);
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 38px;
  margin: 1px var(--space-3);
  padding: 0 var(--space-3);
  border-radius: var(--radius-md);
  color: var(--gray-600);
  font-size: var(--text-sm);
  font-weight: var(--weight-normal);
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-out);
  position: relative;
}

.nav-item:hover {
  background: var(--gray-50);
  color: var(--gray-800);
  text-decoration: none;
}

.nav-item.active {
  background: var(--gray-900);
  color: var(--color-surface);
  font-weight: var(--weight-medium);
  box-shadow: 0 2px 8px rgba(26, 24, 20, 0.15);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: -var(--space-3);
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--gray-900);
  border-radius: 0 2px 2px 0;
}

.nav-item svg {
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out);
}

.nav-item:hover svg {
  transform: scale(1.1);
}

/* ── 滚动条 ──────────────────────────────── */
.sidebar-nav::-webkit-scrollbar {
  width: 3px;
}
.sidebar-nav::-webkit-scrollbar-thumb {
  background: var(--gray-200);
  border-radius: var(--radius-full);
}

/* ── 折叠状态 ─────────────────────────────── */
.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 0;
}
.sidebar.collapsed .nav-item.active::before {
  left: 0;
  height: 24px;
}
</style>
