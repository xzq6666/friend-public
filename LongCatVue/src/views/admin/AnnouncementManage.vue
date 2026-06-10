<template>
  <div class="announcement-manage-page">
    <div class="page-header">
      <h2>公告管理</h2>
      <p class="header-desc">发布和管理系统公告，精准触达目标用户</p>
    </div>

    <div class="main-content">
      <div class="publish-section">
        <div class="section-header">
          <div class="header-icon-wrap">
            <el-icon :size="22" style="color: #fff"><Notification /></el-icon>
          </div>
          <div class="header-text">
            <h3>发布公告</h3>
            <p class="header-subtitle">快速发布系统公告，通知平台用户</p>
          </div>
        </div>

        <el-form :model="form" label-width="0" class="publish-form">
          <div class="form-row">
            <label class="form-label">公告标题</label>
            <el-input 
              v-model="form.title" 
              placeholder="请输入公告标题" 
              maxlength="100" 
              show-word-limit
              class="form-input"
            />
          </div>
          
          <div class="form-row">
            <label class="form-label">公告内容</label>
            <el-input 
              v-model="form.content" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入公告内容（可选，最多500字）" 
              maxlength="500" 
              show-word-limit
              class="form-textarea"
            />
          </div>

          <div class="form-row">
            <label class="form-label">目标用户</label>
            <div class="target-selector">
              <button 
                v-for="target in targetOptions" 
                :key="target.value"
                type="button"
                class="target-btn"
                :class="{ active: form.targetType === target.value }"
                @click="form.targetType = target.value"
              >
                <el-icon :size="16" :style="{ color: form.targetType === target.value ? '#fff' : target.color }"><component :is="target.icon" /></el-icon>
                <span class="btn-text">{{ target.label }}</span>
              </button>
            </div>
          </div>

          <div class="form-actions">
            <el-button type="primary" :loading="publishing" class="submit-btn" @click="publish">
              <el-icon><Promotion /></el-icon> 发布公告
            </el-button>
          </div>
        </el-form>
      </div>

      <div class="list-section">
        <div class="section-header-row">
          <div class="section-title-group">
            <el-icon :size="20" style="color: var(--gray-600); margin-right: var(--space-2)"><Document /></el-icon>
            <span class="section-title">历史公告</span>
            <el-badge :value="total" class="badge" />
          </div>
          <div class="section-actions">
            <el-button type="text" size="small" @click="fetchAnnouncements" class="refresh-action">
              <el-icon :size="16"><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>

        <div class="list-container">
          <div v-if="announcements.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon :size="48" style="color: var(--gray-300)"><Document /></el-icon>
            </div>
            <p class="empty-text">暂无公告</p>
            <p class="empty-hint">点击上方发布按钮创建第一条公告</p>
          </div>

          <div v-else class="announcement-list">
            <div 
              v-for="item in announcements" 
              :key="item.id" 
              class="announcement-card"
            >
              <div class="card-header">
                <div class="title-row">
                  <el-icon :size="18" style="color: var(--primary-500); margin-right: var(--space-2)"><Document /></el-icon>
                  <span class="card-title">{{ item.title }}</span>
                  <el-tag :type="targetTagType(item.targetType)" size="mini" class="target-tag">
                    {{ targetLabel(item.targetType) }}
                  </el-tag>
                </div>
                <span class="card-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <p v-if="item.content" class="card-content">{{ item.content }}</p>
              <p v-else class="card-content-empty">暂无内容</p>
              <div class="card-actions">
                <el-button type="text" size="small" @click="openEdit(item)" class="action-edit">
                  <el-icon :size="14"><Edit /></el-icon> 编辑
                </el-button>
                <el-button type="text" size="small" @click="remove(item.id)" class="action-delete">
                  <el-icon :size="14"><Delete /></el-icon> 删除
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="total > pageSize" class="pagination-area">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchAnnouncements"
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="editVisible" title="编辑公告" width="520px" class="edit-dialog">
      <div class="edit-body">
        <el-form :model="editForm" label-width="0" class="edit-form">
          <div class="form-row">
            <label class="form-label">公告标题</label>
            <el-input 
              v-model="editForm.title" 
              placeholder="请输入公告标题" 
              maxlength="100" 
              show-word-limit
              class="form-input"
            />
          </div>
          
          <div class="form-row">
            <label class="form-label">公告内容</label>
            <el-input 
              v-model="editForm.content" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入公告内容（可选）" 
              maxlength="500" 
              show-word-limit
              class="form-textarea"
            />
          </div>

          <div class="form-row">
            <label class="form-label">目标用户</label>
            <div class="target-selector">
              <button 
                v-for="target in targetOptions" 
                :key="target.value"
                type="button"
                class="target-btn"
                :class="{ active: editForm.targetType === target.value }"
                @click="editForm.targetType = target.value"
              >
                <el-icon :size="16" :style="{ color: editForm.targetType === target.value ? '#fff' : target.color }"><component :is="target.icon" /></el-icon>
                <span class="btn-text">{{ target.label }}</span>
              </button>
            </div>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">
          <el-icon><Check /></el-icon> 保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Notification, Promotion, Document, Refresh, Edit, Delete, Check, UserFilled, User, Briefcase } from '@element-plus/icons-vue'
