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
        <!-- 投递状态分布图 -->
        <div class="dash-card">
          <div class="card-title"><span>投递状态分布</span></div>
          <div v-if="stats.applicationCount > 0" ref="appPieChartRef" class="chart-box chart-sm"></div>
          <div v-else class="empty-state">
            <div class="empty-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            </div>
            <p>投递后查看状态分布</p>
          </div>
        </div>

        <!-- 最近投递 -->
        <div class="dash-card">
          <div class="card-title">
            <span>最近投递</span>
            <button class="link-btn" @click="$router.push('/my-applications')">查看全部</button>
          </div>
          <div v-if="recentApplications.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <p>还没有投递记录</p>
            <el-button type="primary" plain size="small" @click="$router.push('/browse-jobs')">去浏览职位</el-button>
          </div>
          <div v-else class="timeline-list">
            <div v-for="item in recentApplications" :key="item.id" class="timeline-item" @click="$router.push('/my-applications')">
              <div class="timeline-dot" :class="'s' + item.status"></div>
              <div class="timeline-main">
                <span class="timeline-title">{{ item.jobTitle || item.job_title || '未知职位' }}</span>
                <span class="timeline-date">{{ formatDate(item.createTime || item.create_time) }}</span>
              </div>
              <span class="status-pill" :class="'s' + item.status">{{ getStatusText(item.status) }}</span>
            </div>
          </div>
        </div>

        <!-- 面试安排 -->
        <div class="dash-card">
          <div class="card-title">
            <span>面试安排</span>
            <button class="link-btn" @click="$router.push('/my-interviews')">查看全部</button>
          </div>
          <div v-if="recentInterviews.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <p>暂无面试安排</p>
          </div>
          <div v-else class="timeline-list">
            <div v-for="item in recentInterviews" :key="item.id" class="timeline-item" @click="$router.push('/my-interviews')">
              <div class="timeline-dot s2"></div>
              <div class="timeline-main">
                <span class="timeline-title">{{ item.job_title || '未知职位' }}</span>
                <span class="timeline-date">{{ formatDateTime(item.interview_time) }}</span>
              </div>
              <span class="status-pill" :class="'s' + (item.status || 0)">{{ getInterviewStatusText(item.status) }}</span>
            </div>
          </div>
        </div>

        <!-- 热门推荐职位 -->
        <div class="dash-card">
          <div class="card-title">
            <span>热门推荐</span>
            <button class="link-btn" @click="$router.push('/browse-jobs')">查看更多</button>
          </div>
          <div class="hot-jobs-list">
            <div v-for="job in hotJobs" :key="job.id" class="hot-job-item" @click="$router.push('/browse-jobs')">
              <div class="hot-job-info">
                <span class="hot-job-title">{{ job.title }}</span>
                <span class="hot-job-company">{{ job.company }}</span>
              </div>
              <span class="hot-job-salary">{{ job.salary }}</span>
            </div>
          </div>
        </div>

        <!-- 求职进度 -->
        <div class="dash-card">
          <div class="card-title"><span>求职进度</span></div>
          <div class="progress-steps">
            <div v-for="(step, idx) in jobSearchProgress" :key="idx" class="step-item" :class="{ active: step.completed, current: step.current }">
              <div class="step-dot">
                <svg v-if="step.completed" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                <span v-else>{{ idx + 1 }}</span>
              </div>
              <div class="step-content">
                <span class="step-label">{{ step.label }}</span>
                <span class="step-desc">{{ step.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <div class="dash-right">
        <!-- 能力图谱入口 -->
        <div class="dash-card card-highlight">
          <div class="highlight-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v2"/><path d="M12 21v2"/><path d="M4.22 4.22l1.42 1.42"/><path d="M18.36 18.36l1.42 1.42"/><path d="M1 12h2"/><path d="M21 12h2"/><path d="M4.22 19.78l1.42-1.42"/><path d="M18.36 5.64l1.42-1.42"/></svg>
          </div>
          <h4>能力图谱</h4>
          <p>可视化展示您的技能分布，发现提升方向</p>
          <el-button type="primary" plain size="small" @click="$router.push('/skill-graph')">立即查看</el-button>
        </div>

        <!-- 简历完善度 -->
        <div class="dash-card">
          <div class="card-title"><span>简历完善度</span></div>
          <div class="profile-completion">
            <el-progress type="circle" :percentage="profileCompletion" :width="80" :stroke-width="8" color="var(--primary-500)" />
            <div class="completion-tips">
              <p>{{ profileTip }}</p>
              <el-button type="primary" size="small" plain @click="$router.push('/my-resume')">完善简历</el-button>
            </div>
          </div>
        </div>

        <!-- 求职小贴士 -->
        <div class="dash-card">
          <div class="card-title"><span>求职小贴士</span></div>
          <div class="tips-list">
            <div v-for="tip in jobTips" :key="tip.id" class="tip-item">
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

        <!-- 快捷操作 -->
        <div class="dash-card">
          <div class="card-title"><span>快捷操作</span></div>
          <div class="shortcut-grid">
            <button class="shortcut-item" @click="$router.push('/browse-jobs')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
              <span>搜索职位</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/my-resume')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg></span>
              <span>编辑简历</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/skill-graph')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v2"/><path d="M12 21v2"/></svg></span>
              <span>能力图谱</span>
            </button>
            <button class="shortcut-item" @click="$router.push('/match')">
              <span class="shortcut-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg></span>
              <span>智能匹配</span>
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
import { ref, computed, onMounted, nextTick, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import * as echarts from 'echarts'
import { Clock, Document, Calendar, InfoFilled, EditPen, Star, Refresh, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({ applicationCount: 0, interviewCount: 0, jobCount: 0, favoriteCount: 0 })
const recentApplications = ref([])
const recentInterviews = ref([])
const activityFeed = ref([])
const appPieChartRef = ref(null)
let pieChart = null

const loading = ref(false)

const displayStats = computed(() => [
  { label: '我的投递', value: stats.value.applicationCount || 0, route: '/my-applications' },
  { label: '面试邀请', value: stats.value.interviewCount || 0, route: '/my-interviews' },
  { label: '在线职位', value: stats.value.jobCount, route: '/browse-jobs' },
  { label: '收藏职位', value: stats.value.favoriteCount || 0, route: '/favorites' }
])

// 简历完善度
const resumeData = ref(null)

const profileCompletion = computed(() => {
  const r = resumeData.value
  if (!r) return 0
  let score = 0
  // 基本信息（30分）
  if (r.name) score += 10
  if (r.age != null) score += 5
  if (r.education) score += 10
  if (r.expectedSalary != null) score += 5
  // 技能（20分）
  if (r.skills && r.skills !== '[]') score += 20
  // 经验（25分）
  if (r.experience) score += 10
  if (r.workExperience && r.workExperience.length > 50) score += 15
  // 自我评价（15分）
  if (r.selfIntroduction && r.selfIntroduction.length > 30) score += 15
  // 分类（10分）
  if (r.categoryId != null) score += 10
  return Math.min(100, score)
})

const profileTip = computed(() => {
  const c = profileCompletion.value
  if (c === 0) return '请先创建或上传简历'
  if (c < 30) return '简历完善度较低，建议补充基本信息'
  if (c < 60) return '继续完善技能和项目经验'
  if (c < 90) return '简历已较为完善，可投递心仪职位'
  return '简历完善度很高，保持更新即可'
})

// 求职小贴士
const jobTips = [
  { id: 1, title: '优化简历标题', desc: '使用具体职位名称', icon: EditPen, color: '#6366f1', bgColor: 'rgba(99,102,241,0.1)' },
  { id: 2, title: '突出核心技能', desc: '展示与职位匹配的技能', icon: Star, color: '#f59e0b', bgColor: 'rgba(245,158,11,0.1)' },
  { id: 3, title: '定期更新简历', desc: '保持简历新鲜度', icon: Refresh, color: '#10b981', bgColor: 'rgba(16,185,129,0.1)' },
  { id: 4, title: '关注面试反馈', desc: '及时查看面试结果', icon: ChatDotRound, color: '#8b5cf6', bgColor: 'rgba(139,92,246,0.1)' }
]

// 热门推荐职位
const hotJobs = [
  { id: 1, title: '前端开发工程师', company: '科技有限公司', salary: '15-25K' },
  { id: 2, title: 'Java后端开发', company: '互联网公司', salary: '18-30K' },
  { id: 3, title: '产品经理', company: '创新科技', salary: '20-35K' },
  { id: 4, title: 'UI设计师', company: '设计工作室', salary: '12-20K' },
  { id: 5, title: '数据分析师', company: '数据科技', salary: '16-28K' }
]

// 求职进度（响应式，数据加载后自动更新）
const jobSearchProgress = computed(() => {
  const s = stats.value
  const resumeOk = profileCompletion.value >= 60
  const hasApplied = s.applicationCount > 0
  const hasInterview = s.interviewCount > 0
  const hasHired = recentApplications.value.some(a => a.status === 3 || a.status === 4)
  return [
    { label: '完善简历', desc: `简历完善度 ${profileCompletion.value}%`, completed: resumeOk, current: !resumeOk },
    { label: '投递职位', desc: hasApplied ? `已投递 ${s.applicationCount} 个职位` : '浏览并投递心仪职位', completed: hasApplied, current: resumeOk && !hasApplied },
    { label: '面试邀约', desc: hasInterview ? `已收到 ${s.interviewCount} 个面试邀请` : '等待企业邀请面试', completed: hasInterview, current: hasApplied && !hasInterview },
    { label: '拿到Offer', desc: hasHired ? '恭喜收到录用通知！' : '成功获得工作机会', completed: hasHired, current: hasInterview && !hasHired }
  ]
})

const getStatusText = (s) => ['待处理', '已查看', '邀请面试', '已录用', '已拒绝'][s] || '未知'
const getInterviewStatusText = (s) => ['待确认', '已确认', '已取消', '已完成'][s] || '未知'
const formatDate = (d) => d ? new Date(d).toLocaleDateString('zh-CN') : ''
const formatDateTime = (d) => d ? new Date(d).toLocaleString('zh-CN') : '待定'

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

const initPieChart = () => {
  if (!appPieChartRef.value) return
  if (pieChart) { pieChart.dispose(); pieChart = null }
  const cnt = [0, 0, 0, 0, 0]
  recentApplications.value.forEach(a => { if (a.status >= 0 && a.status <= 4) cnt[a.status]++ })
  const data = [
    { value: cnt[0], name: '待处理', itemStyle: { color: '#8C8C8C' } },
    { value: cnt[1], name: '已查看', itemStyle: { color: '#69B1FF' } },
    { value: cnt[2], name: '面试', itemStyle: { color: '#73D13D' } },
    { value: cnt[3], name: '已录用', itemStyle: { color: '#389E0D' } },
    { value: cnt[4], name: '已拒绝', itemStyle: { color: '#FF4D4F' } }
  ].filter(d => d.value > 0)
  if (!data.length) return
  pieChart = echarts.init(appPieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#8C8C8C' } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '42%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#ffffff', borderWidth: 2 },
      label: { show: false }, data
    }]
  })
  setTimeout(() => pieChart?.resize(), 50)
}

const loadActivityFeed = async () => {
  if (!userStore.isLoggedIn) return
  const activities = []
  try {
    const [appsRes, interviewsRes] = await Promise.allSettled([
      request.get('/application/my', { params: { page: 1, size: 3 }, skipErrorNotification: true }),
      request.get('/interview/my', { params: { page: 1, size: 3 }, skipErrorNotification: true })
    ])
    const apps = appsRes.status === 'fulfilled' && Array.isArray(appsRes.value) ? appsRes.value : []
    const interviews = interviewsRes.status === 'fulfilled' && Array.isArray(interviewsRes.value) ? interviewsRes.value : []

    apps.forEach(a => {
      activities.push({ type: 'application', icon: 'Document', color: 'var(--primary-500)', title: `投递了「${a.job_title || '未知职位'}」`, time: a.create_time })
    })
    interviews.forEach(i => {
      activities.push({ type: 'interview', icon: 'Calendar', color: 'var(--success-500)', title: `面试邀请：${i.job_title || '未知职位'}`, time: i.create_time })
    })

    activities.sort((a, b) => new Date(b.time || 0) - new Date(a.time || 0))
    activityFeed.value = activities.slice(0, 8)
  } catch { activityFeed.value = [] }
}

const fetchData = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const [apps, interviews] = await Promise.allSettled([
      request.get('/application/my', { skipErrorNotification: true }),
      request.get('/interview/my', { skipErrorNotification: true })
    ])
    recentApplications.value = (apps.status === 'fulfilled' && Array.isArray(apps.value) ? apps.value : []).slice(0, 5)
    stats.value.applicationCount = apps.status === 'fulfilled' && Array.isArray(apps.value) ? apps.value.length : 0
    recentInterviews.value = (interviews.status === 'fulfilled' && Array.isArray(interviews.value) ? interviews.value : []).slice(0, 5)
    stats.value.interviewCount = interviews.status === 'fulfilled' && Array.isArray(interviews.value) ? interviews.value.length : 0
    try {
      const favRes = await request.get('/favorite/jobs', { skipErrorNotification: true })
      stats.value.favoriteCount = Array.isArray(favRes) ? favRes.length : 0
    } catch { stats.value.favoriteCount = 0 }
    try {
      const jobRes = await request.get('/job', { params: { page: 1, size: 1 }, skipErrorNotification: true })
      stats.value.jobCount = jobRes?.total || 0
    } catch { /* ignore */ }
    // 获取简历数据计算完善度
    try {
      const resumeRes = await request.get('/resume/my', { skipErrorNotification: true })
      resumeData.value = resumeRes || null
    } catch { resumeData.value = null }
    await loadActivityFeed()
  } catch (error) {
    console.debug('加载仪表盘数据失败:', error?.message)
  }
}

