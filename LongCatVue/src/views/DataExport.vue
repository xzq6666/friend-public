<template>
  <div class="page-container-full">
    <div class="page-header">
      <h2>数据导出</h2>
      <p class="header-desc">支持 Excel / CSV 格式，可自定义字段和日期范围</p>
    </div>

    <!-- 导出卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in exportItems" :key="item.key">
        <el-card shadow="hover" class="export-card" @click="openExportDialog(item)">
          <div class="export-icon" :style="{ background: item.color }">
            <el-icon :size="32"><component :is="item.icon" /></el-icon>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
          <el-button type="primary" plain @click.stop="quickExport(item.key)">
            <el-icon><Download /></el-icon> 快速导出
          </el-button>
          <el-button type="default" plain size="small" @click.stop="openExportDialog(item)" class="btn-advanced">
            <el-icon><Setting /></el-icon> 高级
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导出历史 -->
    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="history-header">
          <span>导出历史</span>
          <el-button type="primary" link size="small" @click="clearHistory">
            <el-icon><Delete /></el-icon> 清空记录
          </el-button>
        </div>
      </template>
      <el-table :data="exportHistory" stripe size="small">
        <el-table-column label="导出时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.time) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ getTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="格式" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.format === 'xlsx' ? 'success' : 'info'">{{ row.format.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="记录数" width="100">
          <template #default="{ row }">{{ row.count || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'success' ? 'success' : 'danger'">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="exportHistory.length === 0" description="暂无导出记录" :image-size="60" />
    </el-card>

    <!-- 高级导出对话框 -->
    <el-dialog v-model="dialogVisible" title="高级导出设置" width="600px">
      <div v-if="currentItem" class="export-dialog">
        <!-- 数据预览 -->
        <div class="preview-bar">
          <el-icon><DataLine /></el-icon>
          <span>预计导出 <strong>{{ previewCount }}</strong> 条记录</span>
          <el-button type="primary" link size="small" @click="refreshPreview" :loading="previewLoading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>

        <!-- 格式选择 -->
        <div class="setting-section">
          <label class="setting-label">导出格式</label>
          <el-radio-group v-model="exportForm.format">
            <el-radio value="xlsx">
              <el-icon><Document /></el-icon> Excel (.xlsx)
            </el-radio>
            <el-radio value="csv">
              <el-icon><DocumentCopy /></el-icon> CSV (.csv)
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 日期范围 -->
        <div class="setting-section">
          <label class="setting-label">日期范围</label>
          <el-date-picker
            v-model="exportForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            style="width: 100%"
            @change="refreshPreview"
          />
        </div>

        <!-- 字段选择 -->
        <div class="setting-section">
          <label class="setting-label">
            字段选择
            <el-button type="primary" link size="small" @click="toggleSelectAll">
              {{ allFieldsSelected ? '取消全选' : '全选' }}
            </el-button>
          </label>
          <el-checkbox-group v-model="exportForm.fields" class="field-grid">
            <el-checkbox
              v-for="field in currentItem.fields"
              :key="field.key"
              :value="field.key"
              class="field-item"
            >
              {{ field.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExport" :loading="exporting" :disabled="exportForm.fields.length === 0">
          <el-icon><Download /></el-icon> 确认导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download, User, Document, Briefcase, Connection,
  Setting, DataLine, Refresh, Delete, DocumentCopy
} from '@element-plus/icons-vue'
import { getExportPreview, doExport } from '../api/export'

// 导出类型配置
const exportItems = [
  {
    key: 'users',
    title: '用户数据',
    desc: '导出所有注册用户信息',
    icon: 'User',
    color: '#409EFF',
    fields: [
      { key: 'id', label: '用户ID' },
      { key: 'username', label: '用户名' },
      { key: 'email', label: '邮箱' },
      { key: 'phone', label: '手机号' },
      { key: 'userType', label: '用户类型' },
      { key: 'createTime', label: '注册时间' },
      { key: 'status', label: '状态' },
    ],
  },
  {
    key: 'resumes',
    title: '简历数据',
    desc: '导出所有求职者简历',
    icon: 'Document',
    color: '#67C23A',
    fields: [
      { key: 'id', label: '简历ID' },
      { key: 'name', label: '姓名' },
      { key: 'age', label: '年龄' },
      { key: 'gender', label: '性别' },
      { key: 'phone', label: '手机号' },
      { key: 'email', label: '邮箱' },
      { key: 'education', label: '学历' },
      { key: 'skills', label: '技能' },
      { key: 'workExperience', label: '工作经验' },
      { key: 'expectedSalary', label: '期望薪资' },
      { key: 'createTime', label: '创建时间' },
    ],
  },
  {
    key: 'jobs',
    title: '职位数据',
    desc: '导出所有企业发布的职位',
    icon: 'Briefcase',
    color: '#E6A23C',
    fields: [
      { key: 'id', label: '职位ID' },
      { key: 'title', label: '职位名称' },
      { key: 'company', label: '公司名称' },
      { key: 'location', label: '工作地点' },
      { key: 'salaryMin', label: '最低薪资' },
      { key: 'salaryMax', label: '最高薪资' },
      { key: 'experienceRequired', label: '经验要求' },
      { key: 'educationRequired', label: '学历要求' },
      { key: 'status', label: '状态' },
      { key: 'createTime', label: '发布时间' },
    ],
  },
  {
    key: 'match-records',
    title: '匹配记录',
    desc: '导出所有匹配记录数据',
    icon: 'Connection',
    color: '#9B59B6',
    fields: [
      { key: 'id', label: '记录ID' },
      { key: 'jobTitle', label: '职位名称' },
      { key: 'candidateName', label: '候选人' },
      { key: 'matchScore', label: '匹配分数' },
      { key: 'skillsScore', label: '技能匹配' },
      { key: 'experienceScore', label: '经验匹配' },
      { key: 'educationScore', label: '学历匹配' },
      { key: 'createTime', label: '匹配时间' },
    ],
  },
]

// 状态
const loadingKey = ref(null)
const dialogVisible = ref(false)
const currentItem = ref(null)
const exporting = ref(false)
const previewCount = ref(0)
const previewLoading = ref(false)
const exportHistory = ref([])

// 导出表单
const exportForm = reactive({
  format: 'xlsx',
  dateRange: null,
  fields: [],
})

// 日期快捷选项
const dateShortcuts = [
  { text: '最近7天', value: () => { const d = new Date(); d.setDate(d.getDate() - 7); return [d, new Date()] } },
  { text: '最近30天', value: () => { const d = new Date(); d.setDate(d.getDate() - 30); return [d, new Date()] } },
  { text: '最近90天', value: () => { const d = new Date(); d.setDate(d.getDate() - 90); return [d, new Date()] } },
  { text: '本月', value: () => { const now = new Date(); return [new Date(now.getFullYear(), now.getMonth(), 1), new Date()] } },
]

// 计算属性
const allFieldsSelected = computed(() => {
  return currentItem.value && exportForm.fields.length === currentItem.value.fields.length
})

// 方法
const getTypeLabel = (key) => {
  const item = exportItems.find(i => i.key === key)
  return item?.title || key
}

const formatDateTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const openExportDialog = (item) => {
  currentItem.value = item
  exportForm.format = 'xlsx'
  exportForm.dateRange = null
  exportForm.fields = item.fields.map(f => f.key)
  dialogVisible.value = true
  refreshPreview()
}

const toggleSelectAll = () => {
  if (allFieldsSelected.value) {
    exportForm.fields = []
  } else {
    exportForm.fields = currentItem.value.fields.map(f => f.key)
  }
}

const refreshPreview = async () => {
  if (!currentItem.value) return
  previewLoading.value = true
  try {
    const params = {}
    if (exportForm.dateRange?.length === 2) {
      params.startDate = exportForm.dateRange[0]
      params.endDate = exportForm.dateRange[1]
    }
    const res = await getExportPreview(currentItem.value.key, params)
    previewCount.value = res?.count || 0
  } catch {
    previewCount.value = 0
  } finally {
    previewLoading.value = false
  }
}

const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}

const handleExport = async () => {
  if (!currentItem.value || exportForm.fields.length === 0) return
  exporting.value = true
  const historyEntry = {
    time: new Date().toISOString(),
    type: currentItem.value.key,
    format: exportForm.format,
    count: previewCount.value,
    status: 'pending',
  }
  try {
    const params = {
      format: exportForm.format,
      fields: exportForm.fields,
    }
    if (exportForm.dateRange?.length === 2) {
      params.startDate = exportForm.dateRange[0]
      params.endDate = exportForm.dateRange[1]
    }
    const blob = await doExport(currentItem.value.key, params)
    const ext = exportForm.format === 'xlsx' ? 'xlsx' : 'csv'
    const filename = `${currentItem.value.key}_${new Date().toISOString().slice(0, 10)}.${ext}`
    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
    historyEntry.status = 'success'
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error('导出失败：' + error.message)
    historyEntry.status = 'failed'
  } finally {
    exporting.value = false
    exportHistory.value.unshift(historyEntry)
    saveHistory()
  }
}

const quickExport = async (key) => {
  loadingKey.value = key
  const item = exportItems.find(i => i.key === key)
  const historyEntry = {
    time: new Date().toISOString(),
    type: key,
    format: 'xlsx',
    count: 0,
    status: 'pending',
  }
  try {
    // 先获取记录数
    try {
      const previewRes = await getExportPreview(key, {})
      historyEntry.count = previewRes?.count || 0
    } catch {
      historyEntry.count = 0
    }
    
    const blob = await doExport(key, { format: 'xlsx' })
    const filename = `${key}_${new Date().toISOString().slice(0, 10)}.xlsx`
    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
    historyEntry.status = 'success'
  } catch (error) {
    ElMessage.error('导出失败：' + error.message)
    historyEntry.status = 'failed'
  } finally {
    loadingKey.value = null
    exportHistory.value.unshift(historyEntry)
    saveHistory()
  }
}

const clearHistory = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有导出记录？', '提示', { type: 'warning' })
    exportHistory.value = []
    localStorage.removeItem('export_history')
  } catch { /* cancel */ }
}

