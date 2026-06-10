<template>
<div class="visit-panel" @click="$router.push('/visit-history')">
  <div class="panel-header">
    <div class="panel-icon">
      <el-icon><View /></el-icon>
    </div>
    <div class="panel-text">
      <span class="panel-title">访问记录</span>
      <span class="panel-desc">查看谁访问了你的{{ isEmployer ? '职位' : '简历' }}</span>
    </div>
    <el-icon class="panel-arrow"><ArrowRight /></el-icon>
  </div>
  <div class="panel-stats" v-if="stats.totalCount > 0">
    <div class="stat-item">
      <span class="stat-num">{{ stats.todayCount || 0 }}</span>
      <span class="stat-label">今日</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-num">{{ stats.weekCount || 0 }}</span>
      <span class="stat-label">本周</span>
    </div>
    <div class="stat-divider"></div>
    <div class="stat-item">
      <span class="stat-num">{{ stats.totalCount || 0 }}</span>
      <span class="stat-label">总计</span>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { View, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'

const userStore = useUserStore()
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')

const stats = ref({ todayCount: 0, weekCount: 0, totalCount: 0 })

onMounted(async () => {
  try {
    const targetType = isEmployer.value ? 2 : 1
    const res = await request.get('/visit-history/stats', { params: { targetType }, skipGlobalLoading: true })
    stats.value = res || { todayCount: 0, weekCount: 0, totalCount: 0 }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.visit-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.visit-panel:hover {
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
  background: var(--gray-100);
  color: var(--gray-600);
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
