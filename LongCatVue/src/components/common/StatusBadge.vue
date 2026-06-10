<template>
  <div class="status-badge" :class="[`status-${status}`, { dot: showDot }]">
    <span v-if="showDot" class="status-dot" />
    <slot>{{ text || defaultText }}</slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: [Number, String], required: true },
  text: { type: String, default: '' },
  showDot: { type: Boolean, default: true },
  type: { type: String, default: 'application' }
})

const statusMap = {
  application: {
    0: { text: '待处理', color: 'warning' },
    1: { text: '已查看', color: 'info' },
    2: { text: '面试', color: 'success' },
    3: { text: '已录用', color: 'success' },
    4: { text: '已拒绝', color: 'danger' }
  },
  interview: {
    0: { text: '待确认', color: 'warning' },
    1: { text: '已确认', color: 'success' },
    2: { text: '已取消', color: 'danger' },
    3: { text: '已完成', color: 'info' }
  },
  job: {
    0: { text: '已下线', color: 'inactive' },
    1: { text: '招聘中', color: 'active' },
    2: { text: '已暂停', color: 'pending' }
  },
  user: {
    0: { text: '禁用', color: 'inactive' },
    1: { text: '正常', color: 'active' }
  }
}

const defaultText = computed(() => {
  const map = statusMap[props.type] || statusMap.application
  return map[props.status]?.text || '未知'
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

/* 状态颜色 */
.status-warning { color: var(--warning-600); }
.status-warning .status-dot { background: var(--warning-500); }

.status-success { color: var(--success-600); }
.status-success .status-dot { background: var(--success-500); }

.status-danger { color: var(--danger-600); }
.status-danger .status-dot { background: var(--danger-500); }

.status-info { color: var(--primary-600); }
.status-info .status-dot { background: var(--primary-500); }

.status-active { color: var(--success-600); }
.status-active .status-dot { background: var(--success-500); box-shadow: 0 0 0 3px var(--success-100); }

.status-pending { color: var(--warning-600); }
.status-pending .status-dot { background: var(--warning-500); }

.status-inactive { color: var(--gray-500); }
.status-inactive .status-dot { background: var(--gray-400); }
</style>
