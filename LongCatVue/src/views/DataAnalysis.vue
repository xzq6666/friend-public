<template>
  <div class="page-container-full">
    <div class="page-header-row">
      <h2>数据分析仪表盘</h2>
      <div class="header-actions">
        <el-radio-group v-model="timeRange" size="small" @change="fetchTrendData">
          <el-radio-button value="7">7天</el-radio-button>
          <el-radio-button value="30">30天</el-radio-button>
          <el-radio-button value="90">90天</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
              <div class="stat-change">
                <span class="today-label">今日新增: </span>
                <span class="today-value">{{ stats.todayUsers }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon resume-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalResumes }}</div>
              <div class="stat-label">总简历数</div>
              <div class="stat-change">
                <span class="today-label">今日新增: </span>
                <span class="today-value">{{ stats.todayResumes }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon job-icon">
              <el-icon><Briefcase /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalJobs }}</div>
              <div class="stat-label">总职位数</div>
              <div class="stat-change">
                <span class="today-label">今日新增: </span>
                <span class="today-value">{{ stats.todayJobs }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon match-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalMatches }}</div>
              <div class="stat-label">总匹配数</div>
              <div class="stat-change">
                <span class="today-label">今日新增: </span>
                <span class="today-value">{{ stats.todayMatches }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 趋势图 -->
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <div class="chart-header">
            <h3>数据趋势</h3>
            <span class="chart-period">{{ trendData.period }}</span>
          </div>
          <div class="chart-container">
            <div ref="trendChartRef" class="chart"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 用户分布 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <div class="chart-header">
            <h3>用户分布</h3>
          </div>
          <div class="chart-container">
            <div ref="userChartRef" class="chart"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细信息 -->
    <el-row :gutter="20" class="details-section">
      <!-- 热门技能 -->
      <el-col :span="12">
        <el-card shadow="never" class="detail-card">
          <div class="detail-header">
            <h3>热门技能排行</h3>
          </div>
          <div class="skill-list">
            <div v-for="(skill, index) in topSkills" :key="skill.skill" class="skill-item">
              <div class="skill-rank">
                <span :class="['rank-badge', getRankClass(index)]">{{ index + 1 }}</span>
              </div>
              <div class="skill-info">
                <span class="skill-name">{{ skill.skill }}</span>
                <span class="skill-count">{{ skill.count }} 次</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 热门职位 -->
      <el-col :span="12">
        <el-card shadow="never" class="detail-card">
          <div class="detail-header">
            <h3>热门职位</h3>
          </div>
          <div class="job-list">
            <div v-for="job in popularJobs" :key="job.id" class="job-item">
              <div class="job-title">{{ job.title }}</div>
              <div class="job-company">企业ID: {{ job.company }}</div>
              <div class="job-stats">
                <el-tag size="small" type="info">{{ job.matchCount || 0 }} 匹配</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onUnmounted } from 'vue'
import { User, Document, Briefcase, Connection } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ErrorHandler } from '../utils/errorHandler'
import * as echarts from 'echarts'
import type { StatsData, TrendData, SkillData, JobData } from '../types'

const stats = ref<StatsData>({
  totalUsers: 0,
  totalResumes: 0,
  totalJobs: 0,
  totalMatches: 0,
  todayUsers: 0,
  todayResumes: 0,
  todayJobs: 0,
  todayMatches: 0,
  activeUsers7d: 0
})

const trendData = ref<TrendData>({
  trends: [],
  period: '7天'
})

const topSkills = ref<SkillData[]>([])
const popularJobs = ref<JobData[]>([])
const timeRange = ref('7')

const trendChartRef = ref<HTMLElement>()
const userChartRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let userChart: echarts.ECharts | null = null

// 获取统计数据
const fetchStats = async () => {
  try {
    const data = await request.get('/admin/stats/detailed')
    stats.value = data
  } catch (error) {
    ErrorHandler.handle(error, 'silent')
  }
}

// 获取趋势数据
const fetchTrendData = async () => {
  try {
    const data = await request.get(`/admin/stats/trends?days=${timeRange.value}`)
    trendData.value = data
    await nextTick()
    initTrendChart()
  } catch (error) {
    ErrorHandler.handle(error, 'silent')
  }
}

// 获取热门技能
const fetchTopSkills = async () => {
  try {
    const data = await request.get('/admin/stats/top-skills')
    topSkills.value = data
  } catch (error) {
    ErrorHandler.handle(error, 'silent')
  }
}

