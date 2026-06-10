<template>
<div class="announcement-card">
  <div class="card-header">
    <div class="header-bg"></div>
    <div class="header-content">
      <div class="header-left">
        <div class="header-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="header-text">
          <span class="header-title">系统公告</span>
          <span class="header-count" v-if="announcements.length">共 {{ announcements.length }} 条</span>
        </div>
      </div>
      <div class="header-badge" v-if="recentCount > 0">
        <span class="badge-dot"></span>
        {{ recentCount }} 条最新
      </div>
    </div>
  </div>

  <!-- 公告列表 -->
  <div v-loading="loading" class="announce-list">
    <template v-if="announcements.length > 0">
      <div
        v-for="(item, index) in announcements"
        :key="item.id"
        class="announce-item"
        :class="{ 'is-recent': isRecent(item.createTime) }"
        :style="{ animationDelay: index * 0.06 + 's' }"
      >
        <div class="item-left">
          <div class="item-dot" :class="dotClass(item.targetType)"></div>
          <div v-if="index < announcements.length - 1" class="item-line"></div>
        </div>
        <div class="item-card">
          <div class="item-header">
            <span class="item-title">{{ item.title }}</span>
            <span v-if="isRecent(item.createTime)" class="new-badge">NEW</span>
          </div>
          <div v-if="item.content" class="item-content">{{ item.content }}</div>
          <div class="item-footer">
            <el-tag :type="tagType(item.targetType)" size="small" class="item-tag" effect="plain">
              {{ targetLabel(item.targetType) }}
            </el-tag>
            <span class="item-time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(item.createTime) }}
            </span>
          </div>
        </div>
      </div>
    </template>
    <div v-else class="announce-empty">
      <div class="empty-illustration">
        <div class="empty-circle"></div>
        <el-icon :size="28"><Bell /></el-icon>
      </div>
      <span class="empty-text">暂无公告</span>
      <span class="empty-sub">新公告发布后会在这里显示</span>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bell, Clock } from '@element-plus/icons-vue'
import request from '../../utils/request'

const loading = ref(false)
const announcements = ref([])

const recentCount = computed(() => announcements.value.filter(a => isRecent(a.createTime)).length)

const isRecent = (t) => {
  if (!t) return false
  return Date.now() - new Date(t).getTime() < 86400000
}

const targetLabel = (type) => ({ ALL: '全部用户', EMPLOYEE: '求职者', EMPLOYER: '企业用户' }[type] || type)
const tagType = (type) => ({ ALL: '', EMPLOYEE: 'success', EMPLOYER: 'warning' }[type] || 'info')
const dotClass = (type) => ({ ALL: 'dot-all', EMPLOYEE: 'dot-employee', EMPLOYER: 'dot-employer' }[type] || 'dot-all')

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await request.get('/announcement/latest', { skipGlobalLoading: true })
    announcements.value = Array.isArray(res) ? res : []
  } catch {
    announcements.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchAnnouncements() })
</script>

<style scoped>
.announcement-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* ── 头部 ──────────────────────────────────── */
.card-header {
  position: relative;
  padding: var(--space-4) var(--space-5);
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--gray-900) 0%, var(--gray-700) 100%);
  opacity: 0.03;
}

.header-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.header-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--gray-900);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  letter-spacing: 0.02em;
}

.header-count {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 2px;
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: var(--weight-medium);
  color: var(--primary-600);
  background: var(--primary-50);
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-500);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* ── 列表 ──────────────────────────────────── */
.announce-list {
  padding: var(--space-2) var(--space-5) var(--space-4);
  max-height: 420px;
  overflow-y: auto;
}

.announce-list::-webkit-scrollbar { width: 4px; }
.announce-list::-webkit-scrollbar-thumb { background: var(--gray-200); border-radius: 4px; }

.announce-item {
  display: flex;
  gap: var(--space-3);
  animation: slideIn 0.35s ease both;
}

.announce-item.is-recent .item-card {
  border-left-color: var(--primary-400);
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── 左侧时间线 ──────────────────────────── */
.item-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 16px;
  flex-shrink: 0;
  width: 12px;
}

.item-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid var(--color-surface);
  box-shadow: 0 0 0 2px var(--gray-200);
  z-index: 1;
}
.dot-all { background: var(--primary-500); box-shadow: 0 0 0 2px var(--primary-200); }
.dot-employee { background: var(--success-500); box-shadow: 0 0 0 2px var(--success-200); }
.dot-employer { background: var(--warning-500); box-shadow: 0 0 0 2px var(--warning-200); }

.item-line {
  width: 2px;
  flex: 1;
  background: var(--gray-100);
  margin-top: 4px;
}

/* ── 卡片 ──────────────────────────────────── */
.item-card {
  flex: 1;
  min-width: 0;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--gray-100);
  border-left: 3px solid var(--gray-200);
  background: var(--color-surface);
  transition: all var(--duration-fast) var(--ease-out);
}

.item-card:hover {
  border-color: var(--gray-200);
  box-shadow: var(--shadow-sm);
  transform: translateX(2px);
}

.item-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 6px;
}

.item-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.new-badge {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: var(--weight-bold);
  color: var(--primary-600);
  background: var(--primary-50);
  padding: 1px 6px;
  border-radius: var(--radius-full);
  letter-spacing: 0.05em;
}

.item-content {
  font-size: 12px;
  color: var(--gray-500);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-tag {
  font-size: 10px !important;
  height: 20px !important;
  padding: 0 6px !important;
}

.item-time {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--gray-400);
}
.item-time .el-icon { font-size: 12px; }

/* ── 空状态 ────────────────────────────────── */
.announce-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) 0 var(--space-6);
}

.empty-illustration {
  position: relative;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-1);
  color: var(--gray-300);
}

.empty-circle {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: var(--gray-50);
  border: 2px dashed var(--gray-100);
}

.empty-text {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-400);
}

.empty-sub {
  font-size: 12px;
  color: var(--gray-300);
}
</style>
