<template>
  <div class="page-container">
    <div class="page-header-row">
      <div>
        <h2>面试管理</h2>
        <p class="page-subtitle">管理您向候选人发出的面试邀请</p>
      </div>
    </div>

    <!-- 状态统计 -->
    <div class="stats-row">
      <div
        v-for="s in statusStats"
        :key="s.value"
        class="stat-tab"
        :class="{ active: statusFilter === s.value }"
        @click="statusFilter = s.value"
      >
        <span class="stat-count">{{ s.count }}</span>
        <span class="stat-name">{{ s.label }}</span>
      </div>
    </div>

    <!-- 面试列表 -->
    <div v-loading="loading" class="interview-list">
      <el-empty v-if="filteredInterviews.length === 0 && !loading" description="暂无面试邀请" />

      <div v-else class="interview-card" v-for="item in filteredInterviews" :key="item.id">
        <div class="card-top">
          <div class="candidate-info">
            <h3 class="candidate-name">{{ item.applicant_name || '候选人' }}</h3>
            <span class="job-title">- {{ item.job_title || '未知职位' }}</span>
          </div>
          <el-tag :type="getStatusType(item.status)" size="large" effect="dark">
            {{ getStatusText(item.status) }}
          </el-tag>
        </div>

        <div class="card-details">
          <div class="detail-grid">
            <div class="detail-item">
              <div class="detail-icon"><el-icon><Clock /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">面试时间</span>
                <span class="detail-value">{{ formatDateTime(item.interview_time) }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><Location /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">面试地点</span>
                <span class="detail-value">{{ item.interview_location || '未设置' }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><User /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">联系人</span>
                <span class="detail-value">{{ item.contact_person || '未设置' }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><Phone /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">联系电话</span>
                <span class="detail-value">{{ item.contact_phone || '未设置' }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><VideoCamera /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">面试类型</span>
                <span class="detail-value">{{ getInterviewTypeText(item.interview_type) }}</span>
              </div>
            </div>
            <div v-if="item.notes" class="detail-item detail-full">
              <div class="detail-icon"><el-icon><Document /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">备注</span>
                <span class="detail-value">{{ item.notes }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card-actions" v-if="item.status === 0 || item.status === 1">
          <el-button type="primary" size="small" @click="goToChat(item.id)">
            <el-icon><ChatDotRound /></el-icon> 在线沟通
          </el-button>
          <el-button v-if="item.status === 0" type="success" size="small" @click="updateStatus(item.id, 3)">
            <el-icon><Check /></el-icon> 标记完成
          </el-button>
          <el-button type="danger" size="small" plain @click="updateStatus(item.id, 2)">
            <el-icon><Close /></el-icon> 取消面试
          </el-button>
        </div>
        <div class="card-actions" v-else>
          <el-button type="info" size="small" disabled>
            {{ item.status === 2 ? '已取消' : '已完成' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Location, User, Phone, VideoCamera, Document, Check, Close, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const interviews = ref([])
const statusFilter = ref('all')

const filteredInterviews = computed(() => {
  if (statusFilter.value === 'all') return interviews.value
  return interviews.value.filter(i => String(i.status) === statusFilter.value)
})

const statusStats = computed(() => {
  const all = interviews.value
  return [
    { label: '全部', value: 'all', count: all.length },
    { label: '待确认', value: '0', count: all.filter(i => i.status === 0).length },
    { label: '已确认', value: '1', count: all.filter(i => i.status === 1).length },
    { label: '已完成', value: '3', count: all.filter(i => i.status === 3).length },
    { label: '已取消', value: '2', count: all.filter(i => i.status === 2).length },
  ]
})

const fetchInterviews = async () => {
  loading.value = true
  try {
    const res = await request.get('/interview/employer')
    interviews.value = res || []
  } catch (error) {
    console.error('加载面试列表失败:', error)
  } finally {
    loading.value = false
  }
}

const updateStatus = async (id, status) => {
  const action = status === 2 ? '取消' : '标记完成'
  try {
    await ElMessageBox.confirm(`确定要${action}此面试吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.put(`/interview/${id}/status`, { status })
    ElMessage.success(`已${action}`)
    fetchInterviews()
  } catch { /* 用户取消 */ }
}

const formatDateTime = (dt) => {
  if (!dt) return '未设置'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已完成' }
  return texts[status] || '未知'
}

const getInterviewTypeText = (type) => {
  const texts = { 1: '现场面试', 2: '视频面试', 3: '电话面试' }
  return texts[type] || '未设置'
}

const goToChat = (interviewId) => {
  router.push({ name: 'InterviewChat', params: { id: interviewId } })
}

onMounted(() => { fetchInterviews() })
</script>

<style scoped>
/* ── 面试卡片 ─────────────────────────────── */
.interview-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.interview-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5) var(--space-6);
  transition: all var(--duration-normal) var(--ease-out);
}

.interview-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
}

.candidate-name {
  margin: 0;
  font-size: var(--text-lg);
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
  display: inline;
}

.job-title {
  font-size: var(--text-base);
  color: var(--color-text-sub);
  font-weight: var(--weight-normal);
}

/* ── 详情网格补充 ─────────────────────────── */
.detail-full {
  grid-column: 1 / -1;
}

.detail-value {
  font-weight: var(--weight-medium);
}

/* ── 操作按钮 ─────────────────────────────── */
.card-actions {
  display: flex;
  gap: var(--space-2);
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}
</style>
