<template>
  <div class="forum-page">
    <!-- 顶部：职位信息 -->
    <div class="job-header-card" v-if="jobInfo">
      <div class="job-header-left">
        <h2 class="job-title">{{ jobInfo.title }}</h2>
        <div class="job-meta">
          <span v-if="jobInfo.companyInfo">{{ jobInfo.companyInfo.companyName }}</span>
          <span>{{ jobInfo.location || '未设置' }}</span>
          <span>{{ formatSalary(jobInfo.salaryMin, jobInfo.salaryMax) }}</span>
        </div>
      </div>
      <el-button @click="$router.back()">返回职位</el-button>
    </div>

    <!-- 发帖按钮 -->
    <div class="toolbar">
      <span class="post-count">共 {{ total }} 条讨论</span>
      <el-button type="primary" @click="showCreateDialog" :disabled="jobInfo?.status !== 1">
        <el-icon><EditPen /></el-icon> 发起讨论
      </el-button>
    </div>

    <!-- 帖子列表 -->
    <div v-loading="loading">
      <el-empty v-if="!loading && posts.length === 0" description="暂无讨论，快来发起第一条吧" />
      <div v-else class="post-list">
        <div v-for="post in posts" :key="post.id" :id="'post-' + post.id" class="post-card" :class="{ pinned: post.isPinned === 1, 'highlight-flash': highlightPostId === post.id }">
          <div class="post-header">
            <div class="post-header-left">
              <el-tag v-if="post.isPinned === 1" type="warning" size="small" effect="dark">置顶</el-tag>
              <el-tag v-if="post.isClosed === 1" type="info" size="small">已关闭</el-tag>
              <h3 class="post-title" @click="openPostDetail(post)">{{ post.title }}</h3>
            </div>
            <div class="post-actions" v-if="isEmployer && isJobOwner">
              <el-button size="small" :type="post.isPinned === 1 ? 'warning' : 'default'" @click="togglePin(post)">
                {{ post.isPinned === 1 ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button size="small" :type="post.isClosed === 1 ? 'success' : 'warning'" @click="toggleClose(post)">
                {{ post.isClosed === 1 ? '开启讨论' : '关闭讨论' }}
              </el-button>
              <el-button size="small" type="danger" @click="deletePost(post)">删除</el-button>
            </div>
          </div>
          <p class="post-content-preview">{{ post.content }}</p>
          <div class="post-footer">
            <div class="post-author">
              <el-avatar :size="24" >{{ post.username?.charAt(0) }}</el-avatar>
              <span class="author-name">{{ post.username || '匿名用户' }}</span>
              <el-tag v-if="post.userType === 'EMPLOYER'" size="small" type="primary">企业</el-tag>
            </div>
            <div class="post-stats">
              <el-button link size="small" @click="openPostDetail(post)">
                <el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }} 评论
              </el-button>
              <el-button
                v-if="post.userId !== currentUserId"
                link size="small" type="warning"
                @click="openReportDialog(1, post.id, post.title)"
              >
                <el-icon><Warning /></el-icon> 举报
              </el-button>
              <span>{{ formatDate(post.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
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

    <!-- 发帖对话框 -->
    <el-dialog v-model="createDialogVisible" title="发起讨论" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入帖子标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="6" placeholder="请输入讨论内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPost" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情 + 评论对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="currentPost?.title" width="700px" top="5vh">
      <div class="post-detail" v-if="currentPost">
        <div class="post-detail-header">
          <div class="post-author">
            <el-avatar :size="32" >{{ currentPost.username?.charAt(0) }}</el-avatar>
            <div>
              <span class="author-name">{{ currentPost.username || '匿名用户' }}</span>
              <el-tag v-if="currentPost.userType === 'EMPLOYER'" size="small" type="primary">企业</el-tag>
              <div class="post-time">{{ formatDate(currentPost.createTime) }}</div>
            </div>
          </div>
          <el-tag v-if="currentPost.isClosed === 1" type="info">已关闭讨论</el-tag>
        </div>
        <div class="post-detail-content">{{ currentPost.content }}</div>

        <el-divider />

        <!-- 评论区 -->
        <div class="comment-section">
          <h4>评论 ({{ currentPost.commentCount || 0 }})</h4>

          <!-- 评论输入框 -->
          <div class="comment-input" v-if="currentPost.isClosed !== 1">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              :placeholder="replyTo ? `回复 @${replyTo.username}：` : '发表你的评论...'"
            />
            <div class="comment-input-footer">
              <span v-if="replyTo" class="reply-hint">
                回复 @{{ replyTo.username }}
                <el-button link size="small" @click="replyTo = null">取消回复</el-button>
              </span>
              <el-button type="primary" size="small" @click="submitComment" :loading="submittingComment">
                发表评论
              </el-button>
            </div>
          </div>
          <el-alert v-else title="该讨论已关闭，无法发表评论" type="info" show-icon :closable="false" style="margin-bottom: 16px" />

          <!-- 评论树 -->
          <div class="comment-tree" v-loading="loadingComments">
            <el-empty v-if="!loadingComments && comments.length === 0" description="暂无评论" />
            <div v-for="comment in comments" :key="comment.id" :id="'comment-' + comment.id" class="comment-item" :class="{ 'highlight-flash': highlightCommentId === comment.id }">
              <div class="comment-main">
                <el-avatar :size="28" >{{ comment.username?.charAt(0) }}</el-avatar>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="author-name">{{ comment.username || '匿名用户' }}</span>
                    <el-tag v-if="comment.userType === 'EMPLOYER'" size="small" type="primary">企业</el-tag>
                    <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                  </div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <el-button v-if="currentPost.isClosed !== 1" link size="small" @click="setReply(comment)">
                      <el-icon><ChatDotRound /></el-icon> 回复
                    </el-button>
                    <el-button
                      v-if="comment.userId !== currentUserId"
                      link size="small" type="warning"
                      @click="openReportDialog(2, comment.id, comment.content)"
                    >
                      <el-icon><Warning /></el-icon> 举报
                    </el-button>
                    <el-button
                      v-if="canDeleteComment(comment)"
                      link size="small" type="danger"
                      @click="deleteComment(comment)"
                    >
                      <el-icon><Delete /></el-icon> 删除
                    </el-button>
                  </div>

                  <!-- 子评论 -->
                  <div v-if="comment.children && comment.children.length > 0" class="sub-comments">
                    <div v-for="child in comment.children" :key="child.id" class="comment-item sub">
                      <el-avatar :size="24" >{{ child.username?.charAt(0) }}</el-avatar>
                      <div class="comment-body">
                        <div class="comment-header">
                          <span class="author-name">{{ child.username || '匿名用户' }}</span>
                          <el-tag v-if="child.userType === 'EMPLOYER'" size="small" type="primary">企业</el-tag>
                          <span v-if="child.replyToUsername" class="reply-to">回复 @{{ child.replyToUsername }}</span>
                          <span class="comment-time">{{ formatDate(child.createTime) }}</span>
                        </div>
                        <div class="comment-text">{{ child.content }}</div>
                        <div class="comment-actions">
                          <el-button v-if="currentPost.isClosed !== 1" link size="small" @click="setReply(child)">
                            <el-icon><ChatDotRound /></el-icon> 回复
                          </el-button>
                          <el-button
                            v-if="child.userId !== currentUserId"
                            link size="small" type="warning"
                            @click="openReportDialog(2, child.id, child.content)"
                          >
                            <el-icon><Warning /></el-icon> 举报
                          </el-button>
                          <el-button
                            v-if="canDeleteComment(child)"
                            link size="small" type="danger"
                            @click="deleteComment(child)"
                          >
                            <el-icon><Delete /></el-icon> 删除
                          </el-button>
                        </div>

                        <!-- 三级子评论 -->
                        <div v-if="child.children && child.children.length > 0" class="sub-comments">
                          <div v-for="grandchild in child.children" :key="grandchild.id" class="comment-item sub">
                            <el-avatar :size="22" >{{ grandchild.username?.charAt(0) }}</el-avatar>
                            <div class="comment-body">
                              <div class="comment-header">
                                <span class="author-name">{{ grandchild.username || '匿名用户' }}</span>
                                <span v-if="grandchild.replyToUsername" class="reply-to">回复 @{{ grandchild.replyToUsername }}</span>
                                <span class="comment-time">{{ formatDate(grandchild.createTime) }}</span>
                              </div>
                              <div class="comment-text">{{ grandchild.content }}</div>
                              <div class="comment-actions">
                                <el-button v-if="currentPost.isClosed !== 1" link size="small" @click="setReply(grandchild)">
                                  <el-icon><ChatDotRound /></el-icon> 回复
                                </el-button>
                                <el-button
                                  v-if="grandchild.userId !== currentUserId"
                                  link size="small" type="warning"
                                  @click="openReportDialog(2, grandchild.id, grandchild.content)"
                                >
                                  <el-icon><Warning /></el-icon> 举报
                                </el-button>
                                <el-button
                                  v-if="canDeleteComment(grandchild)"
                                  link size="small" type="danger"
                                  @click="deleteComment(grandchild)"
                                >
                                  <el-icon><Delete /></el-icon> 删除
                                </el-button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 举报弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="举报" width="500px">
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="80px">
        <el-form-item label="举报类型">
          <el-tag>{{ reportForm.reportedType === 1 ? '帖子' : '评论' }}</el-tag>
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
            <el-option label="垃圾广告" value="垃圾广告" />
            <el-option label="色情低俗" value="色情低俗" />
            <el-option label="人身攻击" value="人身攻击" />
            <el-option label="虚假信息" value="虚假信息" />
            <el-option label="违法内容" value="违法内容" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            placeholder="请补充说明举报原因（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitReport" :loading="reportSubmitting">
          <el-icon><Warning /></el-icon> 提交举报
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, ChatDotRound, Delete, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const jobId = computed(() => route.params.jobId)
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')
const currentUserId = computed(() => userStore.user?.id)

const jobInfo = ref(null)
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 判断当前用户是否为该职位的企业主
const isJobOwner = computed(() => {
  if (!jobInfo.value || !currentUserId.value) return false
  return jobInfo.value.employerId === currentUserId.value
})

// 发帖
const createDialogVisible = ref(false)
const createFormRef = ref()
const submitting = ref(false)
const createForm = reactive({ title: '', content: '' })
const createRules = {
  title: [{ required: true, message: '请输入帖子标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入讨论内容', trigger: 'blur' }]
}

// 帖子详情
const detailDialogVisible = ref(false)
const currentPost = ref(null)

// 评论
const comments = ref([])
const loadingComments = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)
const replyTo = ref(null)

const formatSalary = (min, max) => {
  if (!min && !max) return '面议'
  const f = v => (v / 1000).toFixed(0) + 'K'
  if (min && max) return `${f(min)} - ${f(max)}`
  return min ? `${f(min)}起` : `最高${f(max)}`
}

const formatDate = d => d ? new Date(d).toLocaleString('zh-CN') : ''

const fetchJobInfo = async () => {
  try {
    jobInfo.value = await request.get(`/job/${jobId.value}`)
  } catch {
    ElMessage.error('获取职位信息失败')
  }
}

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await request.get('/forum/post/list', {
      params: { jobId: jobId.value, page: currentPage.value, size: pageSize.value },
      skipGlobalLoading: true
    })
    posts.value = res.records || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  createForm.title = ''
  createForm.content = ''
  createDialogVisible.value = true
}

const submitPost = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await request.post('/forum/post', {
      jobId: Number(jobId.value),
      title: createForm.title,
      content: createForm.content
    })
    ElMessage.success('发帖成功')
    createDialogVisible.value = false
    fetchPosts()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '发帖失败')
  } finally {
    submitting.value = false
  }
}

