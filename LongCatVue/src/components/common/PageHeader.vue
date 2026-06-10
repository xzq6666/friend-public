<template>
  <div class="page-header" :class="{ 'has-border': border }">
    <div class="header-content">
      <div class="header-main">
        <div v-if="icon" class="header-icon">
          <slot name="icon">
            <el-icon :size="24"><component :is="icon" /></el-icon>
          </slot>
        </div>
        <div class="header-text">
          <h2 class="header-title">{{ title }}</h2>
          <p v-if="description || $slots.description" class="header-desc">
            <slot name="description">{{ description }}</slot>
          </p>
        </div>
      </div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.extra" class="header-extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  icon: { type: [String, Object], default: null },
  border: { type: Boolean, default: true }
})
</script>

<style scoped>
.page-header {
  margin-bottom: var(--space-6);
}
.page-header.has-border {
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--gray-100);
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
}
.header-main {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  flex: 1;
}
.header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--gray-50);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-600);
  flex-shrink: 0;
}
.header-text {
  flex: 1;
}
.header-title {
  margin: 0;
  font-size: var(--text-xl);
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
  line-height: 1.3;
}
.header-desc {
  margin: var(--space-1) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  line-height: 1.5;
}
.header-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}
.header-extra {
  margin-top: var(--space-4);
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
  }
  .header-actions {
    width: 100%;
  }
  .header-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
