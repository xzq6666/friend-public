<template>
  <div class="parser-status" :class="'phase-' + progress.phase">
    <!-- 步骤条 -->
    <div class="steps">
      <div
        v-for="(step, index) in steps"
        :key="step.key"
        class="step"
        :class="{
          'is-active': isStepActive(step.key),
          'is-done': isStepDone(step.key),
          'is-error': progress.phase === 'error' && isStepActive(step.key)
        }"
      >
        <div class="step-indicator">
          <el-icon v-if="isStepDone(step.key)" :size="14"><Check /></el-icon>
          <div v-else-if="isStepActive(step.key) && isProcessing" class="step-spinner"></div>
          <span v-else class="step-number">{{ index + 1 }}</span>
        </div>
        <span class="step-label">{{ step.label }}</span>
        <span v-if="isStepActive(step.key) && stepDescription" class="step-desc">
          {{ stepDescription }}
        </span>
      </div>
    </div>

    <!-- 进度条（仅在处理中显示） -->
    <div v-if="isProcessing" class="overall-progress">
      <div class="progress-track">
        <div class="progress-bar" :style="{ width: progress.percent + '%' }"></div>
      </div>
      <span class="progress-label">{{ progress.percent }}%</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  progress: {
    type: Object,
    default: () => ({ phase: 'idle', percent: 0, message: '' })
  }
})

const steps = [
  { key: 'uploading', label: '文件上传' },
  { key: 'parsing', label: '内容解析' },
  { key: 'analyzing', label: 'AI 分析' },
  { key: 'success', label: '完成' }
]

const isProcessing = computed(() =>
  ['uploading', 'parsing', 'analyzing'].includes(props.progress.phase)
)

const currentPhaseIndex = computed(() => {
  const idx = steps.findIndex(s => s.key === props.progress.phase)
  return idx >= 0 ? idx : 0
})

const isStepActive = (key) => props.progress.phase === key
const isStepDone = (key) => currentPhaseIndex.value > steps.findIndex(s => s.key === key)

const stepDescription = computed(() => {
  const msgMap = {
    uploading: '正在上传文件...',
    parsing: 'AI 正在解析简历内容...',
    analyzing: 'AI 正在分析竞争力...',
    success: '分析完成'
  }
  return msgMap[props.progress.phase] || ''
})
</script>

<style scoped>
.parser-status {
  padding: var(--space-4) 0;
}

/* ── 步骤条 ───────────────────────────────── */
.steps {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
  margin-bottom: var(--space-4);
}

.steps::before {
  content: '';
  position: absolute;
  top: 14px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: var(--gray-200);
  z-index: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  position: relative;
  z-index: 1;
  flex: 1;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gray-200);
  color: var(--gray-500);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  transition: all var(--duration-normal) var(--ease-out);
}

.step.is-active .step-indicator {
  background: var(--primary-500);
  color: var(--color-surface);
  box-shadow: 0 0 0 3px rgba(78, 100, 150, 0.2);
}

.step.is-done .step-indicator {
  background: var(--success-500);
  color: var(--color-surface);
}

.step.is-error .step-indicator {
  background: var(--danger-500);
  color: var(--color-surface);
  box-shadow: 0 0 0 3px rgba(192, 57, 57, 0.2);
}

.step-number {
  line-height: 1;
}

.step-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--color-surface);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.step-label {
  font-size: var(--text-xs);
  color: var(--gray-500);
  font-weight: var(--weight-medium);
  text-align: center;
  white-space: nowrap;
}

.step.is-active .step-label {
  color: var(--primary-600);
  font-weight: var(--weight-semibold);
}

.step.is-done .step-label {
  color: var(--success-600);
}

.step-desc {
  font-size: 10px;
  color: var(--gray-400);
  text-align: center;
}

/* ── 进度条 ───────────────────────────────── */
.overall-progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.progress-track {
  flex: 1;
  height: 4px;
  background: var(--gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: var(--primary-500);
  border-radius: var(--radius-full);
  transition: width var(--duration-normal) var(--ease-out);
}

.progress-label {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--primary-500);
  min-width: 30px;
  text-align: right;
}
</style>
