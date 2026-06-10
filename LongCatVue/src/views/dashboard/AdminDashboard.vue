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
        <!-- 近 7 日数据趋势 -->
        <div class="dash-card">
          <div class="card-title">
            <span>近 7 日数据趋势</span>
          </div>
          <!-- 汇总数据 -->
          <div class="trend-summary">
            <div class="summary-item" v-for="item in trendSummary" :key="item.label">
              <span class="summary-val" :style="{ color: item.color }">{{ item.total }}</span>
              <span class="summary-label">{{ item.label }}</span>
            </div>
          </div>
          <div ref="adminChartRef" class="chart-box"></div>
          <div class="trend-note">柱状图：新增用户/简历/职位（左轴） · 折线：有效匹配（右轴）</div>
        </div>

        <!-- 系统概况 -->
        <div class="dash-card">
          <div class="card-title"><span>系统概况</span></div>
          <div class="overview-grid">
            <div class="overview-cell clickable" @click="$router.push('/admin/users')">
              <span class="overview-val">{{ stats.userCount }}</span>
              <span class="overview-key">用户总数</span>
            </div>
            <div class="overview-cell clickable" @click="$router.push('/admin/resumes')">
              <span class="overview-val">{{ stats.resumeCount }}</span>
              <span class="overview-key">简历总数</span>
            </div>
            <div class="overview-cell clickable" @click="$router.push('/admin/jobs')">
              <span class="overview-val">{{ stats.jobCount }}</span>
              <span class="overview-key">在线职位</span>
            </div>
            <div class="overview-cell">
              <span class="overview-val">{{ stats.matchCount }}</span>
              <span class="overview-key">有效匹配</span>
            </div>
          </div>
        </div>

        <!-- 待审核列表 -->
        <div class="dash-card">
          <div class="card-title">
            <span>待审核</span>
            <button class="link-btn" @click="$router.push('/admin/company-verify')">查看全部</button>
          </div>
          <div class="pending-review-list">
            <div v-for="item in pendingReviews" :key="item.id" class="review-item" @click="$router.push(item.route)">
              <div class="review-icon" :style="{ background: item.bgColor }">
                <el-icon :size="14" :color="item.color"><component :is="item.icon" /></el-icon>
              </div>
              <div class="review-info">
                <span class="review-title">{{ item.title }}</span>
                <span class="review-desc">{{ item.desc }}</span>
              </div>
              <span class="review-count">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <!-- 系统公告 -->
        <div class="dash-card">
          <div class="card-title"><span>系统公告</span></div>
          <div class="announcements-list">
            <div v-for="item in announcements" :key="item.id" class="announcement-item">
              <el-icon class="announcement-icon" :color="item.color"><component :is="item.icon" /></el-icon>
              <div class="announcement-info">
                <span class="announcement-title">{{ item.title }}</span>
                <span class="announcement-desc">{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="dash-right">
        <!-- 今日数据 -->
        <div class="dash-card">
          <div class="card-title"><span>今日数据</span></div>
          <div class="platform-stats">
            <div class="platform-stat-item">
              <span class="platform-num" style="color: #6366f1;">{{ stats.todayUsers || 0 }}</span>
              <span class="platform-label">新增用户</span>
            </div>
            <div class="platform-stat-item">
              <span class="platform-num" style="color: #10b981;">{{ stats.todayResumes || 0 }}</span>
              <span class="platform-label">新增简历</span>
            </div>
            <div class="platform-stat-item">
              <span class="platform-num" style="color: #f59e0b;">{{ stats.todayJobs || 0 }}</span>
              <span class="platform-label">新增职位</span>
            </div>
            <div class="platform-stat-item">
              <span class="platform-num" style="color: #409eff;">{{ stats.todayMatches || 0 }}</span>
              <span class="platform-label">今日有效匹配</span>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="dash-card">
          <div class="card-title"><span>快捷操作</span></div>
          <div class="shortcut-grid">
            <button class="shortcut-item" @click="$router.push('/admin/users')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg></span>
              <span>用户管理</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/operation-logs')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></span>
              <span>操作日志</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/profile')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
              <span>个人中心</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/data-analysis')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/></svg></span>
              <span>数据分析</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/admin/manage-keywords')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg></span>
              <span>关键词管理</span>
            </button>
          </div>
        </div>

        <!-- 最近注册用户 -->
        <div class="dash-card">
          <div class="card-title">
            <span>最近注册</span>
            <button class="link-btn" @click="$router.push('/admin/users')">查看全部</button>
          </div>
          <div v-if="recentUsers.length === 0" class="empty-state empty-sm">
            <p>暂无新用户</p>
          </div>
          <div v-else class="activity-list">
            <div v-for="(u, idx) in recentUsers" :key="idx" class="activity-item">
              <div class="activity-icon" style="background: var(--primary-50); color: var(--primary-500)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <div class="activity-main">
                <span class="activity-title">{{ u.username || u.phone || '新用户' }} <span class="user-type-tag">{{ getUserTypeLabel(u.userType) }}</span></span>
                <span class="activity-time">{{ formatActivityTime(u.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 系统健康 -->
        <div class="dash-card">
          <div class="card-title"><span>系统状态</span></div>
          <div class="data-rows">
            <div class="data-row">
              <span class="data-label">数据库</span>
              <span class="data-value" style="color: var(--success-500)">正常</span>
            </div>
            <div class="data-row">
              <span class="data-label">缓存</span>
              <span class="data-value" style="color: var(--success-500)">正常</span>
            </div>
            <div class="data-row">
              <span class="data-label">文件存储</span>
              <span class="data-value" style="color: var(--success-500)">正常</span>
            </div>
            <div class="data-row">
              <span class="data-label">AI 服务</span>
              <span class="data-value" style="color: var(--success-500)">正常</span>
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

const router = useRouter()
const userStore = useUserStore()

const stats = ref({ userCount: 0, resumeCount: 0, jobCount: 0, matchCount: 0, pendingCompanyVerifies: 0, pendingReports: 0 })
const recentUsers = ref([])
const adminChartRef = ref(null)
let adminChart = null
const loading = ref(false)

const displayStats = computed(() => [
  { label: '用户总数', value: stats.value.userCount, route: '/admin/users' },
  { label: '简历总数', value: stats.value.resumeCount, route: '/admin/resumes' },
  { label: '职位总数', value: stats.value.jobCount, route: '/admin/jobs' },
  { label: '有效匹配', value: stats.value.matchCount, route: null }
])


// 待审核列表
const pendingReviews = computed(() => [
  { id: 1, title: '企业认证审核', desc: `${stats.value.pendingCompanyVerifies || 0}家企业等待认证审核`, icon: 'OfficeBuilding', color: '#6366f1', bgColor: 'rgba(99,102,241,0.1)', count: stats.value.pendingCompanyVerifies || 0, route: '/admin/company-verify' },
  { id: 2, title: '职位审核', desc: '暂无待审核职位', icon: 'Document', color: '#f59e0b', bgColor: 'rgba(245,158,11,0.1)', count: 0, route: '/admin/jobs' },
  { id: 3, title: '举报处理', desc: `${stats.value.pendingReports || 0}条举报等待处理`, icon: 'Warning', color: '#ef4444', bgColor: 'rgba(239,68,68,0.1)', count: stats.value.pendingReports || 0, route: '/admin/report-manage' }
])

// 系统公告
const announcements = [
  { id: 1, title: '系统升级通知', desc: '本周六凌晨将进行系统维护升级', icon: 'InfoFilled', color: '#6366f1' },
  { id: 2, title: '新功能上线', desc: '智能匹配算法已优化升级', icon: 'Trophy', color: '#10b981' },
  { id: 3, title: '安全提醒', desc: '请定期修改密码保障账户安全', icon: 'Warning', color: '#f59e0b' }
]

const getUserTypeLabel = (t) => ({ JOB_SEEKER: '求职者', EMPLOYER: '企业', ADMIN: '管理员' }[t] || '用户')

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

const trendSummary = ref([])

const initAdminChart = async () => {
  await nextTick()
  if (!adminChartRef.value) return
  if (adminChart) { adminChart.dispose(); adminChart = null }

  let dates = getRecentDates(7)
  let seriesData = {
    users: [5, 8, 3, 6, 9, 4, 7],
    resumes: [3, 5, 2, 4, 6, 3, 5],
    jobs: [2, 3, 1, 4, 2, 3, 2],
    matches: [12, 19, 15, 22, 18, 25, 20]
  }

  try {
    const res = await request.get('/admin/stats/trends', { params: { days: 7 }, skipErrorNotification: true })
    const trends = res?.trends || []
    if (trends.length) {
      dates = trends.map(t => t.date)
      seriesData.users = trends.map(t => t.users || 0)
      seriesData.resumes = trends.map(t => t.resumes || 0)
      seriesData.jobs = trends.map(t => t.jobs || 0)
      seriesData.matches = trends.map(t => t.matches || 0)
    }
  } catch { /* use fallback */ }

  // 汇总数据
  const summaryItems = [
    { label: '新用户', color: '#6366f1', key: 'users' },
    { label: '新简历', color: '#f59e0b', key: 'resumes' },
    { label: '新职位', color: '#10b981', key: 'jobs' },
    { label: '有效匹配', color: '#409eff', key: 'matches' }
  ]
  trendSummary.value = summaryItems.map(item => ({
    label: item.label,
    color: item.color,
    total: seriesData[item.key].reduce((a, b) => a + b, 0)
  }))

  const chart = echarts.init(adminChartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333', fontSize: 12 },
      formatter: (params) => {
        let html = `<div style="font-weight:600;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0">
            <span style="width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span>${p.seriesName}：<b>${p.value}</b></span></div>`
        })
        return html
      }
    },
    legend: {
      data: ['新用户', '新简历', '新职位', '有效匹配'],
      bottom: 0,
      textStyle: { fontSize: 11, color: '#8C8C8C' },
      itemWidth: 12, itemHeight: 8
    },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '8%', containLabel: true },
    xAxis: {
      type: 'category', data: dates,
      axisLine: { lineStyle: { color: '#E8E8E8' } },
      axisLabel: { color: '#8C8C8C', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value', name: '新增数', nameTextStyle: { color: '#8C8C8C', fontSize: 11 },
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { lineStyle: { color: '#F5F5F5' } },
        axisLabel: { color: '#8C8C8C', fontSize: 11 }
      },
      {
        type: 'value', name: '匹配数', nameTextStyle: { color: '#8C8C8C', fontSize: 11 },
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: '#8C8C8C', fontSize: 11 }
      }
    ],
    series: [
      {
        name: '新用户', type: 'bar', barWidth: 14,
        itemStyle: { color: '#6366f1', borderRadius: [3, 3, 0, 0] },
        data: seriesData.users
      },
      {
        name: '新简历', type: 'bar', barWidth: 14,
        itemStyle: { color: '#f59e0b', borderRadius: [3, 3, 0, 0] },
        data: seriesData.resumes
      },
      {
        name: '新职位', type: 'bar', barWidth: 14,
        itemStyle: { color: '#10b981', borderRadius: [3, 3, 0, 0] },
        data: seriesData.jobs
      },
      {
        name: '有效匹配', type: 'line', yAxisIndex: 1,
        smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2.5, color: '#409eff' },
        itemStyle: { color: '#409eff', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: '#409eff20' }, { offset: 1, color: '#409eff00' }]
          }
        },
        data: seriesData.matches
      }
    ]
  })
  adminChart = chart
  setTimeout(() => adminChart?.resize(), 50)
}