const openPostDetail = async (post) => {
  try {
    // 从后端获取最新帖子详情（确保 isClosed 等字段正确）
    const detail = await request.get(`/forum/post/${post.id}`, { skipGlobalLoading: true })
    currentPost.value = detail
  } catch {
    currentPost.value = post
  }
  replyTo.value = null
  commentContent.value = ''
  detailDialogVisible.value = true
  await fetchComments(post.id)
}

const fetchComments = async (postId) => {
  loadingComments.value = true
  try {
    const res = await request.get('/forum/comment/tree', {
      params: { postId },
      skipGlobalLoading: true
    })
    comments.value = res || []
  } catch {
    ElMessage.error('获取评论失败')
  } finally {
    loadingComments.value = false
  }
}

const setReply = (comment) => {
  replyTo.value = { id: comment.id, username: comment.username, userId: comment.userId }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submittingComment.value = true
  try {
    const payload = {
      postId: currentPost.value.id,
      content: commentContent.value.trim()
    }
    if (replyTo.value) {
      payload.parentId = replyTo.value.id
      payload.replyToUserId = replyTo.value.userId
    }
    await request.post('/forum/comment', payload)
    ElMessage.success('评论成功')
    commentContent.value = ''
    replyTo.value = null
    // 从后端获取最新帖子详情（评论数已由后端更新）
    const detail = await request.get(`/forum/post/${currentPost.value.id}`, { skipGlobalLoading: true })
    if (detail) {
      currentPost.value.commentCount = detail.commentCount
    }
    await fetchComments(currentPost.value.id)
    await fetchPosts()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '评论失败')
  } finally {
    submittingComment.value = false
  }
}

