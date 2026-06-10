<template>
  <div class="page-container">
    <div class="page-header-row">
      <div>
        <h2>面试日历</h2>
        <p class="page-subtitle">可视化查看您的面试安排</p>
      </div>
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="calendar">
          <el-icon><Calendar /></el-icon> 日历视图
        </el-radio-button>
        <el-radio-button value="list">
          <el-icon><List /></el-icon> 列表视图
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 日历视图 -->
    <div v-if="viewMode === 'calendar'" class="calendar-wrapper">
      <el-card shadow="never" class="calendar-card">
        <el-calendar v-model="currentDate" ref="calendarRef">
          <template #header="{ date }">
            <div class="calendar-header">
              <span class="calendar-title">{{ date }}</span>
              <div class="calendar-nav">
                <el-button size="small" @click="selectDate('prev-year')">上一年</el-button>
                <el-button size="small" @click="selectDate('prev-month')">上月</el-button>
                <el-button size="small" @click="selectDate('today')">今天</el-button>
                <el-button size="small" @click="selectDate('next-month')">下月</el-button>
                <el-button size="small" @click="selectDate('next-year')">下一年</el-button>
              </div>
            </div>
          </template>
          <template #date-cell="{ data }">
            <div
              class="calendar-cell"
              :class="{ today: data.isSelected, 'has-events': getEventsForDate(data.day).length > 0 }"
            >
              <span class="cell-day">{{ data.day.split('-')[2] }}</span>
              <div class="cell-events">
                <div
                  v-for="evt in getEventsForDate(data.day).slice(0, 3)"
                  :key="evt.id"
                  class="event-dot"
                  :class="'status-' + evt.status"
                  :title="evt.job_title"
                  @click.stop="openDetail(evt)"
                >
                  <span class="dot-label">{{ evt.job_title }}</span>
                </div>
                <span v-if="getEventsForDate(data.day).length > 3" class="more-tag">
                  +{{ getEventsForDate(data.day).length - 3 }}
                </span>
              </div>
            </div>
          </template>
        </el-calendar>
      </el-card>

      <!-- 今日面试 -->
      <el-card shadow="never" class="today-card" v-if="todayEvents.length > 0">
        <template #header>
          <span>今日面试 ({{ todayEvents.length }})</span>
        </template>
        <div class="today-list">
          <div v-for="evt in todayEvents" :key="evt.id" class="today-item" @click="openDetail(evt)">
            <div class="today-time">{{ formatTime(evt.interview_time) }}</div>
            <div class="today-info">
              <strong>{{ evt.job_title }}</strong>
              <span>{{ evt.employer_name || evt.company || '' }}</span>
            </div>
            <el-tag :type="getStatusType(evt.status)" size="small">{{ getStatusText(evt.status) }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-wrapper">
      <el-card shadow="never">
        <el-table :data="interviews" v-loading="loading" stripe>
          <el-table-column label="面试时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.interview_time) }}</template>
          </el-table-column>
          <el-table-column label="职位" min-width="160">
            <template #default="{ row }">{{ row.job_title || '-' }}</template>
          </el-table-column>
          <el-table-column label="企业" width="140">
            <template #default="{ row }">{{ row.employer_name || row.company || '-' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ getTypeText(row.interview_type) }}</template>
          </el-table-column>
          <el-table-column label="地点" width="140">
            <template #default="{ row }">{{ row.interview_location || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 1" type="success" link size="small" @click="goToChat(row.id)">沟通</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 面试详情对话框 -->
    <el-dialog v-model="detailVisible" title="面试详情" width="500px" destroy-on-close>
      <div v-if="currentInterview" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="职位">{{ currentInterview.job_title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业">{{ currentInterview.employer_name || currentInterview.company || '-' }}</el-descriptions-item>
          <el-descriptions-item label="面试时间">{{ formatDateTime(currentInterview.interview_time) }}</el-descriptions-item>
          <el-descriptions-item label="面试地点">{{ currentInterview.interview_location || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="面试类型">{{ getTypeText(currentInterview.interview_type) }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentInterview.contact_person || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentInterview.contact_phone || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentInterview.status)">{{ getStatusText(currentInterview.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentInterview.notes" label="备注">
            {{ currentInterview.notes }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentInterview?.status === 0" type="primary" @click="confirmInterview(currentInterview)">确认参加</el-button>
        <el-button v-if="currentInterview?.status === 1" type="success" @click="goToChat(currentInterview.id)">在线沟通</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, List, Clock } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()

// 状态
const viewMode = ref('calendar')
const currentDate = ref(new Date())
const calendarRef = ref()
const interviews = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentInterview = ref(null)

// 计算属性
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const todayEvents = computed(() => {
  return getEventsForDate(todayStr.value)
})

// 获取某天的面试事件
const getEventsForDate = (day) => {
  return interviews.value.filter(evt => {
    if (!evt.interview_time) return false
    const d = new Date(evt.interview_time)
    const evtDay = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    return evtDay === day
  })
}

// 日历导航
const selectDate = (action) => {
  if (!calendarRef.value) return
  if (action === 'today') {
    currentDate.value = new Date()
  } else {
    calendarRef.value.selectDate(action)
  }
}

// 加载面试数据
const fetchInterviews = async () => {
  loading.value = true
  try {
    const res = await request.get('/interview/my')
    interviews.value = res || []
  } catch {
    ElMessage.error('获取面试列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return '待定'
  const d = new Date(date)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatDateTime = (date) => {
  if (!date) return '待定'
  return new Date(date).toLocaleString('zh-CN')
}

// 状态和类型
const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已完成' }
  return texts[status] || '未知'
}

const getTypeText = (type) => {
  const texts = { 1: '现场面试', 2: '视频面试', 3: '电话面试' }
  return texts[type] || '未知'
}

// 详情对话框
const openDetail = (evt) => {
  currentInterview.value = evt
  detailVisible.value = true
}

// 确认面试
const confirmInterview = async (item) => {
  try {
    await ElMessageBox.confirm('确认参加此次面试？', '提示', { type: 'info' })
    await request.put(`/interview/${item.id}/status`, { status: 1 })
    ElMessage.success('已确认参加面试')
    detailVisible.value = false
    fetchInterviews()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

// 跳转聊天
const goToChat = (interviewId) => {
  detailVisible.value = false
  router.push({ name: 'InterviewChat', params: { id: interviewId } })
}

onMounted(() => {
  fetchInterviews()
})
</script>

<style scoped>
.page-container {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.page-header-row h2 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.page-subtitle {
  margin: var(--space-1) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* ── 日历视图 ─────────────────────────────── */
.calendar-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.calendar-card {
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border) !important;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 var(--space-2);
}

.calendar-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.calendar-nav {
  display: flex;
  gap: var(--space-1);
}

.calendar-cell {
  height: 100%;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  padding: var(--space-1);
  position: relative;
}

.cell-day {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-700);
  margin-bottom: 2px;
}

.calendar-cell.today .cell-day {
  color: var(--color-surface);
  background: var(--primary-500);
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-cell.has-events { background: var(--gray-50); }

.cell-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  overflow: hidden;
}

.event-dot {
  font-size: 10px;
  padding: 1px var(--space-1);
  border-radius: var(--radius-xs);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  transition: opacity var(--duration-fast);
}

.event-dot:hover { opacity: 0.8; }
.event-dot.status-0 { background: var(--warning-50); color: var(--warning-700); }
.event-dot.status-1 { background: var(--success-50); color: var(--success-700); }
.event-dot.status-2 { background: var(--danger-50); color: var(--danger-700); }
.event-dot.status-3 { background: var(--primary-50); color: var(--primary-700); }

.more-tag {
  font-size: 10px;
  color: var(--color-text-muted);
  padding: 0 var(--space-1);
}

/* ── 今日面试卡片 ──────────────────────────── */
.today-card {
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border) !important;
}

.today-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.today-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.today-item:hover {
  background: var(--gray-100);
  transform: translateX(2px);
}

.today-time {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--primary-500);
  min-width: 55px;
}

.today-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.today-info strong {
  font-size: var(--text-sm);
  color: var(--gray-900);
}

.today-info span {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.detail-content { padding: var(--space-2) 0; }

/* ── Element Plus 日历覆盖 ─────────────────── */
:deep(.el-calendar) { --el-calendar-cell-width: auto; }
:deep(.el-calendar-day) { min-height: 80px; padding: 0; }
:deep(.el-calendar-table td) { border-right: 1px solid var(--gray-100); }
:deep(.el-calendar-table td.is-today) { background: var(--primary-50); }
</style>
