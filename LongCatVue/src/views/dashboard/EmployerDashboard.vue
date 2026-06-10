<template>
  <div class="dashboard-page">
    <!-- 数据概览条 -->
    <div class="stats-strip" v-loading="loading">
      <div
        v-for="stat in displayStats"
        :key="stat.label"
        class="stat-cell"
        :class="{ clickable: !!stat.route }"
        @click="stat.route && $router.push(stat.route)"
      >
        <span class="stat-val">{{ stat.value }}</span>
        <span class="stat-key">{{ stat.label }}</span>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="dash-body" v-loading="loading">
      <!-- 左侧主内容 -->
      <div class="dash-left">
        <!-- 近 7 日投递趋势 -->
        <div class="dash-card">
          <div class="card-title"><span>近 7 日投递趋势</span></div>
          <div ref="applicationChartRef" class="chart-box"></div>
        </div>

        <!-- 最近收到的投递 -->
        <div class="dash-card">
          <div class="card-title">
            <span>最近投递</span>
            <button class="link-btn" @click="$router.push('/my-candidates')">查看全部</button>
          </div>
          <div v-if="recentApplications.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            </div>
            <p>暂无收到投递</p>
            <el-button type="primary" plain size="small" @click="$router.push('/my-jobs')">去发布职位</el-button>
          </div>
          <div v-else class="timeline-list">
            <div v-for="item in recentApplications" :key="item.id" class="timeline-item" @click="$router.push('/my-candidates')">
              <div class="timeline-dot s0"></div>
              <div class="timeline-main">
                <span class="timeline-title">{{ item.applicant_name }} 投递了 {{ item.job_title }}</span>
                <span class="timeline-date">{{ formatDate(item.create_time) }}</span>
              </div>
              <span class="status-pill" :class="'s' + item.status">{{ getStatusText(item.status) }}</span>
            </div>
          </div>
        </div>

        <!-- 职位表现 -->
        <div class="dash-card">
          <div class="card-title"><span>职位表现</span></div>
          <div class="job-performance">
            <div v-for="job in jobPerformance" :key="job.id" class="performance-item" @click="$router.push('/my-jobs')">
              <div class="performance-info">
                <span class="performance-title">{{ job.title }}</span>
                <div class="performance-stats">
                  <span class="stat"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg> {{ job.views }}</span>
                  <span class="stat"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/></svg> {{ job.applicants }}</span>
                </div>
              </div>
              <span class="status-pill" :class="job.status === 1 ? 's2' : 's0'">{{ job.status === 1 ? '在线' : '下线' }}</span>
            </div>
          </div>
        </div>

        <!-- 招聘小贴士 -->
        <div class="dash-card">
          <div class="card-title"><span>招聘小贴士</span></div>
          <div class="tips-list">
            <div v-for="tip in hiringTips" :key="tip.id" class="tip-item">
              <div class="tip-icon" :style="{ background: tip.bgColor }">
                <el-icon :size="14" :color="tip.color"><component :is="tip.icon" /></el-icon>
              </div>
              <div class="tip-content">
                <span class="tip-title">{{ tip.title }}</span>
                <span class="tip-desc">{{ tip.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="dash-right">
        <!-- 待办事项 -->
        <div class="dash-card">
          <div class="card-title"><span>待办事项</span></div>
          <div class="pending-tasks">
            <div v-for="task in pendingTasks" :key="task.label" class="task-item" @click="$router.push(task.route)">
              <div class="task-icon" :style="{ background: task.bgColor }">
                <el-icon :size="14" :color="task.color"><component :is="task.icon" /></el-icon>
              </div>
              <div class="task-info">
                <span class="task-label">{{ task.label }}</span>
                <span class="task-count">{{ task.count }} 项待处理</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 招聘漏斗 -->
        <div class="dash-card">
          <div class="card-title"><span>招聘漏斗</span></div>
          <div class="recruitment-funnel">
            <div v-for="stage in funnelStages" :key="stage.label" class="funnel-item">
              <div class="funnel-bar">
                <div class="funnel-fill" :style="{ width: stage.percent + '%', background: stage.color }"></div>
              </div>
              <div class="funnel-info">
                <span class="funnel-label">{{ stage.label }}</span>
                <span class="funnel-count">{{ stage.count }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 我的职位概览 -->
        <div class="dash-card">
          <div class="card-title">
            <span>我的职位</span>
            <button class="link-btn" @click="$router.push('/my-jobs')">管理</button>
          </div>
          <div v-if="myJobs.length === 0" class="empty-state empty-sm">
            <p>还没有发布职位</p>
            <el-button type="primary" plain size="small" @click="$router.push('/my-jobs')">去发布</el-button>
          </div>
          <div v-else class="data-rows">
            <div v-for="job in myJobs" :key="job.id" class="data-row" style="cursor:pointer" @click="$router.push('/my-jobs')">
              <div style="flex:1;min-width:0">
                <span class="data-value" style="display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ job.title }}</span>
                <span class="data-label" style="font-size:var(--text-xs)">{{ job.salaryRange || '面议' }}</span>
              </div>
              <span class="status-pill" :class="job.status === 1 ? 's2' : 's0'">{{ job.status === 1 ? '在线' : '下线' }}</span>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="dash-card">
          <div class="card-title"><span>快捷操作</span></div>
          <div class="shortcut-grid">
            <button class="shortcut-item" @click="$router.push('/my-jobs')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg></span>
              <span>发布职位</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/my-resumes-pool')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg></span>
              <span>查看简历</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/match')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg></span>
              <span>人才匹配</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/my-manage-interviews')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg></span>
              <span>面试管理</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/profile')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
              <span>个人中心</span>
            </button>
          </div>
        </div>

        <!-- 实时活动流 -->
        <div class="dash-card">
          <div class="card-title">
            <span>实时动态</span>
            <el-icon :size="14" color="var(--gray-400)"><Clock /></el-icon>
          </div>
          <div v-if="activityFeed.length === 0" class="empty-state empty-sm">
            <el-icon :size="24" color="var(--gray-300)"><InfoFilled /></el-icon>
            <p>暂无最新动态</p>
          </div>
          <div v-else class="activity-list">
            <div v-for="(item, idx) in activityFeed" :key="idx" class="activity-item">
              <div class="activity-icon" :style="{ background: item.color + '18', color: item.color }">
                <el-icon :size="14"><component :is="item.icon" /></el-icon>
              </div>
              <div class="activity-main">
                <span class="activity-title">{{ item.title }}</span>
                <span class="activity-time">{{ formatActivityTime(item.time) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import * as echarts from 'echarts'
import { Clock, User, InfoFilled, Document, Calendar, Bell, EditPen, Star, Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({ jobCount: 0, applicationCount: 0, pendingApplications: 0, todayApplications: 0, interviewCount: 0, screenedApplications: 0, hiredCount: 0 })
const unreadCount = ref(0)
const recentApplications = ref([])
const myJobs = ref([])
const activityFeed = ref([])
const applicationChartRef = ref(null)
let appChart = null
const loading = ref(false)

const displayStats = computed(() => [
  { label: '在线职位', value: stats.value.jobCount, route: '/my-jobs' },
  { label: '收到投递', value: stats.value.applicationCount || 0, route: '/my-candidates' },
  { label: '待处理', value: stats.value.pendingApplications || 0, route: '/my-candidates' },
  { label: '今日新增', value: stats.value.todayApplications || 0, route: null }
])

// 待办事项
const pendingTasks = computed(() => [
  { label: '待审核简历', count: stats.value.pendingApplications || 0, icon: Document, color: '#6366f1', bgColor: 'rgba(99,102,241,0.1)', route: '/my-candidates' },
  { label: '待安排面试', count: stats.value.interviewCount || 0, icon: Calendar, color: '#f59e0b', bgColor: 'rgba(245,158,11,0.1)', route: '/my-manage-interviews' },
  { label: '待回复消息', count: unreadCount.value || 0, icon: Bell, color: '#10b981', bgColor: 'rgba(16,185,129,0.1)', route: '/chat-history' }
])

// 招聘漏斗
const funnelStages = computed(() => {
  const total = stats.value.applicationCount || 0
  const screened = stats.value.screenedApplications || 0
  const interview = stats.value.interviewCount || 0
  const hired = stats.value.hiredCount || 0
  return [
    { label: '简历投递', count: total, percent: 100, color: '#6366f1' },
    { label: '简历筛选', count: screened, percent: total ? Math.round(screened / total * 100) : 0, color: '#8b5cf6' },
    { label: '面试邀请', count: interview, percent: total ? Math.round(interview / total * 100) : 0, color: '#f59e0b' },
    { label: '录用通知', count: hired, percent: total ? Math.round(hired / total * 100) : 0, color: '#10b981' }
  ]
})

// 职位表现
const jobPerformance = [
  { id: 1, title: '前端开发工程师', views: 256, applicants: 18, status: 1 },
  { id: 2, title: 'Java后端开发', views: 189, applicants: 12, status: 1 },
  { id: 3, title: '产品经理', views: 145, applicants: 8, status: 0 },
  { id: 4, title: 'UI设计师', views: 98, applicants: 5, status: 1 }
]

// 招聘小贴士
const hiringTips = [
  { id: 1, title: '优化职位描述', desc: '清晰的职位描述吸引更多合适候选人', icon: EditPen, color: '#6366f1', bgColor: 'rgba(99,102,241,0.1)' },
  { id: 2, title: '及时回复投递', desc: '快速响应提高候选人体验', icon: Star, color: '#f59e0b', bgColor: 'rgba(245,158,11,0.1)' },
  { id: 3, title: '完善企业信息', desc: '展示企业文化和福利待遇', icon: Trophy, color: '#10b981', bgColor: 'rgba(16,185,129,0.1)' }
]

const getStatusText = (s) => ['待处理', '已查看', '邀请面试', '已录用', '已拒绝'][s] || '未知'
const formatDate = (d) => d ? new Date(d).toLocaleDateString('zh-CN') : ''

const formatActivityTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString('zh-CN')
}

const getRecentDates = (n) => {
  const dates = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return dates
}

const resolveColor = (cssVar) => {
  if (!cssVar.startsWith('var(')) return cssVar
  const varName = cssVar.replace('var(', '').replace(')', '')
  return getComputedStyle(document.documentElement).getPropertyValue(varName).trim() || '#5B8FF9'
}

const initLineChart = (chartRef, dates, values, cssColor) => {
  if (!chartRef.value || !dates.length) return null
  const color = resolveColor(cssColor)
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c}' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates,
      axisLine: { lineStyle: { color: '#E8E8E8' } }, axisLabel: { color: '#8C8C8C', fontSize: 11 } },
    yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false },
      splitLine: { lineStyle: { color: '#F5F5F5' } }, axisLabel: { color: '#8C8C8C', fontSize: 11 } },
    series: [{ type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { width: 2, color }, itemStyle: { color },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: color + '18' }, { offset: 1, color: color + '00' }] } },
      data: values }]
  })
  return chart
}

