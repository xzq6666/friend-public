<template>
  <div class="notification-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <h3 class="panel-title">消息通知</h3>
      <el-button
        v-if="unreadCount > 0"
        type="primary"
        link
        size="small"
        @click="$emit('read-all')"
      >
        全部已读
      </el-button>
    </div>

    <!-- 内容区 -->
    <div class="panel-body">
      <!-- 加载中 -->
      <div v-if="loading && !notifications.length" class="panel-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!notifications.length" class="panel-empty">
        <el-icon :size="40" color="var(--gray-300)"><Bell /></el-icon>
        <p>暂无通知消息</p>
      </div>

      <!-- 通知列表 -->
      <el-scrollbar v-else max-height="360px" class="panel-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.isRead }"
          @click="handleClick(item)"
        >
          <div class="item-icon" :style="{ background: typeConfig[item.type]?.color || '#909399' }">
            <el-icon :size="16" color="#fff">
              <component :is="typeConfig[item.type]?.icon || 'InfoFilled'" />
            </el-icon>
          </div>
          <div class="item-content">
            <div class="item-header">
              <span class="item-title">{{ item.title || '系统通知' }}</span>
              <span class="item-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <p class="item-desc">{{ getDisplayContent(item) }}</p>
          </div>
          <div v-if="!item.isRead" class="item-dot"></div>
          <el-icon
            class="item-delete"
            :size="14"
            @click.stop="$emit('delete', item.id)"
          >
            <Close />
          </el-icon>
        </div>
      </el-scrollbar>
    </div>

    <!-- 底部 -->
    <div class="panel-footer">
      <el-button type="primary" link size="small" @click="$emit('close')">
        关闭
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  Bell,
  VideoCamera,
  Document,
  Star,
  InfoFilled,
  Loading,
  ChatDotRound,
  Close
} from '@element-plus/icons-vue'

const props = defineProps({
  notifications: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['read', 'read-all', 'close', 'delete'])

const router = useRouter()
const userStore = useUserStore()

const unreadCount = computed(() => props.notifications.filter(n => !n.isRead).length)

// 根据用户角色返回对应路由
const getRoute = (type, item) => {
  const userType = userStore.user?.userType
  const isEmployer = userType === 'EMPLOYER'
  const isAdmin = userType === 'ADMIN'
  if (type === 'interview') return isEmployer ? '/my-manage-interviews' : '/my-interviews'
  if (type === 'job_match') {
    // 订阅匹配通知 → 跳转到订阅管理页
    if (item?.title?.includes('订阅提醒')) return '/job-subscription'
    // 职位下架通知等 → 跳转到投递/候选人页
    return isEmployer ? '/my-candidates' : '/my-applications'
  }
  if (type === 'chat') {
    const match = item?.content?.match(/interviewId:(\d+)/)
    return match ? `/interview-chat/${match[1]}` : '/chat-history'
  }
  if (type === 'announcement') return '/profile'
  if (type === 'system') {
    // 论坛通知：解析 jobId 跳转到论坛页面
    const forumMatch = item?.content?.match(/jobId:(\d+)/)
    if (forumMatch) return `/forum/${forumMatch[1]}`
    // 举报通知
    if (item?.title?.includes('举报')) {
      if (isAdmin) return '/admin/report-manage'
      // 举报人收到处理结果 → 跳转到个人中心
      return '/profile'
    }
    if (isAdmin) return '/admin/company-verify'
    if (isEmployer) return '/company-profile'
    return null
  }
  return null
}

// 通知类型配置
const typeConfig = {
  interview: { icon: 'VideoCamera', color: '#409EFF', route: 'interview' },
  job_match: { icon: 'Star', color: '#E6A23C', route: 'job_match' },
  chat: { icon: 'ChatDotRound', color: '#67C23A', route: 'chat' },
  announcement: { icon: 'Bell', color: '#F56C6C', route: 'announcement' },
  system: { icon: 'InfoFilled', color: '#909399', route: null }
}

// 获取通知显示内容（去掉内部标记）
const getDisplayContent = (item) => {
  if (item.content) {
    return item.content.replace(/\n(?:interviewId|jobId|reportId):\d+$/gm, '')
  }
  return item.content
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString('zh-CN')
}

// 点击通知
const handleClick = (item) => {
  if (!item.isRead) {
    emit('read', item.id)
  }
  const route = getRoute(item.type, item)
  if (route) {
    router.push(route)
    emit('close')
  }
}
</script>

<style scoped>
.notification-panel {
  display: flex;
  flex-direction: column;
}

/* ── 头部 ──────────────────────────────────── */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-100);
}

.panel-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-900);
}

/* ── 内容区 ────────────────────────────────── */
.panel-body {
  min-height: 120px;
  max-height: 400px;
}

.panel-loading,
.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--gray-400);
  font-size: 13px;
  gap: 12px;
}

.panel-empty p {
  margin: 0;
}

/* ── 通知列表 ──────────────────────────────── */
.panel-list {
  padding: 4px 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
  position: relative;
}

.notification-item:hover {
  background: var(--gray-50);
}

.notification-item.unread {
  background: rgba(64, 158, 255, 0.03);
}

.notification-item.unread:hover {
  background: rgba(64, 158, 255, 0.06);
}

.item-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--gray-800);
}

.item-time {
  font-size: 11px;
  color: var(--gray-400);
  flex-shrink: 0;
  margin-left: 8px;
}

.item-desc {
  margin: 0;
  font-size: 12px;
  color: var(--gray-500);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--score-good);
  margin-top: 6px;
}

.item-delete {
  flex-shrink: 0;
  color: var(--gray-300);
  cursor: pointer;
  opacity: 0;
  transition: all var(--duration-fast) var(--ease-out);
  margin-top: 2px;
}

.notification-item:hover .item-delete {
  opacity: 1;
}

.item-delete:hover {
  color: var(--danger-500);
  transform: scale(1.2);
}

/* ── 底部 ──────────────────────────────────── */
.panel-footer {
  display: flex;
  justify-content: center;
  padding: 8px 16px;
  border-top: 1px solid var(--gray-100);
}
</style>
