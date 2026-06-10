<template>
  <div class="page-container">
    <div class="page-header-row">
      <h2>操作日志</h2>
      <div class="header-actions">
        <el-button type="danger" @click="clearLogs" :loading="clearing">
          <el-icon><Delete /></el-icon> 清空日志
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="filter-bar">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="searchForm.module" placeholder="请选择模块" clearable>
            <el-option label="用户" value="用户" />
            <el-option label="简历" value="简历" />
            <el-option label="职位" value="职位" />
            <el-option label="匹配" value="匹配" />
            <el-option label="认证" value="认证" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operation" placeholder="请选择操作" clearable>
            <el-option label="登录" value="登录" />
            <el-option label="注册" value="注册" />
            <el-option label="创建" value="创建" />
            <el-option label="更新" value="更新" />
            <el-option label="删除" value="删除" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 日志列表 -->
    <div class="table-card">
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationType(row.operation)" size="small">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="deleteLog(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const clearing = ref(false)
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive({
  username: '',
  module: '',
  operation: ''
})

const getOperationType = (operation) => {
  const types = {
    '登录': 'primary',
    '注册': 'success',
    '创建': 'success',
    '更新': 'warning',
    '删除': 'danger'
  }
  return types[operation] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm
    }
    const res = await request.get('/operation-log/page', { params })
    logs.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('获取日志失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.module = ''
  searchForm.operation = ''
  fetchLogs()
}

const deleteLog = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条日志吗？', '提示', { type: 'warning' })
    await request.delete(`/operation-log/${row.id}`)
    ElMessage.success('删除成功')
    fetchLogs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const clearLogs = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复！', '警告', { type: 'error' })
    clearing.value = true
    await request.delete('/operation-log/clear')
    ElMessage.success('日志已清空')
    fetchLogs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  } finally {
    clearing.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}
</style>
