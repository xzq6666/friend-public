<template>
  <div class="page-container">
    <div class="page-header-row">
      <div>
        <h2>我的投递</h2>
        <p class="page-subtitle">查看您投递的职位及处理状态</p>
      </div>
    </div>

    <!-- 状态统计 -->
    <div class="stats-row">
      <div
        v-for="s in statusStats"
        :key="s.value"
        class="stat-tab"
        :class="{ active: statusFilter === s.value }"
        @click="statusFilter = s.value; fetchApplications()"
      >
        <span class="stat-count">{{ s.count }}</span>
        <span class="stat-name">{{ s.label }}</span>
      </div>
    </div>

    <!-- 投递列表 -->
    <div v-loading="loading" class="application-list">
      <el-empty v-if="!loading && applications.length === 0" description="暂无投递记录" />

      <div v-else class="application-card" v-for="item in applications" :key="item.id">
        <div class="card-top">
          <div class="job-info">
            <h3 class="job-title">{{ item.job_title || '未知职位' }}</h3>
            <div class="meta-row">
              <span class="meta-item">
                <el-icon><Location /></el-icon>
                {{ item.job_location || '未知地点' }}
              </span>
              <span class="meta-item">
                <el-icon><Money /></el-icon>
                {{ formatSalary(item.salary_min, item.salary_max) }}
              </span>
              <span class="meta-item">
                <el-icon><OfficeBuilding /></el-icon>
                {{ item.employer_name || '未知企业' }}
              </span>
            </div>
          </div>
          <el-tag :type="getStatusType(item.status)" size="large" effect="dark">
            {{ getStatusText(item.status) }}
          </el-tag>
        </div>

        <div class="card-bottom">
          <div class="tags" v-if="item.experience_required || item.education_required">
            <el-tag v-if="item.experience_required" size="small" type="info">{{ item.experience_required }}</el-tag>
            <el-tag v-if="item.education_required" size="small" type="info">{{ item.education_required }}</el-tag>
          </div>
          <div class="apply-time">{{ formatDate(item.create_time) }}</div>
        </div>

        <div v-if="item.cover_letter" class="cover-letter">
          <span class="label">求职信：</span>{{ item.cover_letter }}
        </div>
        <div v-if="item.reject_reason" class="reject-reason">
          <span class="label">拒绝原因：</span>{{ item.reject_reason }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, Money, OfficeBuilding } from '@element-plus/icons-vue'
import request from '../utils/request'

const applications = ref([])
const loading = ref(false)
const statusFilter = ref('')

const statusStats = computed(() => {
  const all = applications.value
  return [
    { label: '全部', value: '', count: all.length },
    { label: '待处理', value: '0', count: all.filter(i => i.status === 0).length },
    { label: '已查看', value: '1', count: all.filter(i => i.status === 1).length },
    { label: '面试', value: '2', count: all.filter(i => i.status === 2).length },
    { label: '已录用', value: '3', count: all.filter(i => i.status === 3).length },
    { label: '已拒绝', value: '4', count: all.filter(i => i.status === 4).length },
  ]
})

const fetchApplications = async () => {
  loading.value = true
  try {
    const res = await request.get('/application/my')
    let data = res || []
    if (statusFilter.value !== '') {
      data = data.filter(item => String(item.status) === statusFilter.value)
    }
    applications.value = data
  } catch (error) {
    ElMessage.error('获取投递记录失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待处理', 1: '已查看', 2: '邀请面试', 3: '已录用', 4: '已拒绝' }
  return texts[status] || '未知'
}

const formatSalary = (min, max) => {
  if (!min && !max) return '面议'
  const fmt = (v) => (v / 1000).toFixed(0) + 'K'
  if (min && max) return `${fmt(min)} - ${fmt(max)}`
  if (min) return `${fmt(min)}起`
  return `最高${fmt(max)}`
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => { fetchApplications() })
</script>

<style scoped>
/* ── 投递卡片 ─────────────────────────────── */
.application-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.application-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  transition: all var(--duration-normal) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.application-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--gray-200);
  transition: background var(--duration-normal) var(--ease-out);
}

.application-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.application-card:hover::before {
  background: var(--primary-500);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.job-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-lg);
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.tags {
  display: flex;
  gap: var(--space-2);
}

.apply-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.cover-letter,
.reject-reason {
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--gray-100);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.cover-letter .label,
.reject-reason .label {
  color: var(--color-text-muted);
  font-weight: var(--weight-medium);
}

.reject-reason {
  color: var(--danger-500);
}
</style>
