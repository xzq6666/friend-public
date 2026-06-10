<template>
  <div class="page-container-lg">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>智能简历导入</h2>
        <p class="header-desc">上传简历文件，AI 将自动解析并生成完整简历</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/resume-manage')">
          <el-icon><List /></el-icon> 简历管理
        </el-button>
        <el-button @click="$router.push('/my-resume')">
          <el-icon><EditPen /></el-icon> 手动填写
        </el-button>
      </div>
    </div>

    <!-- 上传区域 -->
    <el-row :gutter="32">
      <el-col :span="14">
        <!-- 拖拽上传区 -->
        <ResumeDropzone
          :is-dragging="isDragging"
          :is-processing="isProcessing"
          :is-success="isSuccess"
          :is-error="isError"
          :progress="progress"
          :file-name="fileName"
          :file-size="fileSize"
          :max-file-size="MAX_FILE_SIZE"
          :accepted-extensions="ACCEPTED_EXTENSIONS"
          :format-file-size="formatFileSize"
          @dragover="onDragOver"
          @dragleave="onDragLeave"
          @drop="onDrop"
          @file-select="onFileSelect"
          @reset="reset"
        />

        <!-- 解析状态与结果 -->
        <ResumeParserStatus
          v-if="!isIdle"
          :progress="progress"
          :upload-result="uploadResult"
          :format-file-size="formatFileSize"
          @retry="retryWithLastFile"
          @clear="reset"
        />

        <!-- 提示信息 -->
        <div class="upload-tips">
          <h4>
            <el-icon><InfoFilled /></el-icon>
            导入说明
          </h4>
          <ul>
            <li>支持 PDF、DOC、DOCX 格式，文件大小不超过 10MB</li>
            <li>AI 将自动识别您的姓名、联系方式、技能、工作经历等信息</li>
            <li>导入后可继续编辑和完善简历内容</li>
            <li>AI 分析将在导入完成后自动生成</li>
          </ul>
        </div>
      </el-col>

      <el-col :span="10">
        <!-- 解析结果预览 -->
        <ResumePreviewCard
          v-if="isSuccess && parsedData"
          :data="parsedData"
          :has-ai-analysis="!!uploadResult?.aiAnalysis"
          @go-to-edit="goToEditResume"
          @analyze="goToEditWithAnalysis"
        />

        <!-- 功能介绍卡片（未上传时显示） -->
        <el-card v-else shadow="hover" class="feature-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">智能导入功能</span>
            </div>
          </template>
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon :size="20" color="#409eff"><Document /></el-icon>
              </div>
              <div class="feature-content">
                <h4>智能识别</h4>
                <p>AI 自动从简历文件中提取姓名、联系方式、技能、工作经历等关键信息</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon :size="20" color="#67c23a"><MagicStick /></el-icon>
              </div>
              <div class="feature-content">
                <h4>AI 分析</h4>
                <p>导入完成后自动生成多维度简历分析报告和优化建议</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon :size="20" color="#e6a23c"><Connection /></el-icon>
              </div>
              <div class="feature-content">
                <h4>精准匹配</h4>
                <p>解析完成后即可使用智能匹配功能，寻找最适合的职位</p>
              </div>
            </div>
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon :size="20" color="#909399"><EditPen /></el-icon>
              </div>
              <div class="feature-content">
                <h4>灵活编辑</h4>
                <p>导入后可随时修改和完善简历内容，支持多版本管理</p>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="supported-formats">
            <h4>支持的文件格式</h4>
            <div class="format-tags">
              <el-tag type="primary" effect="plain" size="large">PDF</el-tag>
              <el-tag type="success" effect="plain" size="large">DOC</el-tag>
              <el-tag type="warning" effect="plain" size="large">DOCX</el-tag>
            </div>
            <p class="format-note">文件大小不超过 10MB</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, Document, MagicStick, Connection, EditPen, List } from '@element-plus/icons-vue'
import { useResumeUpload } from '../composables/useResumeUpload'
import ResumeDropzone from '../components/ResumeDropzone.vue'
import ResumeParserStatus from '../components/ResumeParserStatus.vue'
import ResumePreviewCard from '../components/ResumePreviewCard.vue'
import type { ResumeParseResult, ResumeUploadResult } from '../types'

const router = useRouter()

// 保存最后一次上传的文件用于重试
const lastFile = ref<File | null>(null)

const {
  fileName,
  fileSize,
  isDragging,
  uploadResult,
  parsedData,
  progress,
  isIdle,
  isProcessing,
  isSuccess,
  isError,
  formatFileSize,
  MAX_FILE_SIZE,
  ACCEPTED_EXTENSIONS,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  handleFileSelect,
  handleFile,
  reset
} = useResumeUpload({
  onParsed: (data: ResumeParseResult, result: ResumeUploadResult) => {
    ElMessage.success(`简历导入成功！已解析 ${data.name || '候选人'} 的简历信息`)
  },
  onError: () => {
    // 错误消息已在 composable 中显示
  }
})

/** 包装拖拽事件以保存文件引用 */
const onDragOver = (e: DragEvent) => handleDragOver(e)
const onDragLeave = (e: DragEvent) => handleDragLeave(e)
const onDrop = async (e: DragEvent) => {
  handleDragLeave(e)
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    lastFile.value = files[0]
    await handleFile(files[0])
  }
}

/** 包装文件选择事件以保存文件引用 */
const onFileSelect = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    lastFile.value = files[0]
    await handleFile(files[0])
  }
  target.value = ''
}

/** 使用上次文件重试 */
const retryWithLastFile = async () => {
  if (lastFile.value) {
    await handleFile(lastFile.value)
  } else {
    reset()
  }
}

/** 跳转到编辑页面（带简历 ID） */
const goToEditResume = () => {
  const id = uploadResult.value?.id
  if (id) {
    router.push({ path: '/my-resume', query: { id } })
  } else {
    router.push('/my-resume')
  }
}

/** 跳转到编辑页面并触发 AI 分析 */
const goToEditWithAnalysis = () => {
  const id = uploadResult.value?.id
  if (id) {
    router.push({ path: '/my-resume', query: { id, analyze: 'true' } })
  } else {
    router.push({ path: '/my-resume', query: { analyze: 'true' } })
  }
}
</script>

<style scoped>
/* ── 提示信息 ─────────────────────────────── */
.upload-tips {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.upload-tips h4 {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.upload-tips h4 .el-icon {
  color: var(--primary-500);
}

.upload-tips ul {
  margin: 0;
  padding-left: var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  line-height: 1.8;
}

.upload-tips li {
  margin-bottom: var(--space-1);
}

/* ── 功能介绍卡片 ─────────────────────────── */
.feature-card {
  position: sticky;
  top: var(--space-6);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.feature-item {
  display: flex;
  gap: var(--space-3);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--gray-50);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-content h4 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.feature-content p {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-sub);
  line-height: 1.6;
}

/* ── 支持格式 ─────────────────────────────── */
.supported-formats h4 {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.format-tags {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.format-note {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

/* ── 响应式 ───────────────────────────────── */
@media (max-width: 900px) {
  .feature-card {
    position: static;
    margin-top: var(--space-6);
  }
}
</style>