const initApplicationChart = async () => {
  await nextTick()
  if (!applicationChartRef.value) return
  if (appChart) { appChart.dispose(); appChart = null }
  let dates = getRecentDates(7)
  let values = [3, 5, 2, 8, 6, 4, 7]
  try {
    const res = await request.get('/admin/stats/employer/trends', { params: { days: 7 }, skipErrorNotification: true })
    const trends = res?.trends || []
    if (trends.length) {
      dates = trends.map(t => t.date)
      values = trends.map(t => t.applications || 0)
    }
  } catch { /* use fallback */ }
  appChart = initLineChart(applicationChartRef, dates, values, 'var(--primary-500)')
  setTimeout(() => appChart?.resize(), 50)
}

const loadActivityFeed = async () => {
  if (!userStore.isLoggedIn) return
  const activities = []
  try {
    const jobsRes = await request.get('/job', { params: { page: 1, size: 5 }, skipErrorNotification: true })
    const jobs = jobsRes?.records || []
    for (const job of jobs.slice(0, 3)) {
      try {
        const apps = await request.get(`/application/job/${job.id}`, { params: { page: 1, size: 2 }, skipErrorNotification: true })
        ;(Array.isArray(apps) ? apps : []).forEach(a => {
          activities.push({
            type: 'application', icon: 'User', color: 'var(--primary-500)',
            title: `${a.applicant_name || '候选人'} 投递了「${job.title}」`,
            time: a.create_time
          })
        })
      } catch { /* ignore single job failure */ }
    }
    activities.sort((a, b) => new Date(b.time || 0) - new Date(a.time || 0))
    activityFeed.value = activities.slice(0, 8)
  } catch { activityFeed.value = [] }
}

