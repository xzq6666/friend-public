<template>
<div class="content-card">
  <div class="card-title">数据概览</div>
  <div class="stats-grid">
    <StatCard
      v-for="(stat, index) in statItems"
      :key="index"
      :value="stat.value"
      :label="stat.label"
      :icon="stat.icon"
      :icon-color="stat.iconColor"
      :icon-bg="stat.iconBg"
      size="default"
    />
  </div>
</div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { StatCard } from '../../components/common'
import { Briefcase, Document, User, Promotion } from '@element-plus/icons-vue'

const data = inject('profileData')

const statItems = computed(() => [
  {
    value: data.stats.value.count1,
    label: data.getStatLabel(0),
    icon: Briefcase,
    iconColor: 'var(--primary-600)',
    iconBg: 'var(--primary-50)'
  },
  {
    value: data.stats.value.count2,
    label: data.getStatLabel(1),
    icon: Document,
    iconColor: 'var(--success-600)',
    iconBg: 'var(--success-50)'
  },
  {
    value: data.stats.value.count3,
    label: data.getStatLabel(2),
    icon: User,
    iconColor: 'var(--warning-600)',
    iconBg: 'var(--warning-50)'
  },
  {
    value: data.stats.value.count4,
    label: data.getStatLabel(3),
    icon: Promotion,
    iconColor: 'var(--gray-700)',
    iconBg: 'var(--gray-50)'
  }
])
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
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}
@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