const handleResize = () => { pieChart?.resize() }

onMounted(async () => {
  loading.value = true
  try {
    await fetchData()
    nextTick(() => nextTick(() => { if (recentApplications.value.length) initPieChart() }))
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', handleResize)
})

watch(recentApplications, () => {
  if (recentApplications.value.length) nextTick(() => initPieChart())
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (pieChart) pieChart.dispose()
})
</script>

<style scoped>
@import './dashboard-shared.css';

.status-pill.s0 { background: var(--gray-100); color: var(--gray-600); }
.status-pill.s1 { background: var(--primary-50); color: var(--primary-600); }
.status-pill.s2 { background: var(--success-50); color: var(--success-600); }
.status-pill.s3 { background: var(--success-50); color: var(--success-600); }
.status-pill.s4 { background: var(--danger-50); color: var(--danger-600); }

/* 简历完善度 */
.profile-completion {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.completion-tips {
  flex: 1;
}

.completion-tips p {
  font-size: var(--text-xs);
  color: var(--gray-500);
  margin: 0 0 var(--space-3);
  line-height: 1.5;
}

/* 求职小贴士 */
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

/* 热门推荐职位 */
.hot-jobs-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.hot-job-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.hot-job-item:hover {
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.hot-job-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hot-job-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.hot-job-company {
  font-size: var(--text-xs);
  color: var(--gray-500);
}

.hot-job-salary {
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  color: #ef4444;
  white-space: nowrap;
}

/* 求职进度 */
.progress-steps {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  position: relative;
}

.step-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 13px;
  top: 28px;
  bottom: -16px;
  width: 2px;
  background: var(--gray-200);
}

.step-item.active:not(:last-child)::after {
  background: var(--primary-300);
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-500);
  z-index: 1;
}

.step-item.active .step-dot {
  background: var(--primary-500);
  color: white;
}

.step-item.current .step-dot {
  background: var(--primary-100);
  color: var(--primary-600);
  box-shadow: 0 0 0 3px var(--primary-50);
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-label {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.step-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
}
</style>
