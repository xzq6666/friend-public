<template>
  <div class="page-container">
    <div class="page-header-row">
      <div>
        <h2>我的面试</h2>
        <p class="page-subtitle">查看和管理您收到的面试邀请</p>
      </div>
    </div>

    <div class="stats-row">
      <div
        v-for="s in statusStats"
        :key="s.value"
        class="stat-tab"
        :class="{ active: statusFilter === s.value }"
        @click="statusFilter = s.value; fetchInterviews()"
      >
        <span class="stat-count">{{ s.count }}</span>
        <span class="stat-name">{{ s.label }}</span>
      </div>
    </div>

    <div v-loading="loading" class="interview-list">
      <el-empty v-if="!loading && interviews.length === 0" description="暂无面试邀请" />

      <div v-else class="interview-card" v-for="item in interviews" :key="item.id">
        <div class="card-top">
          <div class="job-info">
            <h3 class="job-title">{{ item.job_title || '未知职位' }}</h3>
            <div class="company-info">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ item.employer_name || '未知企业' }}</span>
            </div>
          </div>
          <div class="status-area">
            <el-tag :type="getStatusType(item.status)" size="large" effect="dark">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
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
                <span class="detail-value">{{ item.interview_location || '待定' }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><User /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">联系人</span>
                <span class="detail-value">{{ item.contact_person || '待定' }}</span>
              </div>
            </div>
            <div class="detail-item">
              <div class="detail-icon"><el-icon><Phone /></el-icon></div>
              <div class="detail-content">
                <span class="detail-label">联系电话</span>
                <span class="detail-value">{{ item.contact_phone || '待定' }}</span>
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

        <div class="card-actions" v-if="item.status === 0">
          <el-button type="primary" @click="goToChat(item.id)">
            <el-icon><ChatDotRound /></el-icon> 在线沟通
          </el-button>
          <el-button type="primary" @click="confirmInterview(item)">
            <el-icon><Check /></el-icon> 确认参加
          </el-button>
          <el-button type="danger" plain @click="cancelInterview(item)">
            <el-icon><Close /></el-icon> 拒绝
          </el-button>
          <div class="action-spacer"></div>
          <el-button type="danger" plain @click="deleteInterview(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
        <div class="card-actions" v-else-if="item.status === 1 || item.status === 3">
          <el-button type="primary" @click="goToChat(item.id)">
            <el-icon><ChatDotRound /></el-icon> 在线沟通
          </el-button>
          <div class="action-spacer"></div>
          <el-button type="danger" plain @click="deleteInterview(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
        <div class="card-actions" v-else>
          <div class="action-spacer"></div>
          <el-button type="danger" plain @click="deleteInterview(item)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Clock, User, Phone, VideoCamera, Document, OfficeBuilding, Check, Close, ChatDotRound, Delete } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const interviews = ref([])
const loading = ref(false)
const statusFilter = ref('')

const statusStats = computed(() => {
  const all = interviews.value
  return [
    { label: '全部', value: '', count: all.length },
    { label: '待确认', value: '0', count: all.filter(i => i.status === 0).length },
    { label: '已确认', value: '1', count: all.filter(i => i.status === 1).length },
    { label: '已完成', value: '3', count: all.filter(i => i.status === 3).length },
    { label: '已取消', value: '2', count: all.filter(i => i.status === 2).length },
  ]
})

const fetchInterviews = async () => {
  loading.value = true
  try {
    const res = await request.get('/interview/my', { skipErrorNotification: true })
    let data = Array.isArray(res) ? res : []
    if (statusFilter.value !== '') {
      data = data.filter(item => String(item.status) === statusFilter.value)
    }
    interviews.value = data
  } catch (error) {
    interviews.value = []
    if (error?.response?.status !== 401) {
      ElMessage.error('获取面试列表失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const confirmInterview = async (item) => {
  try {
    await ElMessageBox.confirm('确认参加此次面试？', '提示', { type: 'info' })
    await request.put(`/interview/${item.id}/status`, { status: 1 })
    ElMessage.success('已确认参加面试')
    const idx = interviews.value.findIndex(i => i.id === item.id)
    if (idx !== -1) interviews.value[idx].status = 1
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.error || '操作失败，请重试')
      fetchInterviews()
    }
  }
}

const cancelInterview = async (item) => {
  try {
    await ElMessageBox.confirm('确定拒绝此次面试邀请？', '提示', { type: 'warning' })
    await request.put(`/interview/${item.id}/status`, { status: 2 })
    ElMessage.success('已拒绝面试邀请')
    const idx = interviews.value.findIndex(i => i.id === item.id)
    if (idx !== -1) interviews.value[idx].status = 2
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.error || '操作失败，请重试')
      fetchInterviews()
    }
  }
}

const deleteInterview = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除此面试记录？删除后不可恢复。', '提示', { type: 'warning' })
    await request.delete(`/interview/${item.id}`)
    ElMessage.success('删除成功')
    interviews.value = interviews.value.filter(i => i.id !== item.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.error || '删除失败，请重试')
    }
  }
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
  return texts[type] || '未知'
}

const formatDateTime = (date) => {
  if (!date) return '待定'
  return new Date(date).toLocaleString('zh-CN')
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
  align-items: flex-start;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
}

.job-title {
  margin: 0 0 var(--space-1);
  font-size: var(--text-lg);
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
}

.company-info {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.card-details { margin-bottom: var(--space-4); }

/* ── 详情网格补充 ─────────────────────────── */
.detail-full { grid-column: 1 / -1; }

.detail-value {
  font-weight: var(--weight-medium);
}

.card-actions {
  display: flex;
  gap: var(--space-2);
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.action-spacer {
  flex: 1;
}
</style>
