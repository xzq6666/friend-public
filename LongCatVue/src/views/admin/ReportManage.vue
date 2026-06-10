<template>
  <div class="report-manage-page page-enter">
    <div class="page-header">
      <h2>举报管理</h2>
      <p class="page-subtitle">处理用户举报，维护平台秩序</p>
    </div>

    <div class="stats-section">
      <div 
        v-for="stat in statItems" 
        :key="stat.key"
        class="stat-card"
        :class="{ active: activeStatus === stat.key, clickable: true }"
        @click="setStatusFilter(stat.key)"
      >
        <div class="stat-icon-wrap" :style="{ background: stat.bgColor }">
          <el-icon :size="20" :style="{ color: stat.iconColor }"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-count" :style="{ color: stat.iconColor }">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div v-if="activeStatus === stat.key" class="stat-indicator"></div>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">举报类型</span>
          <el-select 
            v-model="filterType" 
            placeholder="全部" 
            clearable 
            style="width: 140px" 
            @change="fetchReports"
          >
            <el-option label="全部" :value="null" />
            <el-option label="帖子" :value="1" />
            <el-option label="评论" :value="2" />
            <el-option label="用户" :value="3" />
            <el-option label="职位" :value="4" />
          </el-select>
        </div>
        <div class="filter-group">
          <span class="filter-label">举报时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :shortcuts="dateShortcuts"
            style="width: 240px"
            @change="fetchReports"
          />
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="fetchReports" class="action-btn">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter" class="action-btn">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-wrapper">
      <div class="table-header-row">
        <span class="table-title">举报列表</span>
        <div class="table-actions">
          <el-button 
            v-if="selectedReports.length > 0" 
            type="danger" 
            size="small"
            @click="batchDelete"
          >
            <el-icon><Delete /></el-icon> 批量删除 ({{ selectedReports.length }})
          </el-button>
        </div>
      </div>

      <div class="table-card">
        <el-table 
          v-loading="loading" 
          :data="filteredReports" 
          stripe 
          style="width: 100%"
          :default-sort="{ prop: 'createTime', order: 'descending' }"
          @select="handleSelect"
          @select-all="handleSelectAll"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column 
            label="举报类型" 
            width="100" 
            :sortable="false"
          >
            <template #default="{ row }">
              <el-tag 
                :type="getReportTypeTag(row.reportedType)" 
                size="small" 
                effect="plain"
                class="report-type-tag"
              >
                {{ getReportTypeText(row.reportedType) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column 
            label="被举报内容" 
            min-width="180" 
            show-overflow-tooltip
            :sortable="false"
          >
            <template #default="{ row }">
              <el-link 
                type="primary" 
                @click="goToContent(row)" 
                :underline="false"
                class="content-link"
              >
                {{ row.reportedContent || 'ID: ' + row.reportedId }}
              </el-link>
            </template>
          </el-table-column>
          
          <el-table-column 
            prop="reason" 
            label="举报原因" 
            width="120"
            :sortable="false"
          />
          
          <el-table-column 
            prop="description" 
            label="详细描述" 
            min-width="180" 
            show-overflow-tooltip
            :sortable="false"
          />
          
          <el-table-column 
            label="举报人" 
            width="110"
            :sortable="false"
          >
            <template #default="{ row }">
              <span class="reporter-info">
                {{ row.reporterUsername || 'ID: ' + row.reporterId }}
              </span>
            </template>
          </el-table-column>
          
          <el-table-column 
            label="状态" 
            width="110"
            :sortable="false"
          >
            <template #default="{ row }">
              <div class="status-wrapper">
                <span class="status-indicator" :class="'status-' + row.status"></span>
                <span class="status-text" :class="'status-' + row.status">
                  {{ getStatusText(row.status) }}
                </span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column 
            label="举报时间" 
            width="170"
            prop="createTime"
            :sortable="true"
          >
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          
          <el-table-column 
            label="操作" 
            width="180" 
            fixed="right"
          >
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button 
                  type="primary" 
                  link 
                  size="small" 
                  @click="viewDetail(row)"
                  class="action-btn-detail"
                >
                  <el-icon><View /></el-icon> 详情
                </el-button>
                <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                  <el-button type="primary" link size="small" class="action-btn-more">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item 
                        v-if="row.status === 0 || row.status === 1" 
                        command="handle"
                      >
                        <el-icon><Edit /></el-icon> 处理
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon> 删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <span class="pagination-info">共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next"
            @size-change="fetchReports"
            @current-change="fetchReports"
          />
        </div>
      </div>
    </div>

    <el-dialog 
      v-model="detailDialogVisible" 
      title="举报详情" 
      width="720px"
      class="report-detail-dialog"
    >
      <div v-if="currentReport" class="report-detail">
        <div class="detail-header">
          <div class="detail-title-row">
            <span class="detail-id">举报ID: {{ currentReport.id }}</span>
            <el-tag :type="getStatusTagType(currentReport.status)" class="detail-status">
              {{ getStatusText(currentReport.status) }}
            </el-tag>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="detail-section">
            <h4 class="section-title">基本信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">举报类型</span>
                <el-tag :type="getReportTypeTag(currentReport.reportedType)">
                  {{ getReportTypeText(currentReport.reportedType) }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="info-label">被举报对象</span>
                <span class="info-value">{{ currentReport.reportedContent || 'ID: ' + currentReport.reportedId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">举报人</span>
                <span class="info-value">{{ currentReport.reporterUsername || 'ID: ' + currentReport.reporterId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">举报原因</span>
                <span class="info-value">{{ currentReport.reason }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">举报时间</span>
                <span class="info-value">{{ formatDate(currentReport.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4 class="section-title">详细描述</h4>
            <div class="description-box">{{ currentReport.description || '无' }}</div>
          </div>

          <div v-if="currentReport.evidenceUrls && parseEvidenceUrls(currentReport.evidenceUrls).length > 0" class="detail-section">
            <h4 class="section-title">证据图片</h4>
            <div class="evidence-grid">
              <el-image 
                v-for="(url, index) in parseEvidenceUrls(currentReport.evidenceUrls)" 
                :key="index"
                :src="url" 
                :preview-src-list="parseEvidenceUrls(currentReport.evidenceUrls)"
                fit="cover"
                class="evidence-image"
              />
            </div>
          </div>

          <div v-if="currentReport.handleResult" class="detail-section">
            <h4 class="section-title">处理结果</h4>
            <div class="handle-result-box">
              <div class="result-header">
                <span class="result-status" :class="'status-' + currentReport.status">
                  {{ getStatusText(currentReport.status) }}
                </span>
                <span class="result-time">{{ formatDate(currentReport.handleTime) }}</span>
              </div>
              <p class="result-content">{{ currentReport.handleResult }}</p>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="currentReport?.status === 0 || currentReport?.status === 1" 
          type="success" 
          @click="detailDialogVisible = false; showHandleDialog(currentReport)"
        >
          <el-icon><Edit /></el-icon> 处理举报
        </el-button>
      </template>
    </el-dialog>

    <el-dialog 
      v-model="handleDialogVisible" 
      title="处理举报" 
      width="600px"
      class="handle-report-dialog"
    >
      <div class="handle-form-container">
        <div class="form-section">
          <div class="form-info-row">
            <span class="info-badge">举报ID: {{ currentReport?.id }}</span>
            <el-tag :type="getReportTypeTag(currentReport?.reportedType)">
              {{ getReportTypeText(currentReport?.reportedType) }}
            </el-tag>
          </div>
        </div>

        <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" class="handle-form">
          <el-form-item label="处理结果" prop="status" class="form-item">
            <div class="radio-group">
              <el-radio-group v-model="handleForm.status">
                <el-radio :label="1" class="radio-option">
                  <span class="radio-label">处理中</span>
                  <span class="radio-desc">标记为正在处理</span>
                </el-radio>
                <el-radio :label="2" class="radio-option">
                  <span class="radio-label">已处理</span>
                  <span class="radio-desc">举报已核实并处理</span>
                </el-radio>
                <el-radio :label="3" class="radio-option">
                  <span class="radio-label">已驳回</span>
                  <span class="radio-desc">举报不成立，予以驳回</span>
                </el-radio>
              </el-radio-group>
            </div>
          </el-form-item>

          <el-form-item v-if="handleForm.status === 2 && currentReport?.reportedType !== 3" class="form-item">
            <label class="form-label">内容处罚</label>
            <el-checkbox v-model="handleForm.deleteContent" class="checkbox-option">
              {{ deleteContentLabel }}
            </el-checkbox>
          </el-form-item>

          <el-form-item v-if="handleForm.status === 2 && currentReport?.reportedType !== 4" class="form-item">
            <label class="form-label">用户处罚</label>
            <el-checkbox v-model="handleForm.banUser" class="checkbox-option">
              禁言被举报用户（{{ currentReport?.reportedContent || '账号设为禁用' }}）
            </el-checkbox>
          </el-form-item>

          <el-form-item label="处理说明" prop="handleResult" class="form-item">
            <el-input
              v-model="handleForm.handleResult"
              type="textarea"
              :rows="4"
              placeholder="请填写处理说明，将通知举报人"
              class="result-textarea"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleReport" :loading="submitting" class="submit-btn">
          <el-icon><Check /></el-icon> 确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, View, Edit, Check, Delete, ArrowDown,
  Document, User, Briefcase, WarnTriangleFilled
} from '@element-plus/icons-vue'

const router = useRouter()


const loading = ref(false)
const submitting = ref(false)
const reports = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterType = ref(null)
const filterStatus = ref(null)
const activeStatus = ref('all')
const dateRange = ref([])
const selectedReports = ref([])

const stats = reactive({
  pending: 0,
  processing: 1,
  handled: 1,
  total: 1
})

const detailDialogVisible = ref(false)
const handleDialogVisible = ref(false)
const currentReport = ref(null)

const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  }
]

const statItems = computed(() => [
  { 
    key: 'all', 
    label: '全部', 
    value: stats.total, 
    icon: WarnTriangleFilled, 
    iconColor: '#4e6496',
    bgColor: 'rgba(78, 100, 150, 0.1)'
  },
  { 
    key: 'pending', 
    label: '待处理', 
    value: stats.pending, 
    icon: WarnTriangleFilled, 
    iconColor: '#c08a2e',
    bgColor: 'rgba(192, 138, 46, 0.1)'
  },
  { 
    key: 'processing', 
    label: '处理中', 
    value: stats.processing, 
    icon: Document, 
    iconColor: '#4e6496',
    bgColor: 'rgba(78, 100, 150, 0.1)'
  },
  { 
    key: 'handled', 
    label: '已处理', 
    value: stats.handled, 
    icon: Check, 
    iconColor: '#3ea15d',
    bgColor: 'rgba(62, 161, 93, 0.1)'
  }
])

const filteredReports = computed(() => {
  const map = { pending: 0, processing: 1, handled: 2, rejected: 3 }
  if (activeStatus.value === 'all') return reports.value
  const target = map[activeStatus.value]
  if (target !== undefined) return reports.value.filter(r => r.status === target)
  return reports.value
})

const setStatusFilter = (status) => {
  activeStatus.value = status
}

const handleCommand = (cmd, row) => {
  if (cmd === 'handle') showHandleDialog(row)
  else if (cmd === 'delete') deleteReport(row)
}

const handleSelect = (val, row) => {
  const index = selectedReports.value.findIndex(r => r.id === row.id)
  if (index > -1) {
    selectedReports.value.splice(index, 1)
  } else {
    selectedReports.value.push(row)
  }
}

const handleSelectAll = (val) => {
  if (val) {
    selectedReports.value = [...filteredReports.value]
  } else {
    selectedReports.value = []
  }
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedReports.value.length} 条举报记录吗？删除后不可恢复。`, 
      '提示', 
      { type: 'warning' }
    )
    for (const report of selectedReports.value) {
      await request.delete(`/report/${report.id}`)
    }
    ElMessage.success('批量删除成功')
    selectedReports.value = []
    fetchReports()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

const handleForm = reactive({
  status: 2,
  handleResult: '',
  deleteContent: false,
  banUser: false
})

const handleFormRef = ref()
const handleRules = {
  status: [
    { required: true, message: '请选择处理结果', trigger: 'change' }
  ],
  handleResult: [
    { required: true, message: '请填写处理说明', trigger: 'blur' },
    { min: 5, message: '处理说明至少5个字符', trigger: 'blur' }
  ]
}

const deleteContentLabel = computed(() => {
  const type = currentReport.value?.reportedType
  if (type === 1) return '删除被举报帖子'
  if (type === 2) return '删除被举报评论'
  if (type === 4) return '下架被举报职位'
  return '删除被举报内容'
})

const fetchReports = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterType.value !== null) {
      params.reportedType = filterType.value
    }
    if (filterStatus.value !== null) {
      params.status = filterStatus.value
    }
    if (dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    
    const res = await request.get('/report/list', { params })
    reports.value = res.records || []
    total.value = res.total || 0
    
    updateStats()
  } catch (error) {
    ElMessage.error('加载失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    loading.value = false
  }
}

const updateStats = async () => {
  try {
    const res = await request.get('/report/stats')
    stats.pending = res.pending || 0
    stats.processing = res.processing || 0
    stats.handled = res.handled || 0
    stats.total = res.total || 0
  } catch {
    // 忽略统计错误
  }
}

const viewDetail = async (report) => {
  try {
    const res = await request.get(`/report/${report.id}`)
    currentReport.value = res
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载详情失败')
  }
}

const showHandleDialog = (report) => {
  currentReport.value = report
  handleForm.status = 2
  handleForm.handleResult = ''
  handleForm.deleteContent = false
  handleForm.banUser = false
  handleDialogVisible.value = true
}

const handleReport = async () => {
  const valid = await handleFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await request.put(`/report/${currentReport.value.id}/handle`, {
      status: handleForm.status,
      handleResult: handleForm.handleResult,
      deleteContent: handleForm.deleteContent,
      banUser: handleForm.banUser
    })
    ElMessage.success('处理成功')
    handleDialogVisible.value = false
    fetchReports()
  } catch (error) {
    ElMessage.error('操作失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const deleteReport = async (report) => {
  try {
    await ElMessageBox.confirm('确定要删除这条举报记录吗？删除后不可恢复。', '提示', { type: 'warning' })
    await request.delete(`/report/${report.id}`)
    ElMessage.success('删除成功')
    fetchReports()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

const resetFilter = () => {
  filterType.value = null
  filterStatus.value = null
  activeStatus.value = 'all'
  dateRange.value = []
  currentPage.value = 1
  selectedReports.value = []
  fetchReports()
}

const goToContent = async (row) => {
  try {
    const res = await request.get('/report/jump-url', {
      params: { reportedType: row.reportedType, reportedId: row.reportedId }
    })
    if (res.url) {
      router.push(res.url)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '无法跳转，内容可能已删除')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const parseEvidenceUrls = (urls) => {
  if (!urls) return []
  try {
    return JSON.parse(urls)
  } catch {
    return []
  }
}

const getReportTypeText = (type) => {
  const map = { 1: '帖子', 2: '评论', 3: '用户', 4: '职位' }
  return map[type] || '未知'
}

const getReportTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'warning', 3: 'info', 4: 'success' }
  return map[type] || 'info'
}

const getStatusText = (status) => {
  const map = {
    0: '待处理',
    1: '处理中',
    2: '已处理',
    3: '已驳回'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.report-manage-page {
  padding: var(--space-6);
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--space-6);
}

.page-header h2 {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin-bottom: var(--space-2);
}

.page-subtitle {
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  transition: all var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--gray-300);
}

.stat-card.active {
  background: var(--gray-50);
  border-color: var(--primary-300);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-count {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-top: 2px;
}

.stat-indicator {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 32px;
  background: var(--primary-500);
  border-radius: 4px 0 0 4px;
}

.filter-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  margin-bottom: var(--space-5);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.filter-label {
  font-size: var(--text-sm);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}

.filter-actions {
  display: flex;
  gap: var(--space-2);
  margin-left: auto;
}

.action-btn {
  min-width: 90px;
}

.table-wrapper {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.table-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.table-actions {
  display: flex;
  gap: var(--space-2);
}

.table-card {
  padding: var(--space-2);
}

.report-type-tag {
  font-size: var(--text-xs);
  padding: 2px 8px;
}

.content-link {
  font-size: var(--text-sm);
}

.content-link:hover {
  text-decoration: underline;
}

.reporter-info {
  font-size: var(--text-sm);
  color: var(--gray-600);
}

.status-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-indicator.status-0 {
  background: var(--warning-500);
  box-shadow: 0 0 0 3px var(--warning-50);
}

.status-indicator.status-1 {
  background: var(--primary-500);
  box-shadow: 0 0 0 3px var(--primary-50);
}

.status-indicator.status-2 {
  background: var(--success-500);
  box-shadow: 0 0 0 3px var(--success-50);
}

.status-indicator.status-3 {
  background: var(--gray-400);
  box-shadow: 0 0 0 3px var(--gray-100);
}

.status-text {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

.status-text.status-0 { color: var(--warning-600); }
.status-text.status-1 { color: var(--primary-600); }
.status-text.status-2 { color: var(--success-600); }
.status-text.status-3 { color: var(--gray-500); }

.action-buttons {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.action-btn-detail {
  color: var(--primary-600);
}

.action-btn-more {
  color: var(--gray-500);
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-2);
  border-top: 1px solid var(--color-border);
}

.pagination-info {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.report-detail-dialog,
.handle-report-dialog {
  padding: 0;
}

.report-detail {
  padding: var(--space-6);
}

.detail-header {
  margin-bottom: var(--space-6);
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-id {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.detail-status {
  font-size: var(--text-sm);
  padding: 4px 12px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-section {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.section-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  margin-bottom: var(--space-3);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.info-value {
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.description-box {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
}

.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.evidence-image {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform var(--duration-fast);
}

.evidence-image:hover {
  transform: scale(1.02);
}

.handle-result-box {
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.result-status {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

.result-status.status-2 { color: var(--success-600); }
.result-status.status-3 { color: var(--gray-500); }

.result-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.result-content {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: var(--leading-relaxed);
}

.handle-form-container {
  padding: var(--space-6);
}

.form-section {
  margin-bottom: var(--space-5);
}

.form-info-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.info-badge {
  font-size: var(--text-sm);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}

.handle-form {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-5);
}

.form-item {
  margin-bottom: var(--space-5);
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-600);
  margin-bottom: var(--space-2);
  display: block;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.radio-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: var(--space-3);
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  transition: all var(--duration-fast);
}

.radio-option:hover {
  border-color: var(--gray-300);
}

.el-radio__input.is-checked + .el-radio__label .radio-option {
  border-color: var(--primary-500);
  background: var(--primary-50);
}

.radio-label {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
}

.radio-desc {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.checkbox-option {
  font-size: var(--text-sm);
  color: var(--gray-600);
}

.result-textarea {
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
}

.submit-btn {
  min-width: 120px;
}

@media (max-width: 768px) {
  .report-manage-page {
    padding: var(--space-4);
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
  
  .table-header-row {
    flex-direction: column;
    gap: var(--space-3);
    align-items: stretch;
  }
}
</style>