const canDeleteComment = (comment) => {
  if (isAdmin.value) return true
  if (comment.userId === currentUserId.value) return true
  if (isEmployer.value && isJobOwner.value) return true
  return false
}

const deleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
    await request.delete(`/forum/comment/${comment.id}`)
    ElMessage.success('删除成功')
    // 从后端获取最新的帖子详情（评论数已由后端更新）
    const detail = await request.get(`/forum/post/${currentPost.value.id}`, { skipGlobalLoading: true })
    if (detail) {
      currentPost.value.commentCount = detail.commentCount
    }
    await fetchComments(currentPost.value.id)
    await fetchPosts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
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
    await ElMessageBox.confirm('确定要删除这个帖子吗？删除后不可恢复。', '提示', { type: 'warning' })
    await request.delete(`/forum/post/${post.id}`)
    ElMessage.success('删除成功')
    fetchPosts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '删除失败')
    }
  }
}

// 举报
const reportDialogVisible = ref(false)
const reportSubmitting = ref(false)
const reportFormRef = ref()
const reportForm = reactive({
  reportedType: 1,
  reportedId: null,
  reason: '',
  description: '',
  // 用于显示
  _targetName: ''
})
const reportRules = {
  reason: [{ required: true, message: '请选择举报原因', trigger: 'change' }]
}

