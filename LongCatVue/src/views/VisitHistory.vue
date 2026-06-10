<template>
<div class="page-container-sm page-enter">
  <div class="page-header">
    <h2>访问记录</h2>
    <el-button text @click="showPrivacyDialog = true">
      <el-icon><Setting /></el-icon> 隐私设置
    </el-button>
  </div>

  <!-- 统计卡片 -->
  <div class="stats-row">
    <div class="stat-card">
      <div class="stat-value">{{ stats.todayCount || 0 }}</div>
      <div class="stat-label">今日访问</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ stats.weekCount || 0 }}</div>
      <div class="stat-label">本周访问</div>
    </div>
    <div class="stat-card">
      <div class="stat-value">{{ stats.totalCount || 0 }}</div>
      <div class="stat-label">总访问量</div>
    </div>
  </div>

  <!-- 筛选标签 -->
  <div class="filter-tabs">
    <el-radio-group v-model="activeTab" @change="fetchVisitors">
      <el-radio-button :value="null">全部</el-radio-button>
      <el-radio-button v-if="isEmployee" :value="1">简历/主页</el-radio-button>
      <el-radio-button v-if="isEmployer" :value="2">职位访问</el-radio-button>
    </el-radio-group>
  </div>

  <!-- 访问记录列表 -->
  <div class="visitors-card" v-loading="loading">
    <el-empty v-if="!loading && visitors.length === 0" description="暂无访问记录" />
    <div v-else class="visitor-list">
      <div v-for="item in visitors" :key="item.id" class="visitor-item" :class="{ clickable: item.visitor_id && item.visitor_username !== '匿名用户' }" @click="goProfile(item)">
        <div class="visitor-avatar">
          <span class="avatar-placeholder">{{ (item.visitor_username || '匿').charAt(0).toUpperCase() }}</span>
        </div>
        <div class="visitor-info">
          <div class="visitor-name">
            {{ item.visitor_username || '匿名用户' }}
            <el-tag v-if="item.visitor_user_type === 'EMPLOYER'" size="small" type="warning">企业</el-tag>
            <el-tag v-else-if="item.visitor_user_type === 'EMPLOYEE'" size="small" type="info">求职者</el-tag>
          </div>
          <div class="visitor-meta">
            <span v-if="item.resume_name">查看了简历「{{ item.resume_name }}」</span>
            <span v-else-if="item.job_title">查看了职位「{{ item.job_title }}」</span>
          </div>
        </div>
        <span class="visitor-time">{{ formatTime(item.create_time) }}</span>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchVisitors"
      />
    </div>
  </div>

  <!-- 隐私设置对话框 -->
  <el-dialog v-model="showPrivacyDialog" title="隐私设置" width="420px">
    <div class="privacy-setting-item">
      <div class="setting-text">
        <div class="setting-title">默认匿名访问</div>
        <div class="setting-desc">开启后，你浏览他人简历/职位时对方将看到"匿名用户"</div>
      </div>
      <el-switch v-model="privacySettings.defaultAnonymous" @change="savePrivacy" />
    </div>
    <div class="privacy-setting-item">
      <div class="setting-text">
        <div class="setting-title">显示访问记录</div>
        <div class="setting-desc">关闭后，你将不再收到谁访问了你的提醒</div>
      </div>
      <el-switch v-model="privacySettings.showVisitHistory" @change="savePrivacy" />
    </div>
    <template #footer>
      <el-button @click="showPrivacyDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import request from '../utils/request'

const userStore = useUserStore()
const router = useRouter()

