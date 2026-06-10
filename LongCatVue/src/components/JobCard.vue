<template>
  <div class="job-card" @click="handleClick">
    <div class="card-header">
      <h4 class="job-title">{{ job.jobTitle }}</h4>
    </div>
    
    <div class="card-body">
      <div class="company-name">
        <el-icon><OfficeBuilding /></el-icon>
        {{ job.companyName }}
      </div>
      
      <div class="job-info">
        <div class="info-item" v-if="job.salaryMin || job.salaryMax">
          <el-icon><Money /></el-icon>
          <span>{{ formatSalary(job.salaryMin, job.salaryMax) }}</span>
        </div>
        
        <div class="info-item" v-if="job.location">
          <el-icon><Location /></el-icon>
          <span>{{ job.location }}</span>
        </div>
      </div>
      
      <div class="job-meta" v-if="job.daysInStage !== undefined">
        <el-tag size="small" type="info">
          停留 {{ job.daysInStage }} 天
        </el-tag>
      </div>
      
      <div class="job-meta" v-if="job.favoriteTime">
        <el-tag size="small" type="warning">
          收藏于 {{ formatDate(job.favoriteTime) }}
        </el-tag>
      </div>
    </div>
    
    <div class="card-footer" v-if="job.type === 'application'">
      <el-button 
        size="small" 
        type="primary" 
        link
        @click.stop="viewDetail"
      >
        查看详情
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { OfficeBuilding, Money, Location } from '@element-plus/icons-vue'

const props = defineProps({
  job: {
    type: Object,
    required: true
  },
  stage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['click'])

const formatSalary = (min, max) => {
  if (min && max) return `${min}-${max}K`
  if (min) return `${min}K以上`
  if (max) return `${max}K以下`
  return '面议'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const handleClick = () => {
  emit('click', props.job)
}

const viewDetail = () => {
  // TODO: 跳转到职位详情页
  console.log('查看职位详情:', props.job.jobId)
}
</script>

<style scoped>
.job-card {
  background: white;
  border-radius: var(--radius-md);
  padding: var(--space-4);
  cursor: grab;
  transition: all var(--duration-fast) var(--ease-out);
  border: 2px solid transparent;
  box-shadow: var(--shadow-sm);
}

.job-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--primary-200);
}

.job-card:active {
  cursor: grabbing;
}

.card-header {
  margin-bottom: var(--space-3);
}

.job-title {
  margin: 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.company-name {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--gray-600);
}

.job-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--gray-500);
}

.job-meta {
  margin-top: var(--space-2);
}

.card-footer {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
  display: flex;
  justify-content: flex-end;
}
</style>
