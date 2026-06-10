<template>
  <div class="page-container">
    <div class="page-header-row">
      <h2>关键词管理</h2>
      <div class="page-header-actions">
        <el-button @click="downloadTemplate">
          <el-icon><Download /></el-icon>下载模板
        </el-button>
        <el-button @click="exportKeywords">
          <el-icon><Download /></el-icon>导出关键词
        </el-button>
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
        >
          <el-button type="success">
            <el-icon><Upload /></el-icon>导入关键词
          </el-button>
        </el-upload>
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>添加关键词
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索关键词"
        clearable
        style="width: 280px;"
        @keyup.enter="fetchKeywords"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select
        v-model="filterCategory"
        placeholder="选择分类"
        clearable
        style="width: 150px;"
        @change="fetchKeywords"
      >
        <el-option label="编程语言" value="编程语言" />
        <el-option label="框架/库" value="框架/库" />
        <el-option label="数据库" value="数据库" />
        <el-option label="工具/平台" value="工具/平台" />
        <el-option label="软技能" value="软技能" />
        <el-option label="行业领域" value="行业领域" />
        <el-option label="区块链" value="区块链" />
        <el-option label="人工智能" value="人工智能" />
      </el-select>
      <el-button type="primary" @click="fetchKeywords">搜索</el-button>
    </div>

    <div class="table-card">
      <el-table :data="keywords" v-loading="loading" stripe>
        <el-table-column prop="keyword" label="关键词" min-width="150" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100">
          <template #default="{ row }">{{ row.source === 'MANUAL' ? '手动添加' : '系统提取' }}</template>
        </el-table-column>
        <el-table-column prop="usageCount" label="使用次数" width="100" sortable />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="editKeyword(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="deleteKeyword(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchKeywords"
          @current-change="fetchKeywords"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑关键词' : '添加关键词'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="form.keyword" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="编程语言" value="编程语言" />
            <el-option label="框架/库" value="框架/库" />
            <el-option label="数据库" value="数据库" />
            <el-option label="工具/平台" value="工具/平台" />
            <el-option label="软技能" value="软技能" />
            <el-option label="行业领域" value="行业领域" />
            <el-option label="区块链" value="区块链" />
            <el-option label="人工智能" value="人工智能" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入关键词描述（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存修改' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Download, Upload } from '@element-plus/icons-vue'
import request from '../utils/request'

const keywords = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterCategory = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const editingId = ref(null)

const form = ref({
  keyword: '',
  category: '',
  description: ''
})

const rules = {
  keyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const fetchKeywords = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      category: filterCategory.value
    }
    const res = await request.get('/admin/keywords/list', { params })
    const data = res.data || {}
    const allKeywords = []
    for (const category in data) {
      if (Array.isArray(data[category])) {
        allKeywords.push(...data[category])
      }
    }

    let filtered = allKeywords
    if (searchKeyword.value) {
      filtered = filtered.filter(k => k.keyword.toLowerCase().includes(searchKeyword.value.toLowerCase()))
    }
    if (filterCategory.value) {
      filtered = filtered.filter(k => k.category === filterCategory.value)
    }

    total.value = filtered.length
    const start = (currentPage.value - 1) * pageSize.value
    keywords.value = filtered.slice(start, start + pageSize.value)
  } catch (error) {
    ElMessage.error('获取关键词列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.value = { keyword: '', category: '', description: '' }
  dialogVisible.value = true
}

const editKeyword = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    keyword: row.keyword,
    category: row.category,
    description: row.description || ''
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put(`/admin/keywords/${editingId.value}`, form.value)
      ElMessage.success('关键词更新成功')
    } else {
      await request.post('/admin/keywords/add', form.value)
      ElMessage.success('关键词添加成功')
    }
    dialogVisible.value = false
    fetchKeywords()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const deleteKeyword = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该关键词吗？', '警告', { type: 'warning' })
    await request.delete(`/admin/keywords/${row.id}`)
    ElMessage.success('关键词已删除')
    fetchKeywords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

const downloadTemplate = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/admin/keywords/export-template', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '关键词导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败')
    console.error(error)
  }
}

const exportKeywords = async () => {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/admin/keywords/export', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '关键词列表.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('关键词导出成功')
  } catch (error) {
    ElMessage.error('关键词导出失败')
    console.error(error)
  }
}

const handleFileChange = async (uploadFile) => {
  const formData = new FormData()
  formData.append('file', uploadFile.raw)

  try {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/admin/keywords/import-excel', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData
    })
    const data = await res.json()
    if (data.success) {
      ElMessage.success(`导入完成：成功 ${data.successCount} 条，重复 ${data.duplicateCount} 条，失败 ${data.failCount} 条`)
      fetchKeywords()
    } else {
      ElMessage.error(data.error || '导入失败')
    }
  } catch (error) {
    ElMessage.error('文件上传失败')
    console.error(error)
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchKeywords()
})
</script>

<style scoped>
.page-container {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.page-header-row h2 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.filter-bar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-lg);
}

.table-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.pagination-wrapper {
  margin-top: var(--space-5);
  display: flex;
  justify-content: flex-end;
}
</style>
