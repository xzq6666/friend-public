<template>
  <div class="filter-panel" :class="{ collapsed: isCollapsed }">
    <div class="filter-content">
      <slot />
    </div>
    <div v-if="collapsible" class="filter-toggle" @click="isCollapsed = !isCollapsed">
      <el-icon :size="14">
        <ArrowUp v-if="!isCollapsed" />
        <ArrowDown v-else />
      </el-icon>
      <span>{{ isCollapsed ? '展开筛选' : '收起筛选' }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

defineProps({
  collapsible: { type: Boolean, default: false }
})

const isCollapsed = ref(false)
</script>

<style scoped>
.filter-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  margin-bottom: var(--space-5);
  transition: all var(--duration-normal) var(--ease-out);
}
.filter-content {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
}
.filter-content :deep(.el-input),
.filter-content :deep(.el-select),
.filter-content :deep(.el-cascader) {
  width: 180px;
}
.filter-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
  font-size: var(--text-xs);
  color: var(--gray-500);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out);
}
.filter-toggle:hover {
  color: var(--gray-700);
}
.collapsed .filter-content {
  display: none;
}
</style>
