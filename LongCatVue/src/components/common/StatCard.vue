<template>
  <div class="stat-card" :class="{ clickable, [`size-${size}`]: true }" @click="handleClick">
    <div class="stat-header">
      <div v-if="icon || $slots.icon" class="stat-icon" :style="iconStyle">
        <slot name="icon">
          <el-icon :size="iconSize"><component :is="icon" /></el-icon>
        </slot>
      </div>
      <div v-if="trend !== undefined" class="stat-trend" :class="trend > 0 ? 'up' : trend < 0 ? 'down' : ''">
        <el-icon>
          <CaretTop v-if="trend > 0" />
          <CaretBottom v-else-if="trend < 0" />
          <Minus v-else />
        </el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
    <div class="stat-content">
      <div class="stat-value">
        <span v-if="prefix" class="stat-prefix">{{ prefix }}</span>
        <span class="stat-number">{{ formattedValue }}</span>
        <span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
      </div>
      <div class="stat-label">{{ label }}</div>
    </div>
    <div v-if="$slots.footer" class="stat-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CaretTop, CaretBottom, Minus } from '@element-plus/icons-vue'

const props = defineProps({
  value: { type: [Number, String], required: true },
  label: { type: String, required: true },
  icon: { type: [String, Object], default: null },
  iconColor: { type: String, default: '' },
  iconBg: { type: String, default: '' },
  prefix: { type: String, default: '' },
  suffix: { type: String, default: '' },
  trend: { type: Number, default: undefined },
  clickable: { type: Boolean, default: false },
  size: { type: String, default: 'default' },
  decimals: { type: Number, default: 0 }
})

const emit = defineEmits(['click'])

const iconSize = computed(() => props.size === 'small' ? 18 : props.size === 'large' ? 28 : 22)

const iconStyle = computed(() => {
  const style = {}
  if (props.iconColor) style.color = props.iconColor
  if (props.iconBg) style.background = props.iconBg
  return style
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    if (props.value >= 10000) {
      return (props.value / 10000).toFixed(props.decimals) + 'w'
    }
    if (props.value >= 1000) {
      return (props.value / 1000).toFixed(props.decimals) + 'k'
    }
    return props.value.toLocaleString()
  }
  return props.value
})

const handleClick = () => {
  if (props.clickable) emit('click')
}
</script>

<style scoped>
.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: all var(--duration-normal) var(--ease-out);
}
.stat-card.clickable {
  cursor: pointer;
}
.stat-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--gray-300);
}

/* 尺寸变体 */
.stat-card.size-small {
  padding: var(--space-4);
}
.stat-card.size-large {
  padding: var(--space-6);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-3);
}
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--gray-50);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-600);
  flex-shrink: 0;
}
.size-small .stat-icon {
  width: 32px;
  height: 32px;
}
.size-large .stat-icon {
  width: 48px;
  height: 48px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  padding: 2px 6px;
  border-radius: var(--radius-full);
}
.stat-trend.up {
  color: var(--success-600);
  background: var(--success-50);
}
.stat-trend.down {
  color: var(--danger-600);
  background: var(--danger-50);
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
}
.stat-number {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  line-height: 1.2;
}
.size-small .stat-number {
  font-size: var(--text-xl);
}
.size-large .stat-number {
  font-size: var(--text-3xl);
}
.stat-prefix,
.stat-suffix {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}
.stat-footer {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}
</style>
