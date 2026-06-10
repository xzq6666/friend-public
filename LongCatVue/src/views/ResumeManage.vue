<template>
  <div class="resume-manage-page">
    <div class="page-header">
      <div class="header-left">
        <h2>简历管理</h2>
        <p class="header-desc">管理您的所有简历，投递时可灵活选择</p>
      </div>
      <el-button type="primary" @click="goToCreateResume">
        <el-icon><Plus /></el-icon> 创建新简历
      </el-button>
    </div>

    <div v-loading="loading" class="resume-list">
      <el-empty v-if="!loading && resumes.length === 0" description="暂无简历，点击右上角创建">
        <el-button type="primary" @click="goToCreateResume">立即创建</el-button>
      </el-empty>

      <div v-else class="resume-grid">
        <div 
          v-for="resume in resumes" 
          :key="resume.id" 
          class="resume-card"
          :class="{ 'is-default': resume.isDefault === 1 }"
        >
          <div class="card-header">
            <h3 class="resume-name">{{ resume.resumeName || resume.name + '的简历' }}</h3>
            <el-tag v-if="resume.isDefault === 1" type="success" size="small">默认简历</el-tag>
          </div>

          <div class="resume-info">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>{{ resume.name || '未填写' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span>{{ resume.phone || '未填写' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span>{{ resume.email || '未填写' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Briefcase /></el-icon>
              <span>{{ resume.experience || '未填写' }}</span>
            </div>
          </div>

          <div class="resume-note">
            <el-icon><EditPen /></el-icon>
            <el-input
              v-if="editingNoteId === resume.id"
              v-model="editingNoteText"
              type="textarea"
              :rows="2"
              placeholder="输入备注内容，如：适用于前端开发岗位..."
              @blur="saveNote(resume)"
              @keyup.enter="saveNote(resume)"
              ref="noteInputRef"
            />
            <span v-else class="note-text" @click="startEditNote(resume)">
              {{ resume.note || '点击添加备注' }}
            </span>
          </div>

          <div class="card-footer">
            <div class="update-time">
              更新于 {{ formatDate(resume.updateTime) }}
            </div>
            <div class="card-actions">
              <el-button 
                v-if="resume.isDefault !== 1" 
                type="primary" 
                link 
                size="small" 
                @click="handleSetDefault(resume)"
              >
                设为默认
              </el-button>
              <el-button 
                type="primary" 
                link 
                size="small" 
                @click="handleEdit(resume)"
              >
                编辑
              </el-button>
              <el-button 
                type="primary" 
                link 
                size="small" 
                @click="handleCopy(resume)"
              >
                复制
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small" 
                @click="handleDelete(resume)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="tips-card">
      <h4> 使用提示</h4>
      <ul>
        <li>您可以创建多份简历，针对不同职位使用不同简历</li>
        <li>默认简历会在投递时自动选择，也可手动切换</li>
        <li>复制功能可以快速基于现有简历创建新版本</li>
        <li>建议根据岗位需求定制不同版本的简历</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, Phone, Message, Briefcase, EditPen } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const resumes = ref([])
const loading = ref(false)
const editingNoteId = ref(null)
const editingNoteText = ref('')
const noteInputRef = ref(null)

const fetchResumes = async () => {
  loading.value = true
  try {
    const res = await request.get('/resume/my/list', {
      skipErrorNotification: true
    })
    resumes.value = res || []
  } catch (e) {
    if (e.response?.status !== 401) {
      ElMessage.error('加载简历列表失败')
    }
  } finally {
    loading.value = false
  }
}

const goToCreateResume = () => {
  router.push('/my-resume')
}

const handleEdit = (resume) => {
  router.push({ path: '/my-resume', query: { id: resume.id } })
}

const handleSetDefault = async (resume) => {
  try {
    await ElMessageBox.confirm('确定将此简历设为默认吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    await request.put(`/resume/${resume.id}/set-default`)
    ElMessage.success('设置成功')
    await fetchResumes()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '设置失败')
    }
  }
}

const handleCopy = async (resume) => {
  try {
    await ElMessageBox.confirm(`确定要复制简历"${resume.resumeName || resume.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    
    const res = await request.post(`/resume/${resume.id}/copy`)
    ElMessage.success('复制成功')
    await fetchResumes()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '复制失败')
    }
  }
}

const handleDelete = async (resume) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除简历"${resume.resumeName || resume.name}"吗？此操作不可恢复。`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await request.delete(`/resume/${resume.id}`)
    ElMessage.success('删除成功')
    await fetchResumes()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

const startEditNote = (resume) => {
  editingNoteId.value = resume.id
  editingNoteText.value = resume.note || ''
  setTimeout(() => {
    noteInputRef.value?.focus()
  }, 100)
}

const saveNote = async (resume) => {
  try {
    await request.put(`/resume/${resume.id}`, {
      ...resume,
      note: editingNoteText.value
    })
    resume.note = editingNoteText.value
    ElMessage.success('备注已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    editingNoteId.value = null
    editingNoteText.value = ''
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchResumes()
})
</script>

<style scoped>
.resume-manage-page {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.header-left h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
}

.header-desc {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-4);
}

.resume-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: all var(--duration-normal) var(--ease-out);
}

.resume-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.resume-card.is-default {
  border-color: var(--success-500);
  background: linear-gradient(to bottom, var(--success-50), var(--color-surface));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.resume-name {
  margin: 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  flex: 1;
}

.resume-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.info-item .el-icon {
  color: var(--gray-400);
}

.resume-note {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.note-text {
  display: block;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  cursor: pointer;
  line-height: 1.6;
  padding: var(--space-2) 0;
}

.note-text:hover {
  color: var(--gray-700);
}

.resume-note .el-input {
  margin-top: var(--space-1);
}

.resume-note .el-icon {
  display: inline-flex;
  align-items: center;
  margin-right: var(--space-1);
  color: var(--gray-400);
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.update-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.card-actions {
  display: flex;
  gap: var(--space-2);
}

.tips-card {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.tips-card h4 {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.tips-card ul {
  margin: 0;
  padding-left: var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  line-height: 1.8;
}

.tips-card li {
  margin-bottom: var(--space-1);
}
</style>