const fetchStats = async () => {
  try {
    const [statsRes, unreadRes] = await Promise.all([
      request.get('/admin/stats/employer', { skipErrorNotification: true }).catch(() => null),
      request.get('/notification/unread-count', { skipErrorNotification: true }).catch(() => null)
    ])
    if (statsRes) {
      stats.value.jobCount = statsRes.myJobs || 0
      stats.value.applicationCount = statsRes.totalApplications || 0
      stats.value.pendingApplications = statsRes.pendingApplications || 0
      stats.value.todayApplications = statsRes.todayApplications || 0
      stats.value.interviewCount = statsRes.interviewCount || 0
      stats.value.screenedApplications = statsRes.screenedApplications || 0
      stats.value.hiredCount = statsRes.hiredCount || 0
    }
    if (unreadRes) {
      unreadCount.value = unreadRes.count || 0
    }
  } catch (error) {
    console.debug('获取统计数据失败:', error?.message)
  }
}

const fetchRecentApplications = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const jobs = await request.get('/job', { params: { page: 1, size: 5 }, skipErrorNotification: true })
    myJobs.value = (jobs.records || []).slice(0, 5)
    let allApps = []
    for (const job of (jobs.records || [])) {
      try {
        const apps = await request.get(`/application/job/${job.id}`, { skipErrorNotification: true })
        allApps.push(...(apps || []))
      } catch { /* ignore */ }
    }
    recentApplications.value = allApps.slice(0, 5)
  } catch { /* ignore */ }
}

