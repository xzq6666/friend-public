<template>
  <div class="manage-forum-page">
    <div class="page-header-row">
      <h2>论坛管理</h2>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="selectedJobId" placeholder="选择职位" clearable @change="fetchPosts" style="width: 240px">
        <el-option v-for="job in myJobs" :key="job.id" :label="job.title" :value="job.id" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" clearable @change="fetchPosts" style="width: 120px">
        <el-option label="正常" :value="1" />
        <el-option label="已删除" :value="0" />
      </el-select>
    </div>

    <!-- 帖子列表 -->
    <div class="table-card">
      <el-table :data="posts" v-loading="loading" stripe>
        <el-table-column prop="title" label="帖子标题" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.isPinned === 1" type="warning" size="small">置顶</el-tag>
              <el-tag v-if="row.isClosed === 1" type="info" size="small">已关闭</el-tag>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发帖人" width="120">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="commentCount" label="评论数" width="80" />
        <el-table-column label="关联职位" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ getJobTitle(row.jobId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" :type="row.isPinned === 1 ? 'warning' : 'default'" @click="togglePin(row)">
              {{ row.isPinned === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button v-if="row.status === 1" size="small" :type="row.isClosed === 1 ? 'success' : 'warning'" @click="toggleClose(row)">
              {{ row.isClosed === 1 ? '开启' : '关闭' }}
            </el-button>
            <el-button v-if="row.status === 1" size="small" type="danger" @click="deletePost(row)">删除</el-button>
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
          @size-change="fetchPosts"
          @current-change="fetchPosts"
        />
      </div>
    </div>

    <!-- 评论管理对话框 -->
    <el-dialog v-model="commentDialogVisible" title="评论管理" width="700px">
      <div v-loading="loadingComments">
        <el-empty v-if="!loadingComments && postComments.length === 0" description="暂无评论" />
        <div v-for="comment in postComments" :key="comment.id" class="comment-manage-item">
          <div class="comment-manage-header">
            <span class="author-name">{{ comment.username || '匿名' }}</span>
            <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
          </div>
          <div class="comment-manage-content">{{ comment.content }}</div>
          <el-button size="small" type="danger" @click="deleteComment(comment)">删除</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

const myJobs = ref([])
const selectedJobId = ref(null)
const statusFilter = ref(1)
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const commentDialogVisible = ref(false)
const postComments = ref([])
const loadingComments = ref(false)

const formatDate = d => d ? new Date(d).toLocaleString('zh-CN') : ''

const getJobTitle = (jobId) => {
  const job = myJobs.value.find(j => j.id === jobId)
  return job?.title || `职位#${jobId}`
}

const fetchMyJobs = async () => {
  try {
    const res = await request.get('/job', {
      params: { page: 1, size: 100 },
      skipGlobalLoading: true
    })
    myJobs.value = res.records || []
  } catch {
    // ignore
  }
}

const fetchPosts = async () => {
  if (!selectedJobId.value && myJobs.value.length > 0) {
    selectedJobId.value = myJobs.value[0].id
  }
  if (!selectedJobId.value) return

  loading.value = true
  try {
    const res = await request.get('/forum/post/list', {
      params: {
        jobId: selectedJobId.value,
        page: currentPage.value,
        size: pageSize.value
      },
      skipGlobalLoading: true
    })
    let records = res.records || []
    if (statusFilter.value !== null && statusFilter.value !== undefined) {
      records = records.filter(p => p.status === statusFilter.value)
    }
    posts.value = records
    total.value = res.total || 0
  } catch {
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

const togglePin = async (post) => {
  try {
    await request.put(`/forum/post/${post.id}/pin`)
    ElMessage.success(post.isPinned === 1 ? '已取消置顶' : '已置顶')
    fetchPosts()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  }
}

const toggleClose = async (post) => {
  try {
    await request.put(`/forum/post/${post.id}/close`)
    ElMessage.success(post.isClosed === 1 ? '已开启讨论' : '已关闭讨论')
    fetchPosts()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  }
}

const deletePost = async (post) => {
  try {
    await ElMessageBox.confirm('确定要删除这个帖子吗？', '提示', { type: 'warning' })
    await request.delete(`/forum/post/${post.id}`)
    ElMessage.success('删除成功')
    fetchPosts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

const deleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
    await request.delete(`/forum/comment/${comment.id}`)
    ElMessage.success('删除成功')
    postComments.value = postComments.value.filter(c => c.id !== comment.id)
    fetchPosts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

onMounted(() => {
  fetchMyJobs().then(() => fetchPosts())
})
</script>

<style scoped>
.manage-forum-page {
  padding: var(--space-5);
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.page-header-row h2 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.filter-bar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.table-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.title-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.pagination-wrapper {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
}

.comment-manage-item {
  padding: var(--space-3);
  border: 1px solid var(--gray-100);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
}

.comment-manage-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.comment-manage-content {
  font-size: var(--text-sm);
  color: var(--gray-700);
  margin-bottom: var(--space-2);
}

.author-name {
  font-weight: var(--weight-medium);
  color: var(--gray-700);
}

.comment-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
}
</style>
