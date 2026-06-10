<template>
  <div
    class="resume-dropzone"
    :class="{
      'is-dragging': isDragging,
      'is-success': isSuccess,
      'is-error': isError,
      'is-processing': isProcessing
    }"
    @dragover="onDragOver"
    @dragleave="onDragLeave"
    @drop="onDrop"
    @click="triggerFileSelect"
  >
    <!-- 隐藏的文件输入 -->
    <input
      id="resume-file-input"
      type="file"
      :accept="acceptedExtensions.join(',')"
      class="file-input"
      @change="onFileSelect"
    />

    <!-- 上传中 / 解析中状态 -->
    <div v-if="isProcessing" class="dropzone-processing">
      <div class="processing-icon">
        <div class="spinner"></div>
      </div>
      <p class="processing-message">{{ progress.message }}</p>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress.percent + '%' }"></div>
      </div>
      <span class="progress-text">{{ progress.percent }}%</span>
    </div>

    <!-- 成功状态 -->
    <div v-else-if="isSuccess" class="dropzone-success">
      <div class="success-icon">
        <el-icon :size="40" color="#3ea15d"><CircleCheckFilled /></el-icon>
      </div>
      <p class="success-title">简历上传成功</p>
      <p class="success-file">{{ fileName }}</p>
      <el-button type="primary" link size="small" @click.stop="reset">
        重新上传
      </el-button>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="isError" class="dropzone-error">
      <div class="error-icon">
        <el-icon :size="40" color="#c03939"><CircleCloseFilled /></el-icon>
      </div>
      <p class="error-title">上传失败</p>
      <p class="error-message">{{ progress.message }}</p>
      <el-button type="primary" link size="small" @click.stop="reset">
        重试
      </el-button>
    </div>

    <!-- 默认状态 -->
    <div v-else class="dropzone-content">
      <div class="upload-icon">
        <el-icon :size="48" color="#9e9890"><UploadFilled /></el-icon>
      </div>
      <p class="upload-text">
        <span class="upload-main">将简历文件拖到此处</span>
        <span class="upload-divider">或</span>
        <span class="upload-link">点击选择文件</span>
      </p>
      <p class="upload-hint">
        支持 PDF、DOC、DOCX 格式，不超过 {{ maxSizeText }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { UploadFilled, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const props = defineProps({
  isDragging: { type: Boolean, default: false },
  isProcessing: { type: Boolean, default: false },
  isSuccess: { type: Boolean, default: false },
  isError: { type: Boolean, default: false },
  progress: { type: Object, default: () => ({ phase: 'idle', percent: 0, message: '' }) },
  fileName: { type: String, default: '' },
  maxFileSize: { type: Number, default: 10 * 1024 * 1024 },
  acceptedExtensions: { type: Array, default: () => ['.pdf', '.doc', '.docx'] }
})

const emit = defineEmits(['dragover', 'dragleave', 'drop', 'fileSelect', 'reset'])

const maxSizeText = computed(() => {
  const mb = props.maxFileSize / (1024 * 1024)
  return mb >= 1 ? mb + 'MB' : props.maxFileSize / 1024 + 'KB'
})

const onDragOver = (e) => emit('dragover', e)
const onDragLeave = (e) => emit('dragleave', e)
const onDrop = (e) => emit('drop', e)
const onFileSelect = (e) => emit('fileSelect', e)
const reset = () => emit('reset')
</script>

<style scoped>
.resume-dropzone {
  border: 2px dashed var(--gray-300);
  border-radius: var(--radius-lg);
  padding: var(--space-8) var(--space-6);
  text-align: center;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  background: var(--gray-50);
  position: relative;
  overflow: hidden;
}

.resume-dropzone:hover {
  border-color: var(--primary-400);
  background: var(--primary-50);
}

.resume-dropzone.is-dragging {
  border-color: var(--primary-500);
  background: var(--primary-50);
  box-shadow: 0 0 0 3px rgba(78, 100, 150, 0.15);
}

.resume-dropzone.is-processing {
  border-color: var(--primary-400);
  background: var(--color-surface);
  cursor: default;
}

.resume-dropzone.is-success {
  border-color: var(--success-500);
  background: var(--success-50);
}

.resume-dropzone.is-error {
  border-color: var(--danger-400);
  background: var(--danger-50);
}

/* 隐藏文件输入 */
.file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}

/* ── 默认状态 ─────────────────────────────── */
.dropzone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.upload-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-surface);
  margin-bottom: var(--space-1);
  box-shadow: var(--shadow-sm);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: var(--text-base);
}

.upload-main {
  color: var(--gray-700);
  font-weight: var(--weight-medium);
}

.upload-divider {
  color: var(--gray-400);
  font-size: var(--text-sm);
}

.upload-link {
  color: var(--primary-500);
  font-weight: var(--weight-medium);
}

.upload-hint {
  font-size: var(--text-xs);
  color: var(--gray-400);
  margin: 0;
}

/* ── 处理中状态 ───────────────────────────── */
.dropzone-processing {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.processing-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--gray-200);
  border-top-color: var(--primary-500);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.processing-message {
  font-size: var(--text-sm);
  color: var(--gray-600);
  margin: 0;
}

.progress-bar {
  width: 80%;
  max-width: 280px;
  height: 6px;
  background: var(--gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary-500);
  border-radius: var(--radius-full);
  transition: width var(--duration-normal) var(--ease-out);
}

.progress-text {
  font-size: var(--text-xs);
  color: var(--gray-500);
  font-weight: var(--weight-medium);
}

/* ── 成功状态 ─────────────────────────────── */
.dropzone-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(62, 161, 93, 0.1);
}

.success-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--success-600);
  margin: 0;
}

.success-file {
  font-size: var(--text-sm);
  color: var(--gray-600);
  margin: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 错误状态 ─────────────────────────────── */
.dropzone-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.error-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(192, 57, 57, 0.1);
}

.error-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--danger-500);
  margin: 0;
}

.error-message {
  font-size: var(--text-sm);
  color: var(--gray-600);
  margin: 0;
  max-width: 280px;
}
</style>