const goProfile = async (item) => {
  if (!item.visitor_id || item.visitor_username === '匿名用户') return
  if (item.visitor_user_type === 'EMPLOYER') {
    router.push(`/company/${item.visitor_id}`)
  } else if (item.visitor_user_type === 'EMPLOYEE') {
    try {
      const resume = await request.get(`/resume/user/${item.visitor_id}`, { skipErrorNotification: true })
      if (resume?.id) {
        router.push(`/r/resume/${resume.id}`)
      } else {
        ElMessage.info('该用户暂无简历')
      }
    } catch {
      ElMessage.info('该用户暂无简历')
    }
  }
}
const isEmployee = computed(() => userStore.user?.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')

// 统计
const stats = ref({ todayCount: 0, weekCount: 0, totalCount: 0 })

// 访问记录
const visitors = ref([])
const loading = ref(false)
const activeTab = ref(null)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

// 隐私设置
const showPrivacyDialog = ref(false)
const privacySettings = ref({ defaultAnonymous: false, showVisitHistory: true })

const fetchStats = async () => {
  try {
    const targetType = isEmployee.value ? 1 : isEmployer.value ? 2 : undefined
    const res = await request.get('/visit-history/stats', { params: { targetType }, skipGlobalLoading: true })
    stats.value = res || { todayCount: 0, weekCount: 0, totalCount: 0 }
  } catch { /* ignore */ }
}

const fetchVisitors = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      ...(activeTab.value != null ? { targetType: activeTab.value } : {})
    }
    const res = await request.get('/visit-history/visitors', { params, skipGlobalLoading: true })
    visitors.value = res.records || []
    total.value = res.total || 0
  } catch {
    visitors.value = []
  } finally {
    loading.value = false
  }
}

const fetchPrivacy = async () => {
  try {
    const res = await request.get('/visit-history/privacy', { skipGlobalLoading: true })
    privacySettings.value = {
      defaultAnonymous: res.defaultAnonymous ?? false,
      showVisitHistory: res.showVisitHistory ?? true
    }
  } catch { /* ignore */ }
}

const savePrivacy = async () => {
  try {
    await request.put('/visit-history/privacy', {
      defaultAnonymous: privacySettings.value.defaultAnonymous,
      showVisitHistory: privacySettings.value.showVisitHistory
    })
    ElMessage.success('设置已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return d.toLocaleDateString('zh-CN')
}

onMounted(() => {
  // 默认标签
  activeTab.value = isEmployee.value ? 1 : isEmployer.value ? 2 : null
  fetchStats()
  fetchVisitors()
  fetchPrivacy()
})
</script>

<style scoped>
/* ── 统计卡片 ──────────────────────────────── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 28px;
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin-bottom: var(--space-1);
}

/* ── 筛选标签 ──────────────────────────────── */
.filter-tabs {
  margin-bottom: var(--space-4);
}

/* ── 访问记录列表 ──────────────────────────── */
.visitors-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.visitor-list {
  display: flex;
  flex-direction: column;
}

.visitor-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--gray-100);
  transition: background var(--duration-fast);
}
.visitor-item:last-child { border-bottom: none; }
.visitor-item.clickable { cursor: pointer; }
.visitor-item.clickable:hover { background: var(--gray-50); }
.visitor-item.clickable:hover .visitor-name { color: var(--primary-600); }

.visitor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--primary-500);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-placeholder {
  font-size: 16px;
  font-weight: var(--weight-semibold);
  color: #fff;
}

.visitor-info {
  flex: 1;
  min-width: 0;
}
.visitor-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.visitor-meta {
  font-size: var(--text-xs);
  color: var(--gray-500);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visitor-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
  white-space: nowrap;
  flex-shrink: 0;
}

.pagination {
  margin-top: var(--space-4);
  display: flex;
  justify-content: center;
}

/* ── 隐私设置 ──────────────────────────────── */
.privacy-setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) 0;
}
.privacy-setting-item + .privacy-setting-item {
  border-top: 1px solid var(--gray-100);
}
.setting-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
  margin-bottom: 2px;
}
.setting-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
}

@media (max-width: 640px) {
  .stats-row { grid-template-columns: 1fr; }
  .visitor-item { flex-wrap: wrap; }
  .visitor-time { margin-left: auto; }
}
</style>
