<template>
  <header class="topnav" :class="{ 'has-shadow': showShadow, 'with-search': isOnMatchPage }">
    <div class="topnav-inner">
      <!-- Logo -->
      <router-link to="/dashboard" class="topnav-brand">
        <div class="brand-icon">
          <div class="brand-dots">
            <span class="dot"></span>
            <span class="dot delay"></span>
          </div>
        </div>
        <span class="brand-text">智能招聘匹配</span>
      </router-link>

      <!-- 主导航 -->
      <nav class="topnav-menu" ref="menuRef">
        <!-- 求职者 -->
        <template v-if="isEmployee">
          <router-link
            v-for="item in employeeNavItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            <span class="nav-icon" v-html="item.icon"></span>
            <span class="nav-label">{{ item.label }}</span>
          </router-link>
        </template>

        <!-- 企业 -->
        <template v-if="isEmployer">
          <router-link
            v-for="item in employerNavItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            <span class="nav-icon" v-html="item.icon"></span>
            <span class="nav-label">{{ item.label }}</span>
          </router-link>
        </template>
      </nav>

      <!-- 右侧用户区 -->
      <div class="topnav-right">
        <!-- 通知铃铛 -->
        <NotificationBell v-if="isLoggedIn" />

        <el-dropdown trigger="click" placement="bottom-end" @visible-change="dropdownVisible = $event">
          <button class="user-card" :aria-expanded="dropdownVisible">
            <div class="user-avatar">{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
            <div class="user-info">
              <span class="user-name">{{ user?.username || '用户' }}</span>
              <span class="user-role">{{ roleLabel }}</span>
            </div>
            <span class="dropdown-arrow" :class="{ 'open': dropdownVisible }">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item @click="$router.push('/favorites')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                我的收藏
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 滚动渐变遮罩 — 指示下方有可滚动内容 -->
    <div class="nav-scroll-indicator left" v-show="canScrollLeft"></div>
    <div class="nav-scroll-indicator right" v-show="canScrollRight"></div>
  </header>

  <!-- 智能匹配搜索栏 -->
  <MatchSearchBar
    v-if="isOnMatchPage"
    v-model="matchSearchParams"
    :is-employee="isEmployee"
    @search="handleMatchSearch"
    @filter="handleMatchFilter"
  />
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import MatchSearchBar from './MatchSearchBar.vue'
import NotificationBell from './NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const menuRef = ref(null)
const showShadow = ref(false)
const dropdownVisible = ref(false)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

// 智能匹配搜索参数
const matchSearchParams = ref({
  keyword: '',
  location: [],
  salaryRange: '',
  experience: '',
  education: '',
  skills: [],
  minScore: ''
})

// 是否在智能匹配页面
const isOnMatchPage = computed(() => route.path === '/match')

// 处理智能匹配搜索
const handleMatchSearch = (params) => {
  // 触发搜索事件，由 Match.vue 监听
  window.dispatchEvent(new CustomEvent('match-search', { detail: params }))
}

// 处理智能匹配筛选
const handleMatchFilter = (filters) => {
  // 触发筛选事件，由 Match.vue 监听
  window.dispatchEvent(new CustomEvent('match-filter', { detail: filters }))
}

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
const isEmployee = computed(() => userStore.user?.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')

const roleLabel = computed(() => {
  const map = { EMPLOYEE: '求职者', EMPLOYER: '企业', ADMIN: '管理员' }
  return map[userStore.user?.userType] || ''
})

// 导航配置 — 求职者
const employeeNavItems = [
  { path: '/dashboard', label: '仪表盘', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>' },
  { path: '/my-resume', label: '我的简历', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
  { path: '/browse-jobs', label: '浏览职位', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>' },
  { path: '/match', label: '智能匹配', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>' },
  { path: '/my-applications', label: '我的投递', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="16" y1="4" x2="20" y2="4"/><line x1="18" y1="2" x2="18" y2="6"/><path d="M4 6h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"/></svg>' },
  { path: '/my-interviews', label: '面试安排', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>' },
  { path: '/favorites', label: '我的收藏', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' },
  { path: '/visit-history', label: '访问记录', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' },
]

// 导航配置 — 企业
const employerNavItems = [
  { path: '/dashboard', label: '仪表盘', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>' },
  { path: '/my-jobs', label: '职位管理', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>' },
  { path: '/my-candidates', label: '候选人', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
  { path: '/match', label: '智能匹配', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>' },
  { path: '/my-resumes-pool', label: '简历库', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' },
  { path: '/my-manage-interviews', label: '面试管理', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>' },
  { path: '/favorites', label: '我的收藏', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' },
  { path: '/visit-history', label: '访问记录', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' },
  { path: '/my-forum', label: '论坛管理', icon: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>' },
]

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 滚动阴影
const handleScroll = () => {
  showShadow.value = window.scrollY > 4
}

// 检测导航栏是否可滚动
const checkScroll = () => {
  if (!menuRef.value) return
  const el = menuRef.value
  canScrollLeft.value = el.scrollLeft > 2
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 2
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  checkScroll()
  if (menuRef.value) {
    menuRef.value.addEventListener('scroll', checkScroll, { passive: true })
    window.addEventListener('resize', checkScroll)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', checkScroll)
  if (menuRef.value) {
    menuRef.value.removeEventListener('scroll', checkScroll)
  }
})
</script>

<style scoped>
/* ── 顶部导航容器 ──────────────────────────── */
.topnav {
  height: 52px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 200;
  flex-shrink: 0;
  transition: box-shadow var(--duration-normal) var(--ease-out);
}

.topnav.has-shadow {
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.topnav.with-search {
  border-bottom: none;
}

.topnav-inner {
  max-width: 1280px;
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/* ── Logo ─────────────────────────────────── */
.topnav-brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  flex-shrink: 0;
  padding: 5px var(--space-2);
  border-radius: var(--radius-md);
  margin-right: var(--space-1);
  transition: all var(--duration-fast) var(--ease-out);
}

.topnav-brand:hover {
  background: var(--gray-50);
  text-decoration: none;
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--gray-900);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform var(--duration-normal) var(--ease-out);
}

.topnav-brand:hover .brand-icon {
  transform: rotate(-5deg) scale(1.05);
}

.brand-dots {
  display: flex;
  gap: 4px;
}

.brand-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-surface);
}

.brand-dots .dot.delay {
  background: rgba(255, 255, 255, 0.5);
}

.brand-text {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  letter-spacing: 0.01em;
  white-space: nowrap;
}

/* ── 主导航菜单 ────────────────────────────── */
.topnav-menu {
  display: flex;
  align-items: center;
  gap: 1px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
  mask-image: linear-gradient(to right, transparent 0, black 12px, black calc(100% - 12px), transparent 100%);
  -webkit-mask-image: linear-gradient(to right, transparent 0, black 12px, black calc(100% - 12px), transparent 100%);
}

.topnav-menu::-webkit-scrollbar {
  display: none;
}

/* 滚动指示器 */
.nav-scroll-indicator {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 24px;
  pointer-events: none;
  z-index: 2;
}
.nav-scroll-indicator.left {
  left: 0;
  background: linear-gradient(to right, var(--color-surface), transparent);
}
.nav-scroll-indicator.right {
  right: 0;
  background: linear-gradient(to left, var(--color-surface), transparent);
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 10px;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--gray-500);
  text-decoration: none;
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out);
  font-weight: var(--weight-normal);
  flex-shrink: 0;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  opacity: 0.65;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.nav-link:hover {
  background: var(--gray-50);
  color: var(--gray-800);
  text-decoration: none;
}

.nav-link:hover .nav-icon {
  opacity: 1;
}

.nav-link.active {
  background: var(--gray-900);
  color: var(--color-surface);
  font-weight: var(--weight-medium);
}

.nav-link.active .nav-icon {
  opacity: 1;
}

/* ── 右侧用户区 ────────────────────────────── */
.topnav-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.user-card {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 3px 6px 3px 3px;
  border: none;
  background: transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
}

.user-card:hover {
  background: var(--gray-50);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-500);
  color: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 0;
  line-height: 1.3;
}

.user-name {
  font-size: var(--text-sm);
  color: var(--gray-800);
  font-weight: var(--weight-medium);
}

.user-role {
  font-size: 10px;
  color: var(--gray-400);
  font-weight: var(--weight-normal);
}

.dropdown-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-sm);
  color: var(--gray-400);
  transition: all var(--duration-fast) var(--ease-out);
  margin-left: 2px;
}

.dropdown-arrow.open {
  transform: rotate(180deg);
  color: var(--gray-600);
}

.user-card:hover .dropdown-arrow {
  color: var(--gray-600);
}

/* ── 响应式：中等屏幕 ─────────────────────── */
@media (max-width: 1024px) {
  .brand-text {
    font-size: var(--text-sm);
  }

  .nav-link {
    padding: 0 8px;
  }

  .user-info {
    display: none;
  }
}

/* ── 响应式：小屏幕 ───────────────────────── */
@media (max-width: 768px) {
  .topnav-inner {
    padding: 0 var(--space-3);
    gap: var(--space-2);
  }

  .brand-text {
    display: none;
  }

  .topnav-brand {
    margin-right: 0;
  }

  .nav-link {
    padding: 0 6px;
    height: 30px;
  }

  .nav-label {
    display: none;
  }

  .nav-icon {
    opacity: 1;
  }

  .nav-link.active {
    background: transparent;
    color: var(--gray-900);
  }

  .nav-link.active .nav-icon {
    color: var(--gray-900);
  }

  .user-avatar {
    width: 26px;
    height: 26px;
  }

  .dropdown-arrow {
    display: none;
  }
}
</style>