const openReportDialog = (type, id, name) => {
  reportForm.reportedType = type
  reportForm.reportedId = id
  reportForm.reason = ''
  reportForm.description = ''
  reportForm._targetName = name || ''
  reportDialogVisible.value = true
}

const submitReport = async () => {
  const valid = await reportFormRef.value.validate().catch(() => false)
  if (!valid) return

  reportSubmitting.value = true
  try {
    await request.post('/report', {
      reportedType: reportForm.reportedType,
      reportedId: reportForm.reportedId,
      reason: reportForm.reason,
      description: reportForm.description
    })
    ElMessage.success('举报已提交，管理员会尽快处理')
    reportDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '举报提交失败')
  } finally {
    reportSubmitting.value = false
  }
}

// 高亮定位（管理员从举报列表跳转）
const highlightPostId = ref(null)
const highlightCommentId = ref(null)

const handleHighlight = async () => {
  const hp = route.query.highlightPost
  const hc = route.query.highlightComment
  if (hp) {
    highlightPostId.value = Number(hp)
    await nextTick()
    const el = document.getElementById('post-' + hp)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      setTimeout(() => { highlightPostId.value = null }, 3000)
    }
  } else if (hc) {
    // 先查评论所属帖子，打开详情后再滚动
    highlightCommentId.value = Number(hc)
    try {
      const commentRes = await request.get(`/forum/comment/${hc}`)
      if (commentRes?.postId) {
        const post = posts.value.find(p => p.id === commentRes.postId)
        if (post) {
          await openPostDetail(post)
          await nextTick()
          const el = document.getElementById('comment-' + hc)
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' })
            setTimeout(() => { highlightCommentId.value = null }, 3000)
          }
        }
      }
    } catch {
      // ignore
    }
  }
}

