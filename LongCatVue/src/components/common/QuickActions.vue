<template>
  <div class="quick-actions">
    <button
      v-for="action in actions"
      :key="action.key"
      class="action-btn"
      :class="{ disabled: action.disabled }"
      :disabled="action.disabled"
      @click="handleClick(action)"
    >
      <span class="action-icon" :style="action.iconStyle">
        <el-icon :size="18"><component :is="action.icon" /></el-icon>
      </span>
      <span class="action-label">{{ action.label }}</span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  actions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['click'])

const handleClick = (action) => {
  if (!action.disabled) {
    emit('click', action)
  }
}
</script>

<style scoped>
.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--space-3);
}
.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.action-btn:hover:not(.disabled) {
  background: var(--color-surface);
  border-color: var(--gray-300);
  color: var(--gray-900);
  box-shadow: var(--shadow-sm);
}
.action-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.action-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-500);
  transition: all var(--duration-fast) var(--ease-out);
}
.action-btn:hover:not(.disabled) .action-icon {
  background: var(--gray-200);
  color: var(--gray-700);
}
.action-label {
  font-size: var(--text-xs);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}
.action-btn:hover:not(.disabled) .action-label {
  color: var(--gray-800);
}
</style>