// 获取热门职位
const fetchPopularJobs = async () => {
  try {
    const data = await request.get('/admin/stats/popular-jobs')
    popularJobs.value = data
  } catch (error) {
    ErrorHandler.handle(error, 'silent')
  }
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['用户', '简历', '职位', '匹配']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.trends.map(item => item.date)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '用户',
        type: 'line',
        stack: '总量',
        areaStyle: { opacity: 0.3 },
        emphasis: {
          focus: 'series'
        },
        data: trendData.value.trends.map(item => item.users)
      },
      {
        name: '简历',
        type: 'line',
        stack: '总量',
        areaStyle: { opacity: 0.3 },
        emphasis: {
          focus: 'series'
        },
        data: trendData.value.trends.map(item => item.resumes)
      },
      {
        name: '职位',
        type: 'line',
        stack: '总量',
        areaStyle: { opacity: 0.3 },
        emphasis: {
          focus: 'series'
        },
        data: trendData.value.trends.map(item => item.jobs)
      },
      {
        name: '匹配',
        type: 'line',
        stack: '总量',
        areaStyle: { opacity: 0.3 },
        emphasis: {
          focus: 'series'
        },
        data: trendData.value.trends.map(item => item.matches)
      }
    ]
  }

  trendChart.setOption(option)
}

// 初始化用户分布图表
const initUserChart = () => {
  if (!userChartRef.value) return

  if (userChart) {
    userChart.dispose()
  }

  userChart = echarts.init(userChartRef.value)

  const userData = [
    { value: stats.value.userTypeDistribution?.admin || 0, name: '管理员' },
    { value: stats.value.userTypeDistribution?.employer || 0, name: '企业用户' },
    { value: stats.value.userTypeDistribution?.employee || 0, name: '求职者' }
  ]

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '用户分布',
        type: 'pie',
        radius: '50%',
        data: userData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  userChart.setOption(option)
}

// 获取排名样式
const getRankClass = (index: number) => {
  if (index === 0) return 'rank-first'
  if (index === 1) return 'rank-second'
  if (index === 2) return 'rank-third'
  return 'rank-normal'
}

// 初始化
const init = async () => {
  await Promise.all([
    fetchStats(),
    fetchTrendData(),
    fetchTopSkills(),
    fetchPopularJobs()
  ])

  await nextTick()
  initUserChart()
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  trendChart?.resize()
  userChart?.resize()
}

onMounted(() => {
  init()
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  userChart?.dispose()
})
</script>

<style scoped>
/* Uses page-container-full utility class from layout-utilities.css */

/* ── 统计卡片 ─────────────────────────────── */
.stats-cards {
  margin-bottom: var(--space-6);
}

.stat-card {
  min-height: 120px;
  height: auto;
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border) !important;
  transition: all var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  box-shadow: var(--shadow-md) !important;
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  height: auto;
  padding: var(--space-4);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: var(--space-4);
  color: var(--color-surface);
  font-size: 20px;
  background: var(--gray-900);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  line-height: var(--leading-tight);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  margin: var(--space-1) 0;
}

.stat-change {
  display: flex;
  align-items: center;
  font-size: var(--text-xs);
}

.today-label {
  color: var(--color-text-muted);
}

.today-value {
  color: var(--success-600);
  font-weight: var(--weight-semibold);
  margin-left: var(--space-1);
}

/* ── 图表区域 ─────────────────────────────── */
.charts-section {
  margin-bottom: var(--space-6);
}

.chart-card {
  height: 400px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--gray-100);
}

.chart-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  color: var(--gray-900);
}

.chart-period {
  font-size: var(--text-xs);
  color: var(--color-text-sub);
}

.chart-container {
  padding: var(--space-5);
}

.chart {
  width: 100%;
  height: 320px;
}

/* ── 详情区域 ─────────────────────────────── */
.details-section {
  margin-bottom: var(--space-6);
}

.detail-card {
  height: 400px;
}

.detail-header {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--gray-100);
}

.detail-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  color: var(--gray-900);
}

.skill-list {
  padding: var(--space-4);
}

.skill-item {
  display: flex;
  align-items: center;
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--gray-50);
}

.skill-item:last-child {
  border-bottom: none;
}

.skill-rank {
  margin-right: var(--space-3);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: white;
}

.rank-first { background: var(--warning-500); }
.rank-second { background: var(--gray-500); }
.rank-third { background: var(--rank-bronze); }
.rank-normal { background: var(--gray-300); color: var(--gray-600); }

.skill-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skill-name {
  font-weight: var(--weight-medium);
  color: var(--gray-900);
}

.skill-count {
  font-size: var(--text-xs);
  color: var(--color-text-sub);
}

.job-list {
  padding: var(--space-4);
}

.job-item {
  padding: var(--space-3);
  border-bottom: 1px solid var(--gray-50);
}

.job-item:last-child {
  border-bottom: none;
}

.job-title {
  font-weight: var(--weight-medium);
  color: var(--gray-900);
  margin-bottom: var(--space-1);
}

.job-company {
  font-size: var(--text-xs);
  color: var(--color-text-sub);
  margin-bottom: var(--space-2);
}

.job-stats {
  display: flex;
  gap: var(--space-2);
}

/* ── 响应式 ───────────────────────────────── */
@media (max-width: 1200px) {
  .stats-cards .el-col {
    margin-bottom: var(--space-4);
  }

  .stat-card {
    height: auto;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-4);
  }

  .charts-section .el-col,
  .details-section .el-col {
    margin-bottom: var(--space-4);
  }

  .chart-card,
  .detail-card {
    height: 350px;
  }

  .chart {
    height: 280px;
  }
}
</style>