const fetchStats = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const [profileRes, adminRes] = await Promise.all([
      request.get('/user/profile/stats', { skipErrorNotification: true }).catch(() => null),
      request.get('/admin/stats', { skipErrorNotification: true }).catch(() => null)
    ])
    if (profileRes) {
      stats.value.userCount = profileRes.count3 || 0
      stats.value.resumeCount = profileRes.count2 || 0
      stats.value.jobCount = profileRes.count1 || 0
      stats.value.matchCount = profileRes.count4 || 0
    }
    if (adminRes) {
      stats.value.todayUsers = adminRes.todayUsers || 0
      stats.value.todayResumes = adminRes.todayResumes || 0
      stats.value.todayJobs = adminRes.todayJobs || 0
      stats.value.todayMatches = adminRes.todayMatches || 0
      stats.value.pendingCompanyVerifies = adminRes.pendingCompanyVerifies || 0
      stats.value.pendingReports = adminRes.pendingReports || 0
    }
  } catch (error) {
    console.debug('获取统计数据失败:', error?.message)
  }
}

const fetchRecentUsers = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await request.get('/user/list', { params: { page: 1, size: 5 }, skipErrorNotification: true })
    if (Array.isArray(res)) {
      recentUsers.value = res.slice(0, 5)
    } else if (res?.records) {
      recentUsers.value = res.records.slice(0, 5)
    }
  } catch { recentUsers.value = [] }
}