onMounted(async () => {
  fetchJobInfo()
  await fetchPosts()
  handleHighlight()
})
</script>

<style scoped>
.forum-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-4);
}

.job-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  margin-bottom: var(--space-4);
}

.job-title {
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-2);
}

.job-meta {
  display: flex;
  gap: var(--space-4);
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.post-count {
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.post-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: all var(--duration-normal) var(--ease-out);
}

.post-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
}

.post-card.pinned {
  border-left: 3px solid var(--color-warning);
  background: var(--warning-50, #fffbeb);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-2);
  gap: var(--space-3);
}

.post-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex: 1;
}

.post-title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0;
  cursor: pointer;
  transition: color var(--duration-fast);
}

.post-title:hover {
  color: var(--color-primary);
}

.post-content-preview {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: var(--leading-relaxed);
  margin: 0 0 var(--space-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}

.post-author {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.author-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-700);
}

.post-stats {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  font-size: var(--text-xs);
  color: var(--gray-400);
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}

.pagination {
  margin-top: var(--space-6);
  display: flex;
  justify-content: flex-end;
}

/* 帖子详情 */
.post-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.post-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
  margin-top: 2px;
}

.post-detail-content {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
}

/* 评论区 */
.comment-section h4 {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin: 0 0 var(--space-4);
}

.comment-input {
  margin-bottom: var(--space-5);
}

.comment-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-2);
}

.reply-hint {
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.comment-tree {
  min-height: 100px;
}

.comment-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3) 0;
}

.comment-item + .comment-item {
  border-top: 1px solid var(--gray-100);
}

.comment-item.sub {
  padding: var(--space-2) 0;
}

.comment-item.sub + .comment-item.sub {
  border-top: 1px solid var(--gray-50);
}

.comment-main {
  display: flex;
  gap: var(--space-3);
  width: 100%;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.comment-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
}

.reply-to {
  font-size: var(--text-xs);
  color: var(--color-primary);
}

.comment-text {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-relaxed);
  margin-bottom: 4px;
}

.comment-actions {
  display: flex;
  gap: var(--space-2);
}

.sub-comments {
  margin-top: var(--space-2);
  padding-left: var(--space-3);
  border-left: 2px solid var(--gray-100);
}

@media (max-width: 768px) {
  .forum-page {
    padding: var(--space-4) var(--space-3);
  }
  .job-header-card {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
  .post-header {
    flex-direction: column;
  }
  .post-actions {
    flex-wrap: wrap;
  }
}

/* 高亮闪烁动画（管理员从举报跳转定位） */
.highlight-flash {
  animation: highlight-pulse 2s ease-in-out;
  border-color: #e6a23c !important;
  box-shadow: 0 0 0 3px rgba(230, 162, 60, 0.3) !important;
}

@keyframes highlight-pulse {
  0%, 100% { background-color: transparent; }
  25% { background-color: rgba(230, 162, 60, 0.15); }
  50% { background-color: rgba(230, 162, 60, 0.08); }
  75% { background-color: rgba(230, 162, 60, 0.15); }
}
</style>
