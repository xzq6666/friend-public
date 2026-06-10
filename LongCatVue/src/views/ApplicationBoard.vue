<template>
  <div class="application-board-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">求职进度看板</h2>
        <p class="page-subtitle">实时追踪您的求职进展，一目了然</p>
      </div>
      <el-button type="primary" @click="refreshData" :loading="loading" class="refresh-btn">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid" v-loading="loading">
      <div class="stat-card favorited">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.favorited || 0 }}</div>
          <div class="stat-label">已收藏</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">感兴趣职位</span>
        </div>
      </div>

      <div class="stat-card applied">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.applied || 0 }}</div>
          <div class="stat-label">已投递</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">等待反馈</span>
        </div>
      </div>

      <div class="stat-card screening">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><Search /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.screening || 0 }}</div>
          <div class="stat-label">初筛中</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">简历筛选</span>
        </div>
      </div>

      <div class="stat-card interviewing">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><VideoCamera /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.interviewing || 0 }}</div>
          <div class="stat-label">面试中</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">正在面试</span>
        </div>
      </div>

      <div class="stat-card hired">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.hired || 0 }}</div>
          <div class="stat-label">已录用</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">恭喜入职</span>
        </div>
      </div>

      <div class="stat-card rejected">
        <div class="stat-icon-wrapper">
          <div class="stat-icon-bg"></div>
          <el-icon :size="28"><CircleClose /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.rejected || 0 }}</div>
          <div class="stat-label">已拒绝</div>
        </div>
        <div class="stat-trend">
          <span class="trend-text">继续努力</span>
        </div>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-section">
      <div class="progress-header">
        <h3>求职进度</h3>
        <div class="progress-summary">
          <span class="progress-total">总计 {{ totalApplications }} 个职位</span>
          <span class="progress-stat" v-if="statistics.successRate && statistics.successRate !== '0%'">
            录用率 {{ statistics.successRate }}
          </span>
          <span class="progress-stat" v-if="statistics.avgResponseTime && statistics.avgResponseTime !== '-'">
            平均 {{ statistics.avgResponseTime }} 响应
          </span>
        </div>
      </div>
      <div class="progress-bar-container">
        <!-- Tooltip -->
        <div
          class="progress-tooltip"
          v-if="tooltipVisible && tooltipStage"
          :style="tooltipStyle"
        >
          <div class="tooltip-content">
            <span class="tooltip-label">{{ stageLabels[tooltipStage] }}</span>
            <span class="tooltip-value">{{ statistics[tooltipStage] || 0 }} 个</span>
            <span class="tooltip-percent">{{ getProgressPercent(tooltipStage) }}%</span>
          </div>
          <div class="tooltip-arrow"></div>
        </div>
        <div class="progress-bar" v-if="totalApplications > 0">
          <div
            class="progress-segment favorited-segment"
            :style="{ width: getProgressPercent('favorited') + '%' }"
            @mouseenter="showTooltip($event, 'favorited')"
            @mouseleave="hideTooltip"
          ></div>
          <div
            class="progress-segment applied-segment"
            :style="{ width: getProgressPercent('applied') + '%' }"
            @mouseenter="showTooltip($event, 'applied')"
            @mouseleave="hideTooltip"
          ></div>
          <div
            class="progress-segment screening-segment"
            :style="{ width: getProgressPercent('screening') + '%' }"
            @mouseenter="showTooltip($event, 'screening')"
            @mouseleave="hideTooltip"
          ></div>
          <div
            class="progress-segment interviewing-segment"
            :style="{ width: getProgressPercent('interviewing') + '%' }"
            @mouseenter="showTooltip($event, 'interviewing')"
            @mouseleave="hideTooltip"
          ></div>
          <div
            class="progress-segment hired-segment"
            :style="{ width: getProgressPercent('hired') + '%' }"
            @mouseenter="showTooltip($event, 'hired')"
            @mouseleave="hideTooltip"
          ></div>
          <div
            class="progress-segment rejected-segment"
            :style="{ width: getProgressPercent('rejected') + '%' }"
            @mouseenter="showTooltip($event, 'rejected')"
            @mouseleave="hideTooltip"
          ></div>
        </div>
        <div class="progress-empty" v-else>
          <div class="progress-empty-bar"></div>
          <span class="progress-empty-text">暂无求职数据</span>
        </div>
      </div>
      <div class="progress-legend">
        <div
          v-for="stage in progressStages"
          :key="stage.key"
          class="legend-item"
          :class="{ 'legend-item--active': hoveredStage === stage.key }"
          @mouseenter="hoveredStage = stage.key"
          @mouseleave="hoveredStage = null"
        >
          <span class="legend-dot" :class="stage.key"></span>
          <span class="legend-label">{{ stage.label }}</span>
          <span class="legend-count">{{ statistics[stage.key] || 0 }}</span>
          <span class="legend-percent">{{ getProgressPercent(stage.key) }}%</span>
        </div>
      </div>
    </div>

    <!-- 看板区域 -->
    <div class="board-section">
      <div class="board-columns" v-loading="loading">
        <!-- 已收藏 -->
        <div class="board-column favorited">
          <div class="column-header">
            <div class="header-left">
              <el-icon><Star /></el-icon>
              <h3>已收藏</h3>
            </div>
            <span class="count-badge">{{ boardData.favorited?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.favorited"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'favorited'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.favorited?.length" description="暂无收藏" :image-size="80">
              <template #image>
                <div class="empty-icon">
                  <el-icon :size="40"><Star /></el-icon>
                </div>
              </template>
            </el-empty>
          </div>
        </div>

        <!-- 已投递 -->
        <div class="board-column">
          <div class="column-header applied">
            <h3>已投递</h3>
            <span class="count">{{ boardData.applied?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.applied"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'applied'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.applied?.length" description="暂无投递" :image-size="60" />
          </div>
        </div>

        <!-- 初筛中 -->
        <div class="board-column">
          <div class="column-header screening">
            <h3>初筛中</h3>
            <span class="count">{{ boardData.screening?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.screening"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'screening'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.screening?.length" description="暂无数据" :image-size="60" />
          </div>
        </div>

        <!-- 面试中 -->
        <div class="board-column">
          <div class="column-header interviewing">
            <h3>面试中</h3>
            <span class="count">{{ boardData.interviewing?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.interviewing"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'interviewing'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.interviewing?.length" description="暂无面试" :image-size="60" />
          </div>
        </div>

        <!-- 已录用 -->
        <div class="board-column">
          <div class="column-header hired">
            <h3>已录用</h3>
            <span class="count">{{ boardData.hired?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.hired"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'hired'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.hired?.length" description="暂无录用" :image-size="60" />
          </div>
        </div>

        <!-- 已拒绝 -->
        <div class="board-column">
          <div class="column-header rejected">
            <h3>已拒绝</h3>
            <span class="count">{{ boardData.rejected?.length || 0 }}</span>
          </div>
          <div class="column-body">
            <draggable
              v-model="boardData.rejected"
              group="jobs"
              item-key="id"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <JobCard :job="element" :stage="'rejected'" />
              </template>
            </draggable>
            <el-empty v-if="!boardData.rejected?.length" description="暂无数据" :image-size="60" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Star, Document, Search, VideoCamera, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import JobCard from '../components/JobCard.vue'
import { getBoardData, updateApplicationStatus, getBoardStatistics } from '@/api/subscription'

const loading = ref(false)
const boardData = reactive({
  favorited: [],
  applied: [],
  screening: [],
  interviewing: [],
  hired: [],
  rejected: []
})

const statistics = ref({})
const hoveredStage = ref(null)

// 进度阶段配置
const progressStages = [
  { key: 'favorited', label: '已收藏' },
  { key: 'applied', label: '已投递' },
  { key: 'screening', label: '初筛中' },
  { key: 'interviewing', label: '面试中' },
  { key: 'hired', label: '已录用' },
  { key: 'rejected', label: '已拒绝' }
]

// 计算总申请数（只累加各阶段的数值，排除 successRate/avgResponseTime 等非阶段字段）
const totalApplications = computed(() => {
  const s = statistics.value
  return (s.favorited || 0) + (s.applied || 0) + (s.screening || 0) + (s.interviewing || 0) + (s.hired || 0) + (s.rejected || 0)
})

// 计算进度百分比
const getProgressPercent = (stage) => {
  const total = totalApplications.value
  if (total === 0) return 0
  const count = statistics.value[stage] || 0
  return Math.round((count / total) * 100)
}

// Tooltip 状态
const tooltipVisible = ref(false)
const tooltipStyle = ref({})
const tooltipStage = ref('')

const showTooltip = (event, stage) => {
  const rect = event.target.getBoundingClientRect()
  const parentRect = event.target.parentElement.getBoundingClientRect()
  tooltipStyle.value = {
    left: `${rect.left - parentRect.left + rect.width / 2}px`,
    top: '-8px'
  }
  tooltipStage.value = stage
  tooltipVisible.value = true
}

const hideTooltip = () => {
  tooltipVisible.value = false
}

const stageLabels = {
  favorited: '已收藏',
  applied: '已投递',
  screening: '初筛中',
  interviewing: '面试中',
  hired: '已录用',
  rejected: '已拒绝'
}

// 获取看板数据
const fetchBoardData = async () => {
  loading.value = true
  try {
    const res = await getBoardData()
    Object.assign(boardData, res.data)
  } catch (error) {
    ElMessage.error('获取看板数据失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const res = await getBoardStatistics()
    statistics.value = res.data
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

// 拖拽结束
const handleDragEnd = async (event) => {
  const { to, from, item } = event
  
  // 获取目标列的stage
  const toColumn = to.closest('.board-column')
  const fromColumn = from.closest('.board-column')
  
  if (!toColumn || !fromColumn) return
  
  const toStage = getColumnStage(toColumn)
  const fromStage = getColumnStage(fromColumn)
  
  // 如果列相同，不做处理
  if (toStage === fromStage) return
  
  // 获取applicationId
  const jobId = item.__draggable_context.element.applicationId
  
  if (!jobId) {
    ElMessage.warning('无法更新收藏职位的状态')
    return
  }
  
  // 映射stage到status
  const statusMap = {
    applied: 0,
    screening: 0,
    interviewing: 3,
    hired: 4,
    rejected: 2
  }
  
  const newStatus = statusMap[toStage]
  if (newStatus === undefined) return
  
  try {
    await updateApplicationStatus(jobId, newStatus)
    ElMessage.success('状态更新成功')
    
    // 刷新数据
    fetchBoardData()
    fetchStatistics()
  } catch (error) {
    ElMessage.error('状态更新失败')
    // 刷新数据回滚
    fetchBoardData()
  }
}

// 获取列的stage
const getColumnStage = (columnElement) => {
  const header = columnElement.querySelector('.column-header')
  if (header.classList.contains('favorited')) return 'favorited'
  if (header.classList.contains('applied')) return 'applied'
  if (header.classList.contains('screening')) return 'screening'
  if (header.classList.contains('interviewing')) return 'interviewing'
  if (header.classList.contains('hired')) return 'hired'
  if (header.classList.contains('rejected')) return 'rejected'
  return ''
}

// 刷新数据
const refreshData = () => {
  fetchBoardData()
  fetchStatistics()
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  fetchBoardData()
  fetchStatistics()
})
</script>

<style scoped>
.application-board-container {
  padding: var(--space-6) var(--space-7);
  background: var(--color-bg);
  min-height: 100vh;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-7);
}

.header-content {
  flex: 1;
}

.page-title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  letter-spacing: -0.5px;
}

.page-subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.refresh-btn {
  flex-shrink: 0;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--space-5);
  margin-bottom: var(--space-7);
}

.stat-card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  position: relative;
  overflow: hidden;
  transition: all var(--duration-slow) var(--ease-out);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-200);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--card-color-start), var(--card-color-end));
}

.stat-card.favorited { --card-color-start: var(--kanban-favorited); --card-color-end: #e07be0; }
.stat-card.applied { --card-color-start: var(--kanban-applied); --card-color-end: #3d8bfd; }
.stat-card.screening { --card-color-start: var(--kanban-screening); --card-color-end: #e06080; }
.stat-card.interviewing { --card-color-start: var(--kanban-interviewing); --card-color-end: #28b5b5; }
.stat-card.hired { --card-color-start: var(--kanban-hired); --card-color-end: #0e8078; }
.stat-card.rejected { --card-color-start: var(--kanban-rejected); --card-color-end: #d02040; }

.stat-icon-wrapper {
  position: relative;
  width: 56px;
  height: 56px;
  margin-bottom: var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-bg {
  position: absolute;
  inset: 0;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--card-color-start), var(--card-color-end));
  opacity: 0.12;
}

.stat-icon-wrapper .el-icon {
  position: relative;
  z-index: 1;
  color: var(--card-color-start);
}

.stat-info {
  margin-bottom: var(--space-2);
}

.stat-value {
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  line-height: 1;
  margin-bottom: var(--space-1);
}

.stat-label {
  font-size: var(--text-base);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}

.stat-trend {
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}

.trend-text {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* 进度条区域 */
.progress-section {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  margin-bottom: var(--space-7);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-200);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.progress-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.progress-total {
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.progress-summary {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.progress-stat {
  font-size: var(--text-sm);
  color: var(--gray-500);
  padding: var(--space-1) var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.progress-bar-container {
  margin-bottom: var(--space-5);
  position: relative;
}

/* Tooltip */
.progress-tooltip {
  position: absolute;
  transform: translateX(-50%);
  z-index: 10;
  pointer-events: none;
  animation: tooltipFadeIn var(--duration-fast) var(--ease-out);
}

.tooltip-content {
  background: var(--gray-800);
  color: var(--color-surface);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  white-space: nowrap;
  font-size: var(--text-sm);
  box-shadow: var(--shadow-lg);
}

.tooltip-label {
  font-weight: var(--weight-medium);
}

.tooltip-value {
  font-weight: var(--weight-bold);
}

.tooltip-percent {
  color: var(--gray-300);
  font-size: var(--text-xs);
}

.tooltip-arrow {
  width: 8px;
  height: 8px;
  background: var(--gray-800);
  transform: rotate(45deg);
  margin: -4px auto 0;
}

@keyframes tooltipFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

/* 进度条 */
.progress-bar {
  height: 14px;
  background: var(--gray-100);
  border-radius: var(--radius-full);
  overflow: hidden;
  display: flex;
  animation: progressGrow 0.8s var(--ease-out);
}

@keyframes progressGrow {
  from {
    opacity: 0;
    transform: scaleX(0);
    transform-origin: left;
  }
  to {
    opacity: 1;
    transform: scaleX(1);
    transform-origin: left;
  }
}

.progress-segment {
  height: 100%;
  transition: all var(--duration-slow) var(--ease-out);
  cursor: pointer;
  position: relative;
}

.progress-segment:hover {
  filter: brightness(1.1);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.3);
}

.progress-segment.favorited-segment { background: linear-gradient(90deg, var(--kanban-favorited), #e07be0); }
.progress-segment.applied-segment { background: linear-gradient(90deg, var(--kanban-applied), #3d8bfd); }
.progress-segment.screening-segment { background: linear-gradient(90deg, var(--kanban-screening), #e06080); }
.progress-segment.interviewing-segment { background: linear-gradient(90deg, var(--kanban-interviewing), #28b5b5); }
.progress-segment.hired-segment { background: linear-gradient(90deg, var(--kanban-hired), #0e8078); }
.progress-segment.rejected-segment { background: linear-gradient(90deg, var(--kanban-rejected), #d02040); }

/* 空状态 */
.progress-empty {
  text-align: center;
  padding: var(--space-4) 0;
}

.progress-empty-bar {
  height: 14px;
  background: var(--gray-100);
  border-radius: var(--radius-full);
  margin-bottom: var(--space-3);
}

.progress-empty-text {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* 图例 */
.progress-legend {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--gray-600);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
  cursor: default;
}

.legend-item:hover,
.legend-item--active {
  background: var(--gray-50);
  color: var(--gray-800);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: var(--radius-full);
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out);
}

.legend-item:hover .legend-dot,
.legend-item--active .legend-dot {
  transform: scale(1.2);
}

.legend-label {
  font-weight: var(--weight-medium);
}

.legend-count {
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  min-width: 20px;
  text-align: right;
}

.legend-percent {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  min-width: 32px;
  text-align: right;
}

.legend-dot.favorited { background: var(--kanban-favorited); }
.legend-dot.applied { background: var(--kanban-applied); }
.legend-dot.screening { background: var(--kanban-screening); }
.legend-dot.interviewing { background: var(--kanban-interviewing); }
.legend-dot.hired { background: var(--kanban-hired); }
.legend-dot.rejected { background: var(--kanban-rejected); }

/* 看板区域 */
.board-section {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--gray-200);
}

.board-columns {
  display: flex;
  gap: var(--space-5);
  overflow-x: auto;
  padding-bottom: var(--space-4);
}

.board-column {
  min-width: 300px;
  max-width: 300px;
  background: var(--gray-50);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 360px);
}

.column-header {
  padding: var(--space-4) var(--space-5);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-surface);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  border-bottom: 2px solid var(--gray-100);
}

.board-column.favorited .column-header { border-bottom-color: var(--kanban-favorited); }
.board-column.applied .column-header { border-bottom-color: var(--kanban-applied); }
.board-column.screening .column-header { border-bottom-color: var(--kanban-screening); }
.board-column.interviewing .column-header { border-bottom-color: var(--kanban-interviewing); }
.board-column.hired .column-header { border-bottom-color: var(--kanban-hired); }
.board-column.rejected .column-header { border-bottom-color: var(--kanban-rejected); }

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.header-left .el-icon {
  font-size: 18px;
}

.board-column.favorited .header-left .el-icon { color: var(--kanban-favorited); }
.board-column.applied .header-left .el-icon { color: var(--kanban-applied); }
.board-column.screening .header-left .el-icon { color: var(--kanban-screening); }
.board-column.interviewing .header-left .el-icon { color: var(--kanban-interviewing); }
.board-column.hired .header-left .el-icon { color: var(--kanban-hired); }
.board-column.rejected .header-left .el-icon { color: var(--kanban-rejected); }

.column-header h3 {
  margin: 0;
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.count-badge {
  background: var(--gray-100);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-600);
}

.column-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-height: 200px;
}

.column-body::-webkit-scrollbar {
  width: 6px;
}

.column-body::-webkit-scrollbar-thumb {
  background: var(--gray-300);
  border-radius: var(--radius-full);
}

.column-body::-webkit-scrollbar-thumb:hover {
  background: var(--gray-400);
}

.column-body::-webkit-scrollbar-track {
  background: transparent;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-full);
  background: var(--gray-100);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-4);
  color: var(--gray-400);
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .application-board-container {
    padding: var(--space-4);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3);
  }

  .stat-card {
    padding: var(--space-4);
  }

  .stat-value {
    font-size: var(--text-2xl);
  }

  .board-columns {
    gap: var(--space-3);
  }

  .board-column {
    min-width: 260px;
    max-width: 260px;
  }
}
</style>