const handleResize = () => { appChart?.resize() }

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([fetchStats(), fetchRecentApplications()])
    initApplicationChart()
    await loadActivityFeed()
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (appChart) appChart.dispose()
})
</script>

<style scoped>
@import './dashboard-shared.css';

.status-pill.s0 { background: var(--gray-100); color: var(--gray-600); }
.status-pill.s1 { background: var(--primary-50); color: var(--primary-600); }
.status-pill.s2 { background: var(--success-50); color: var(--success-600); }
.status-pill.s3 { background: var(--success-50); color: var(--success-600); }
.status-pill.s4 { background: var(--danger-50); color: var(--danger-600); }

/* 待办事项 */
.pending-tasks {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.task-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.task-item:hover {
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.task-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-label {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.task-count {
  font-size: var(--text-xs);
  color: var(--gray-500);
}

/* 招聘漏斗 */
.recruitment-funnel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.funnel-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.funnel-bar {
  height: 20px;
  background: var(--gray-100);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.funnel-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.6s ease;
}

.funnel-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.funnel-label {
  font-size: var(--text-xs);
  color: var(--gray-700);
}

.funnel-count {
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
}

/* 职位表现 */
.job-performance {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.performance-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.performance-item:hover {
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.performance-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.performance-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.performance-stats {
  display: flex;
  gap: var(--space-3);
}

.performance-stats .stat {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--gray-500);
}

/* 招聘小贴士 */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.tip-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tip-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tip-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.tip-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
}
</style>
