<template>
<div class="content-card">
  <div class="card-title">
    {{ title }}
    <router-link :to="viewAllLink" class="view-all">查看全部</router-link>
  </div>
  <div v-loading="data.activitiesLoading.value" class="activity-list">
    <template v-if="data.recentActivities.value.length">
      <div
        v-for="item in data.recentActivities.value"
        :key="item.id"
        class="activity-item"
      >
        <div class="activity-icon" :class="item.type">
          <el-icon>
            <Document v-if="item.type === 'application'" />
            <Briefcase v-else />
          </el-icon>
        </div>
        <div class="activity-body">
          <div class="activity-title">{{ item.title }}</div>
          <div class="activity-meta">{{ item.subtitle }} · {{ data.formatTime(item.time) }}</div>
        </div>
        <el-tag v-if="item.status" :type="getStatusType(item.status)" size="small" class="activity-status">
          {{ getStatusLabel(item.status) }}
        </el-tag>
      </div>
    </template>
    <EmptyState v-else type="document" :text="emptyText" compact />
  </div>
</div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { EmptyState } from '../../components/common'
import { Document, Briefcase } from '@element-plus/icons-vue'

const data = inject('profileData')

const title = computed(() => {
  const type = data.user.value?.userType
  if (type === 'EMPLOYEE') return '最近投递'
  if (type === 'EMPLOYER') return '最近发布的职位'
  return '最近动态'
})

const viewAllLink = computed(() => {
  const type = data.user.value?.userType
  if (type === 'EMPLOYEE') return '/my-applications'
  if (type === 'EMPLOYER') return '/my-jobs'
  return '/admin/jobs'
})

const emptyText = computed(() => {
  const type = data.user.value?.userType
  if (type === 'EMPLOYEE') return '还没有投递记录，去看看有哪些职位吧'
  if (type === 'EMPLOYER') return '还没有发布过职位'
  return '暂无动态'
})

const statusMap = {
  PENDING: { label: '待处理', type: 'warning' },
  REVIEWING: { label: '审核中', type: '' },
  ACCEPTED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  INTERVIEW: { label: '面试中', type: '' },
  OPEN: { label: '招聘中', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' },
  // 兼容后端数字状态
  0: { label: '待处理', type: 'warning' },
  1: { label: '已查看', type: '' },
  2: { label: '邀请面试', type: '' },
  3: { label: '已录用', type: 'success' },
  4: { label: '已拒绝', type: 'danger' },
}

const getStatusLabel = (status) => statusMap[status]?.label || (typeof status === 'number' ? '未知' : status)
const getStatusType = (status) => statusMap[status]?.type || 'info'
</script>

<style scoped>
.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}
.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.view-all {
  font-size: var(--text-sm);
  font-weight: var(--weight-normal);
  color: var(--gray-500);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}
.view-all:hover { color: var(--gray-900); }

.activity-list { display: flex; flex-direction: column; }
.activity-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--gray-50);
}
.activity-item:last-child { border-bottom: none; }

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: var(--text-base);
}
.activity-icon.application {
  background: var(--primary-50);
  color: var(--primary-600);
}
.activity-icon.job {
  background: var(--success-50);
  color: var(--success-600);
}

.activity-body { flex: 1; min-width: 0; }
.activity-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-meta {
  font-size: var(--text-xs);
  color: var(--gray-500);
  margin-top: 2px;
}
.activity-status { flex-shrink: 0; }
</style>
