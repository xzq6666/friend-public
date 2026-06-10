<template>
  <el-card shadow="hover" class="preview-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">解析结果预览</span>
        <el-tag type="success" size="small">已就绪</el-tag>
      </div>
    </template>

    <div class="preview-content">
      <!-- 基本信息 -->
      <div class="preview-section">
        <h4>基本信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">姓名</span>
            <span class="info-value">{{ data.name || '未识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">年龄</span>
            <span class="info-value">{{ data.age ? data.age + '岁' : '未识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">学历</span>
            <span class="info-value">{{ data.education || '未识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">电话</span>
            <span class="info-value">{{ data.phone || '未识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ data.email || '未识别' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">期望薪资</span>
            <span class="info-value">{{ data.expectedSalary ? data.expectedSalary + '元/月' : '未填写' }}</span>
          </div>
        </div>
      </div>

      <!-- 技能 -->
      <div class="preview-section">
        <h4>技能标签</h4>
        <div class="skill-tags">
          <el-tag
            v-for="skill in skillList"
            :key="skill"
            size="small"
            effect="plain"
            class="skill-tag"
          >
            {{ skill }}
          </el-tag>
          <span v-if="skillList.length === 0" class="empty-text">未识别到技能</span>
        </div>
      </div>

      <!-- 工作经历 -->
      <div class="preview-section">
        <h4>工作经历</h4>
        <div v-if="data.workExperience.length > 0" class="work-list">
          <div v-for="(item, index) in data.workExperience" :key="index" class="work-item">
            <div class="work-header">
              <span class="work-company">{{ item.company }}</span>
              <span class="work-position">{{ item.position }}</span>
            </div>
            <div class="work-date">
              {{ item.startDate }} ~ {{ item.current ? '至今' : item.endDate }}
            </div>
          </div>
        </div>
        <span v-else class="empty-text">未识别到工作经历</span>
      </div>

      <!-- 自我评价 -->
      <div v-if="data.selfIntroduction" class="preview-section">
        <h4>自我评价</h4>
        <p class="self-intro">{{ data.selfIntroduction }}</p>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="preview-actions">
      <el-button type="primary" @click="$emit('goToEdit')">
        <el-icon><EditPen /></el-icon> 编辑完善简历
      </el-button>
      <el-button v-if="hasAiAnalysis" plain @click="$emit('analyze')">
        <el-icon><MagicStick /></el-icon> 查看 AI 分析
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { EditPen, MagicStick } from '@element-plus/icons-vue'

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  hasAiAnalysis: {
    type: Boolean,
    default: false
  }
})

defineEmits(['goToEdit', 'analyze'])

const skillList = computed(() => {
  if (!props.data.skills) return []
  return props.data.skills.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
})
</script>

<style scoped>
.preview-card {
  position: sticky;
  top: var(--space-6);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.preview-section h4 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
}

/* ── 信息网格 ─────────────────────────────── */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2) var(--space-3);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.info-label {
  font-size: 10px;
  color: var(--gray-400);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.info-value {
  font-size: var(--text-sm);
  color: var(--gray-800);
  font-weight: var(--weight-medium);
}

/* ── 技能标签 ─────────────────────────────── */
.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.skill-tag {
  border-radius: var(--radius-sm) !important;
}

/* ── 工作经历 ─────────────────────────────── */
.work-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.work-item {
  padding: var(--space-2);
  background: var(--gray-50);
  border-radius: var(--radius-sm);
}

.work-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-2);
}

.work-company {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
}

.work-position {
  font-size: var(--text-xs);
  color: var(--gray-500);
}

.work-date {
  font-size: 10px;
  color: var(--gray-400);
  margin-top: 2px;
}

/* ── 自我评价 ─────────────────────────────── */
.self-intro {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: 1.6;
  margin: 0;
}

/* ── 空状态 ───────────────────────────────── */
.empty-text {
  font-size: var(--text-sm);
  color: var(--gray-400);
}

/* ── 操作按钮 ─────────────────────────────── */
.preview-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.preview-actions .el-button {
  width: 100%;
  margin: 0 !important;
}
</style>
