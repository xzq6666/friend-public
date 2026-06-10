<template>
<div class="page-container-sm page-enter">
  <div class="page-header">
    <h2>举报历史</h2>
    <p class="header-desc">查看我的举报记录和处理进度</p>
  </div>

  <!-- 统计卡片 -->
  <div class="stats-row">
    <div class="stat-card pending">
      <div class="stat-value">{{ stats.pending }}</div>
      <div class="stat-label">待处理</div>
    </div>
    <div class="stat-card processing">
      <div class="stat-value">{{ stats.processing }}</div>
      <div class="stat-label">处理中</div>
    </div>
    <div class="stat-card handled">
      <div class="stat-value">{{ stats.handled }}</div>
      <div class="stat-label">已处理</div>
    </div>
    <div class="stat-card rejected">
      <div class="stat-value">{{ stats.rejected }}</div>
      <div class="stat-label">已驳回</div>
    </div>
  </div>

  <!-- 筛选 -->
  <div class="filter-bar">
    <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 150px" @change="fetchReports">
      <el-option label="全部" :value="null" />
      <el-option label="待处理" :value="0" />
      <el-option label="处理中" :value="1" />
      <el-option label="已处理" :value="2" />
      <el-option label="已驳回" :value="3" />
    </el-select>
    <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 150px" @change="fetchReports">
      <el-option label="全部" :value="null" />
      <el-option label="帖子" :value="1" />
      <el-option label="评论" :value="2" />
      <el-option label="用户" :value="3" />
      <el-option label="职位" :value="4" />
    </el-select>
  </div>

  <!-- 举报列表 -->
  <div class="report-list-card" v-loading="loading">
    <el-empty v-if="!loading && reports.length === 0" description="暂无举报记录" />
    <div v-else class="report-list">
      <div v-for="item in reports" :key="item.id" class="report-item" @click="viewDetail(item)">
        <div class="report-icon" :class="'type-' + item.reportedType">
          {{ typeIcons[item.reportedType] || '?' }}
        </div>
        <div class="report-info">
          <div class="report-header">
            <el-tag :type="typeTagMap[item.reportedType]" size="small">{{ typeTextMap[item.reportedType] || '未知' }}</el-tag>
            <el-tag :type="statusTagMap[item.status]" size="small" effect="plain">{{ statusTextMap[item.status] || '未知' }}</el-tag>
          </div>
          <div class="report-reason">{{ item.reason }}</div>
          <div class="report-desc" v-if="item.description">{{ item.description }}</div>
        </div>
        <span class="report-time">{{ formatTime(item.createTime) }}</span>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchReports"
      />
    </div>
  </div>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" title="举报详情" width="600px">
    <div v-if="currentReport" class="detail-content">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
        <el-descriptions-item label="举报类型">
          <el-tag :type="typeTagMap[currentReport.reportedType]">{{ typeTextMap[currentReport.reportedType] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报原因" :span="2">{{ currentReport.reason }}</el-descriptions-item>
        <el-descriptions-item label="详细描述" :span="2">{{ currentReport.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="statusTagMap[currentReport.status]">{{ statusTextMap[currentReport.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报时间">{{ formatTime(currentReport.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handleResult" label="处理结果" :span="2">
          {{ currentReport.handleResult }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentReport.handleTime" label="处理时间" :span="2">
          {{ formatTime(currentReport.handleTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'

const loading = ref(false)
const reports = ref([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const filterStatus = ref(null)
const filterType = ref(null)

const detailVisible = ref(false)
const currentReport = ref(null)

const stats = reactive({ pending: 0, processing: 0, handled: 0, rejected: 0 })

const typeTextMap = { 1: '帖子', 2: '评论', 3: '用户', 4: '职位' }
const typeTagMap = { 1: 'primary', 2: 'warning', 3: 'info', 4: 'success' }
const typeIcons = { 1: '帖', 2: '评', 3: '人', 4: '职' }
const statusTextMap = { 0: '待处理', 1: '处理中', 2: '已处理', 3: '已驳回' }
const statusTagMap = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }

const fetchReports = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (filterStatus.value !== null) params.status = filterStatus.value
    if (filterType.value !== null) params.reportedType = filterType.value
    const res = await request.get('/report/my', { params, skipGlobalLoading: true })
    reports.value = res.records || []
    total.value = res.total || 0
  } catch {
    reports.value = []
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const [p, pr, h, r] = await Promise.all([
      request.get('/report/my', { params: { page: 1, size: 1, status: 0 }, skipGlobalLoading: true }).catch(() => ({ total: 0 })),
      request.get('/report/my', { params: { page: 1, size: 1, status: 1 }, skipGlobalLoading: true }).catch(() => ({ total: 0 })),
      request.get('/report/my', { params: { page: 1, size: 1, status: 2 }, skipGlobalLoading: true }).catch(() => ({ total: 0 })),
      request.get('/report/my', { params: { page: 1, size: 1, status: 3 }, skipGlobalLoading: true }).catch(() => ({ total: 0 }))
    ])
    stats.pending = p.total || 0
    stats.processing = pr.total || 0
    stats.handled = h.total || 0
    stats.rejected = r.total || 0
  } catch { /* ignore */ }
}

const viewDetail = async (item) => {
  try {
    const res = await request.get(`/report/${item.id}`, { skipGlobalLoading: true })
    currentReport.value = res
    detailVisible.value = true
  } catch { /* ignore */ }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return d.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchReports()
  fetchStats()
})
</script>

<style scoped>
/* ── 统计卡片 ──────────────────────────────── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.stat-card {
  text-align: center;
}
.pending .stat-value { color: #e6a23c; }
.processing .stat-value { color: #409eff; }
.handled .stat-value { color: #67c23a; }
.rejected .stat-value { color: #909399; }

/* ── 举报列表 ──────────────────────────────── */
.report-list-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.report-list {
  display: flex;
  flex-direction: column;
}

.report-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--gray-100);
  cursor: pointer;
  transition: background var(--duration-fast);
}
.report-item:last-child { border-bottom: none; }
.report-item:hover { background: var(--gray-50); }

.report-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: var(--weight-bold);
  color: #fff;
  flex-shrink: 0;
}
.report-icon.type-1 { background: #409eff; }
.report-icon.type-2 { background: #e6a23c; }
.report-icon.type-3 { background: #909399; }
.report-icon.type-4 { background: #67c23a; }

.report-info {
  flex: 1;
  min-width: 0;
}
.report-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
}
.report-reason {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
}
.report-desc {
  font-size: var(--text-xs);
  color: var(--gray-500);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.report-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
  white-space: nowrap;
  flex-shrink: 0;
}

.pagination {
  margin-top: var(--space-4);
  display: flex;
  justify-content: center;
}

.detail-content {
  padding: var(--space-2) 0;
}

@media (max-width: 640px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