const handleResize = () => { adminChart?.resize() }

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([fetchStats(), fetchRecentUsers()])
    initAdminChart()
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (adminChart) adminChart.dispose()
})
</script>

<style scoped>
@import './dashboard-shared.css';

/* ── 趋势汇总 ─────────────────────────────── */
.trend-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.summary-item {
  text-align: center;
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.summary-val {
  display: block;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.summary-label {
  font-size: 11px;
  color: var(--gray-500);
  margin-top: 2px;
  display: block;
}

.trend-note {
  font-size: 11px;
  color: var(--gray-400);
  text-align: center;
  margin-top: var(--space-2);
}

.user-type-tag {
  font-size: var(--text-xs);
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--gray-100);
  color: var(--gray-500);
  margin-left: 4px;
  vertical-align: middle;
}

/* 今日数据 */
.platform-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.platform-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-4) var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  text-align: center;
}

.platform-num {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.platform-label {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 4px;
}

/* 待审核列表 */
.pending-review-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.review-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.review-item:hover {
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.review-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.review-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.review-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
}

.review-count {
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  color: var(--primary-500);
  background: var(--primary-50);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

/* 系统公告 */
.announcements-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.announcement-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.announcement-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.announcement-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.announcement-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.announcement-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
  line-height: 1.4;
}
</style>
