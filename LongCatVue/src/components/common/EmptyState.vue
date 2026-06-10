<template>
  <div class="empty-state" :class="{ compact }">
    <div class="empty-icon">
      <slot name="icon">
        <el-icon :size="compact ? 40 : 56">
          <Document v-if="type === 'document'" />
          <Search v-else-if="type === 'search'" />
          <User v-else-if="type === 'user'" />
          <Briefcase v-else-if="type === 'briefcase'" />
          <ChatDotRound v-else-if="type === 'message'" />
          <CircleCheck v-else-if="type === 'success'" />
          <Warning v-else-if="type === 'warning'" />
          <Document v-else />
        </el-icon>
      </slot>
    </div>
    <p v-if="text" class="empty-text">{{ text }}</p>
    <p v-if="subtext" class="empty-subtext">{{ subtext }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { Document, Search, User, Briefcase, ChatDotRound, CircleCheck, Warning } from '@element-plus/icons-vue'

defineProps({
  text: { type: String, default: '暂无数据' },
  subtext: { type: String, default: '' },
  type: { type: String, default: 'document' },
  compact: { type: Boolean, default: false }
})
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: var(--space-8) var(--space-4);
  animation: fadeIn var(--duration-normal) var(--ease-out);
}
.empty-state.compact {
  padding: var(--space-5) var(--space-3);
}
.empty-icon {
  color: var(--gray-300);
  margin-bottom: var(--space-3);
  transition: transform var(--duration-normal) var(--ease-out);
}
.empty-state:hover .empty-icon {
  transform: scale(1.05);
}
.empty-text {
  color: var(--gray-500);
  font-size: var(--text-sm);
  margin: 0 0 var(--space-4);
  font-weight: var(--weight-medium);
}
.empty-subtext {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  margin: 0 0 var(--space-3);
}
.empty-actions {
  display: flex;
  gap: var(--space-2);
  justify-content: center;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
