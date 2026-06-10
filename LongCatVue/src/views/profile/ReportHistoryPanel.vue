<template>
<div class="report-panel" @click="$router.push('/report-history')">
  <div class="panel-header">
    <div class="panel-icon">
      <el-icon><WarnTriangleFilled /></el-icon>
    </div>
    <div class="panel-text">
      <span class="panel-title">举报历史</span>
      <span class="panel-desc">查看我的举报记录和处理进度</span>
    </div>
    <el-icon class="panel-arrow"><ArrowRight /></el-icon>
  </div>
  <div class="panel-stats" v-if="stats.total > 0">
    <div class="stat-item">
      <span class="stat-num">{{ stats.pending || 0 }}</span>
      <span class="stat-label">待处理</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-num">{{ stats.handled || 0 }}</span>
      <span class="stat-label">已处理</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-num">{{ stats.total || 0 }}</span>
      <span class="stat-label">总计</span>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { WarnTriangleFilled, ArrowRight } from '@element-plus/icons-vue'
import request from '../../utils/request'

const stats = ref({ pending: 0, handled: 0, total: 0 })

onMounted(async () => {
  try {
    const res = await request.get('/report/my', { params: { page: 1, size: 1 }, skipGlobalLoading: true })
    const total = res.total || 0
    // 统计各状态数量（用一次请求的 total 作为总数，再分别查待处理和已处理）
    const [pendingRes, handledRes] = await Promise.all([
      request.get('/report/my', { params: { page: 1, size: 1, status: 0 }, skipGlobalLoading: true }).catch(() => ({ total: 0 })),
      request.get('/report/my', { params: { page: 1, size: 1, status: 2 }, skipGlobalLoading: true }).catch(() => ({ total: 0 }))
    ])
    stats.value = {
      pending: pendingRes.total || 0,
      handled: handledRes.total || 0,
      total
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.report-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.report-panel:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.panel-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.panel-text {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.panel-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}
.panel-desc {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 1px;
}

.panel-arrow {
  color: var(--gray-400);
  font-size: 14px;
}

.panel-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}
.stat-num {
  font-size: var(--text-base);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
}
.stat-label {
  font-size: 10px;
  color: var(--gray-400);
}

.stat-divider {
  width: 1px;
  height: 24px;
  background: var(--gray-200);
}
</style>