const saveHistory = () => {
  try {
    localStorage.setItem('export_history', JSON.stringify(exportHistory.value.slice(0, 20)))
  } catch { /* ignore */ }
}

const loadHistory = () => {
  try {
    const saved = localStorage.getItem('export_history')
    if (saved) exportHistory.value = JSON.parse(saved)
  } catch { /* ignore */ }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.page-container-full {
  padding: var(--space-6);
}

.page-header {
  margin-bottom: var(--space-6);
}

.page-header h2 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.header-desc {
  margin: var(--space-1) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* ── 导出卡片 ─────────────────────────────── */
.export-card {
  text-align: center;
  padding: var(--space-6) var(--space-4);
  cursor: pointer;
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border) !important;
  transition: all var(--duration-normal) var(--ease-out);
}

.export-card:hover {
  border-color: var(--gray-300) !important;
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg) !important;
}

.export-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-4);
  color: var(--color-surface);
}

.export-card h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.export-card p {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  min-height: 40px;
}

.btn-advanced {
  margin-left: var(--space-2);
}

/* ── 导出历史 ─────────────────────────────── */
.history-card {
  margin-top: var(--space-6);
  border-radius: var(--radius-lg) !important;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ── 高级导出对话框 ───────────────────────── */
.export-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.preview-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.preview-bar strong {
  color: var(--gray-900);
  font-size: var(--text-lg);
}

.setting-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.setting-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.field-item {
  margin: 0 !important;
  font-size: var(--text-sm);
}
</style>
