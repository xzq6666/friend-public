<template>
  <div class="sensitive-word-page page-enter">
    <div class="page-header">
      <h2>敏感词管理</h2>
      <p class="page-subtitle">管理平台敏感词库，拦截不当内容</p>
    </div>

    <!-- 状态筛选 Tab -->
    <div class="stats-row">
      <div class="stat-tab" :class="{ active: activeStatus === 'all' }" @click="setStatusFilter('all')">
        <span class="stat-count">{{ stats.total }}</span>
        <span class="stat-name">全部</span>
      </div>
      <div class="stat-tab" :class="{ active: activeStatus === 'enabled' }" @click="setStatusFilter('enabled')">
        <span class="stat-count">{{ stats.enabled }}</span>
        <span class="stat-name">已启用</span>
      </div>
      <div class="stat-tab" :class="{ active: activeStatus === 'ai' }" @click="setStatusFilter('ai')">
        <span class="stat-count">{{ stats.aiDetected }}</span>
        <span class="stat-name">AI检测</span>
      </div>
      <div class="stat-tab" :class="{ active: activeStatus === 'disabled' }" @click="setStatusFilter('disabled')">
        <span class="stat-count">{{ stats.disabled }}</span>
        <span class="stat-name">已禁用</span>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">搜索</span>
          <el-input v-model="filterKeyword" placeholder="搜索敏感词" clearable style="width: 200px" @keyup.enter="fetchWords">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="filter-item">
          <span class="filter-label">分类</span>
          <el-select v-model="filterCategory" placeholder="全部" clearable style="width: 130px" @change="fetchWords">
            <el-option label="全部" value="" />
            <el-option label="政治" value="政治" />
            <el-option label="色情" value="色情" />
            <el-option label="暴力" value="暴力" />
            <el-option label="广告" value="广告" />
            <el-option label="歧视" value="歧视" />
            <el-option label="自定义" value="自定义" />
            <el-option label="AI检测" value="AI检测" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">来源</span>
          <el-select v-model="filterSource" placeholder="全部" clearable style="width: 120px" @change="fetchWords">
            <el-option label="全部" value="" />
            <el-option label="手动添加" value="manual" />
            <el-option label="AI检测" value="ai" />
          </el-select>
        </div>
        <div class="filter-item">
          <el-button type="primary" @click="fetchWords"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="resetFilter"><el-icon><Refresh /></el-icon> 重置</el-button>
        </div>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <div class="action-bar">
        <el-button type="primary" @click="showAddDialog"><el-icon><Plus /></el-icon> 添加敏感词</el-button>
        <el-button type="success" @click="showBatchDialog"><el-icon><Document /></el-icon> 批量添加</el-button>
        <div class="action-divider"></div>
        <el-button type="danger" :disabled="!selectedIds.length" @click="batchDelete"><el-icon><Delete /></el-icon> 批量删除</el-button>
        <el-button type="success" plain :disabled="!selectedIds.length" @click="batchToggle(1)"><el-icon><CircleCheck /></el-icon> 启用</el-button>
        <el-button plain :disabled="!selectedIds.length" @click="batchToggle(0)"><el-icon><CircleClose /></el-icon> 禁用</el-button>
      </div>

      <el-table v-loading="loading" :data="filteredWords" stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="word" label="敏感词" min-width="160">
          <template #default="{ row }">
            <strong style="color: var(--gray-900)">{{ row.word }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <el-tag :type="getCategoryTag(row.category)" size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="row.source === 'ai' ? 'warning' : 'info'" size="small" effect="plain">
              {{ row.source === 'ai' ? 'AI检测' : '手动添加' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="status-cell">
              <span class="status-dot" :class="row.status === 1 ? 'active' : 'inactive'"></span>
              <span>{{ row.status === 1 ? '启用' : '禁用' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showEditDialog(row)"><el-icon><Edit /></el-icon> 编辑</el-button>
            <el-popconfirm title="确定删除该敏感词吗？" @confirm="deleteWord(row.id)">
              <template #reference>
                <el-button type="danger" link size="small"><el-icon><Delete /></el-icon> 删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchWords"
          @current-change="fetchWords"
        />
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑敏感词' : '添加敏感词'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" placeholder="请输入敏感词" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="政治" value="政治" />
            <el-option label="色情" value="色情" />
            <el-option label="暴力" value="暴力" />
            <el-option label="广告" value="广告" />
            <el-option label="歧视" value="歧视" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">{{ isEdit ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量添加敏感词" width="550px">
      <el-form label-width="80px">
        <el-form-item label="敏感词">
          <el-input v-model="batchWords" type="textarea" :rows="6" placeholder="每行一个敏感词，或用逗号、分号分隔" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="batchCategory" placeholder="请选择分类" style="width: 100%">
            <el-option label="政治" value="政治" />
            <el-option label="色情" value="色情" />
            <el-option label="暴力" value="暴力" />
            <el-option label="广告" value="广告" />
            <el-option label="歧视" value="歧视" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch" :loading="submitting">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, CircleCheck, CircleClose, MagicStick,
  Search, Refresh, Plus, Delete, Edit
} from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const words = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])

const filterKeyword = ref('')
const filterCategory = ref('')
const filterSource = ref('')
const filterStatus = ref(null)
const activeStatus = ref('all')

const stats = reactive({ total: 0, enabled: 0, disabled: 0, aiDetected: 0, manual: 0 })

const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()

const form = reactive({ word: '', category: '自定义', status: 1 })
const formRules = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { min: 2, message: '敏感词长度不能少于2个字符', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const batchWords = ref('')
const batchCategory = ref('自定义')

const filteredWords = computed(() => {
  if (activeStatus.value === 'all') return words.value
  if (activeStatus.value === 'enabled') return words.value.filter(w => w.status === 1)
  if (activeStatus.value === 'disabled') return words.value.filter(w => w.status === 0)
  if (activeStatus.value === 'ai') return words.value.filter(w => w.source === 'ai')
  return words.value
})

const setStatusFilter = (status) => {
  activeStatus.value = status
}

const fetchWords = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filterKeyword.value) params.keyword = filterKeyword.value
    if (filterCategory.value) params.category = filterCategory.value
    if (filterSource.value) params.source = filterSource.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    const res = await request.get('/sensitive-word/list', { params })
    words.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('加载失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await request.get('/sensitive-word/stats')
    Object.assign(stats, res)
  } catch { /* ignore */ }
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterCategory.value = ''
  filterSource.value = ''
  filterStatus.value = null
  activeStatus.value = 'all'
  fetchWords()
}

const handleSelectionChange = (val) => {
  selectedIds.value = val.map(r => r.id)
}

const showAddDialog = () => {
  isEdit.value = false
  editId.value = null
  form.word = ''
  form.category = '自定义'
  form.status = 1
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.word = row.word
  form.category = row.category
  form.status = row.status
  dialogVisible.value = true
}

const showBatchDialog = () => {
  batchWords.value = ''
  batchCategory.value = '自定义'
  batchDialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put(`/sensitive-word/${editId.value}`, { category: form.category, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await request.post('/sensitive-word', { word: form.word, category: form.category, status: form.status })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchWords()
    fetchStats()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '操作失败')
  } finally {
    submitting.value = false
  }
}

const submitBatch = async () => {
  if (!batchWords.value.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  submitting.value = true
  try {
    const res = await request.post('/sensitive-word/batch', { words: batchWords.value, category: batchCategory.value })
    ElMessage.success(res.message || '批量添加成功')
    batchDialogVisible.value = false
    fetchWords()
    fetchStats()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '操作失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/sensitive-word/${row.id}/toggle`)
    fetchStats()
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}

const deleteWord = async (id) => {
  try {
    await request.delete('/sensitive-word', { data: { ids: [id] } })
    ElMessage.success('删除成功')
    fetchWords()
    fetchStats()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个敏感词吗？`, '批量删除', { type: 'warning' })
    await request.delete('/sensitive-word', { data: { ids: selectedIds.value } })
    ElMessage.success('批量删除成功')
    fetchWords()
    fetchStats()
  } catch { /* cancel */ }
}

const batchToggle = async (status) => {
  try {
    const action = status === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确定${action}选中的 ${selectedIds.value.length} 个敏感词吗？`, `批量${action}`, { type: 'warning' })
    await request.put('/sensitive-word/batch-toggle', { ids: selectedIds.value, status })
    ElMessage.success(`批量${action}成功`)
    fetchWords()
    fetchStats()
  } catch { /* cancel */ }
}

const getCategoryTag = (cat) => {
  const map = { '政治': 'danger', '色情': 'danger', '暴力': 'warning', '广告': 'info', '歧视': 'warning', 'AI检测': 'success' }
  return map[cat] || ''
}

const formatDate = (d) => {
  if (!d) return '-'
  const date = new Date(d)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

onMounted(() => {
  fetchWords()
  fetchStats()
})
</script>

<style scoped>
.sensitive-word-page {
  padding: var(--space-6);
}

.action-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-sm);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.active {
  background: var(--success-500);
  box-shadow: 0 0 0 3px var(--success-50);
}

.status-dot.inactive {
  background: var(--gray-400);
  box-shadow: 0 0 0 3px var(--gray-100);
}
</style>