import request from '../../utils/request'

const form = ref({ title: '', content: '', targetType: 'ALL' })
const publishing = ref(false)
const loading = ref(false)
const announcements = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const editVisible = ref(false)
const editForm = ref({ id: null, title: '', content: '', targetType: 'ALL' })
const saving = ref(false)

const targetOptions = [
  { value: 'ALL', label: '全部用户', icon: UserFilled, color: '#4e6496' },
  { value: 'EMPLOYEE', label: '求职者', icon: User, color: '#3ea15d' },
  { value: 'EMPLOYER', label: '企业用户', icon: Briefcase, color: '#c08a2e' }
]

const targetLabel = (type) => ({ ALL: '全部用户', EMPLOYEE: '求职者', EMPLOYER: '企业用户' }[type] || type)
const targetTagType = (type) => ({ ALL: 'info', EMPLOYEE: 'success', EMPLOYER: 'warning' }[type] || 'info')

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await request.get('/announcement', {
      params: { page: currentPage.value, size: pageSize },
      skipGlobalLoading: true
    })
    announcements.value = res?.records || res || []
    total.value = res?.total || 0
  } catch {
    announcements.value = []
  } finally {
    loading.value = false
  }
}

const publish = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  publishing.value = true
  try {
    await request.post('/announcement', form.value)
    ElMessage.success('公告发布成功')
    form.value = { title: '', content: '', targetType: 'ALL' }
    currentPage.value = 1
    await fetchAnnouncements()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '发布失败')
  } finally {
    publishing.value = false
  }
}

const openEdit = (row) => {
  editForm.value = { id: row.id, title: row.title, content: row.content || '', targetType: row.targetType }
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  saving.value = true
  try {
    await request.put(`/announcement/${editForm.value.id}`, editForm.value)
    ElMessage.success('公告修改成功')
    editVisible.value = false
    await fetchAnnouncements()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '修改失败')
  } finally {
    saving.value = false
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？删除后用户将无法看到此公告。', '提示', { type: 'warning' })
    await request.delete(`/announcement/${id}`)
    ElMessage.success('已删除')
    await fetchAnnouncements()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => { fetchAnnouncements() })
</script>

<style scoped>
.announcement-manage-page {
  padding: var(--space-6);
  max-width: 1400px;
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

.header-desc {
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.main-content {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: var(--space-5);
}

.publish-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  position: sticky;
  top: var(--space-6);
  height: fit-content;
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.header-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-text h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
  margin: 0;
}

.header-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin: 2px 0 0;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-700);
}

.form-input {
  border-radius: var(--radius-md);
  height: 40px;
  padding: 0 var(--space-3);
}

.form-textarea {
  border-radius: var(--radius-md);
  padding: var(--space-3);
}

.target-selector {
  display: flex;
  gap: var(--space-2);
}

.target-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: transparent;
  font-size: var(--text-xs);
  color: var(--gray-600);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.target-btn:hover {
  border-color: var(--gray-300);
  background: var(--gray-50);
}

.target-btn.active {
  border-color: var(--primary-500);
  background: var(--primary-500);
  color: #fff;
}

.btn-text {
  font-weight: var(--weight-medium);
}

.form-actions {
  margin-top: var(--space-2);
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: var(--text-base);
}

.list-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
  background: var(--gray-50);
}

.section-title-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.section-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.badge {
  background: var(--primary-500);
}

.section-actions {
  display: flex;
  gap: var(--space-2);
}

.refresh-action {
  color: var(--gray-600);
}

.list-container {
  padding: var(--space-4);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12) var(--space-6);
}

.empty-icon {
  margin-bottom: var(--space-3);
}

.empty-text {
  font-size: var(--text-base);
  color: var(--gray-600);
  margin: 0 0 var(--space-1);
}

.empty-hint {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin: 0;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.announcement-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  transition: all var(--duration-fast);
}

.announcement-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-3);
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.card-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.card-content {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-3);
  padding-left: 26px;
}

.card-content-empty {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--space-3);
  padding-left: 26px;
  font-style: italic;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.action-edit {
  color: var(--primary-600);
}

.action-delete {
  color: var(--danger-600);
}

.pagination-area {
  display: flex;
  justify-content: center;
  padding: var(--space-4) 0;
  border-top: 1px solid var(--color-border);
  margin-top: var(--space-4);
}

.edit-dialog {
  padding: 0;
}

.edit-body {
  padding: var(--space-6);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .publish-section {
    position: static;
  }
}

@media (max-width: 768px) {
  .announcement-manage-page {
    padding: var(--space-4);
  }
  
  .section-header-row {
    flex-direction: column;
    gap: var(--space-3);
    align-items: stretch;
  }
  
  .target-selector {
    flex-wrap: wrap;
  }
  
  .target-btn {
    min-width: calc(33.33% - 8px);
  }
}
</style>