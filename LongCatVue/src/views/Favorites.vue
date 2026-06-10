<template>
  <div class="favorites-root">
  <div class="favorites-page">
    <div class="page-header">
      <div class="header-main">
        <h2>{{ isEmployer ? '收藏的简历' : '我的收藏' }}</h2>
        <p class="header-desc">管理您收藏的职位和简历</p>
      </div>
    </div>

    <div class="favorites-layout">
      <!-- 文件夹侧边栏 -->
      <div class="folder-sidebar">
        <div class="sidebar-header">
          <h4>文件夹</h4>
          <el-button type="primary" link size="small" @click="openFolderDialog()">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="folder-list">
          <div
            class="folder-item"
            :class="{ active: selectedFolderId === null }"
            @click="selectFolder(null)"
          >
            <el-icon><FolderOpened /></el-icon>
            <span class="folder-name">全部收藏</span>
            <span class="folder-count">{{ totalCount }}</span>
          </div>
          <div
            v-for="folder in (folders || [])"
            :key="folder.id || Math.random()"
            class="folder-item"
            :class="{ active: selectedFolderId === folder.id }"
            @click="selectFolder(folder.id)"
          >
            <el-icon><Folder /></el-icon>
            <span class="folder-name">{{ folder.name }}</span>
            <span class="folder-count">{{ folder.count || 0 }}</span>
            <div class="folder-actions">
              <el-icon class="action-icon" @click.stop="openFolderDialog(folder)"><Edit /></el-icon>
              <el-icon class="action-icon delete" @click.stop="handleDeleteFolder(folder)"><FolderDelete /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="favorites-main">
    <!-- 类型切换 -->
    <div class="type-tabs">
      <button
        v-if="isEmployee || isAdmin"
        class="type-tab"
        :class="{ active: activeTab === 'jobs' }"
        @click="activeTab = 'jobs'; fetchFavorites()"
      >
        <el-icon><Briefcase /></el-icon>
        收藏职位
        <span class="tab-badge" v-if="favoriteJobs && favoriteJobs.length">{{ favoriteJobs.length }}</span>
      </button>
      <button
        v-if="isEmployer || isAdmin"
        class="type-tab"
        :class="{ active: activeTab === 'resumes' }"
        @click="activeTab = 'resumes'; fetchFavorites()"
      >
        <el-icon><Document /></el-icon>
        收藏简历
        <span class="tab-badge" v-if="favoriteResumes && favoriteResumes.length">{{ favoriteResumes.length }}</span>
      </button>
    </div>

    <!-- 收藏职位 -->
    <div v-if="activeTab === 'jobs'" v-loading="loading">
      <el-empty v-if="!loading && favoriteJobs.length === 0" :description="selectedFolderId ? '该文件夹暂无收藏职位' : '暂无收藏职位'">
        <el-button v-if="!selectedFolderId" type="primary" plain @click="$router.push('/browse-jobs')">去浏览职位</el-button>
      </el-empty>

      <div v-else class="card-list">
        <div v-for="item in (favoriteJobs || [])" :key="item.id || Math.random()" class="fav-card">
          <div class="card-body">
            <div class="card-main">
              <h3 class="title">{{ item.job_title || '未知职位' }}</h3>
              <div class="meta-row">
                <span class="meta-item"><el-icon><Location /></el-icon>{{ item.job_location || '未知地点' }}</span>
                <span class="meta-item"><el-icon><Money /></el-icon>{{ formatSalary(item.salary_min, item.salary_max) }}</span>
                <span class="meta-item"><el-icon><OfficeBuilding /></el-icon>{{ item.employer_name || '未知企业' }}</span>
              </div>
              <div class="collect-time">
                <el-icon><Clock /></el-icon>
                {{ formatDate(item.create_time) }} 收藏
              </div>
            </div>
            <div class="card-actions">
              <el-button type="default" plain size="small" @click="openMoveDialog(item)">
                <el-icon><Folder /></el-icon> 移动
              </el-button>
              <el-button type="danger" plain size="small" @click="removeJobFavorite(item)">
                <el-icon><Delete /></el-icon> 取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 收藏简历 -->
    <div v-if="activeTab === 'resumes'" v-loading="loading">
      <el-empty v-if="!loading && favoriteResumes.length === 0" description="暂无收藏简历" />

      <div v-else class="card-list">
        <div v-for="item in (favoriteResumes || [])" :key="item.id || Math.random()" class="fav-card">
          <div class="card-body">
            <div class="card-main">
              <h3 class="title">{{ item.resume_name || '未知简历' }}</h3>
              <div class="meta-row">
                <span class="meta-item"><el-icon><User /></el-icon>求职者：{{ item.applicant_name || '未知' }}</span>
                <span class="meta-item"><el-icon><School /></el-icon>{{ item.resume_education || '未知' }}</span>
                <span class="meta-item"><el-icon><Briefcase /></el-icon>{{ item.resume_experience || '未知' }}</span>
              </div>
              <div class="skills" v-if="parseSkills(item.resume_skills).length">
                <el-tag v-for="skill in parseSkills(item.resume_skills)" :key="skill" size="small">{{ skill }}</el-tag>
              </div>
              <div class="collect-time">
                <el-icon><Clock /></el-icon>
                {{ formatDate(item.create_time) }} 收藏
              </div>
            </div>
            <div class="card-actions">
              <el-button type="primary" plain size="small" @click="viewResumeDetail(item)">
                <el-icon><View /></el-icon> 查看详情
              </el-button>
              <el-button type="default" plain size="small" @click="openMoveDialog(item)">
                <el-icon><Folder /></el-icon> 移动
              </el-button>
              <el-button type="danger" plain size="small" @click="removeResumeFavorite(item)">
                <el-icon><Delete /></el-icon> 取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
      </div><!-- end favorites-main -->
    </div><!-- end favorites-layout -->
  </div><!-- end page-container -->

  <!-- 文件夹对话框 -->
  <el-dialog
    v-model="folderDialogVisible"
    :title="editingFolder ? '重命名文件夹' : '新建文件夹'"
    width="400px"
    :append-to-body="false"
    :destroy-on-close="true"
  >
    <el-form ref="folderFormRef" :model="folderForm" :rules="folderFormRules" label-width="80px">
      <el-form-item label="文件夹名" prop="name">
        <el-input v-model="folderForm.name" placeholder="请输入文件夹名称" maxlength="20" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="folderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitFolder">{{ editingFolder ? '保存' : '创建' }}</el-button>
    </template>
  </el-dialog>

  <!-- 移动收藏对话框 -->
  <el-dialog
    v-model="moveDialogVisible"
    title="移动到文件夹"
    width="400px"
    :append-to-body="false"
    :destroy-on-close="true"
  >
    <div class="move-folder-list">
      <div class="move-folder-item" @click="confirmMove(null)">
        <el-icon><FolderOpened /></el-icon>
        <span>未分类</span>
      </div>
      <div
        v-for="folder in (folders || [])"
        :key="folder.id || Math.random()"
        class="move-folder-item"
        @click="confirmMove(folder.id)"
      >
        <el-icon><Folder /></el-icon>
        <span>{{ folder.name }}</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="moveDialogVisible = false">取消</el-button>
    </template>
  </el-dialog>
  </div><!-- end favorites-root -->
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onBeforeUnmount, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Money, User, Clock, Document, Briefcase, OfficeBuilding, School, Delete, Folder, FolderOpened, Plus, Edit, FolderDelete, View } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { getFolders, createFolder, renameFolder, deleteFolder, moveToFolder, getFavoriteJobs, getFavoriteResumes } from '../api/favorite'
import { useRouter, useRoute } from 'vue-router'

// 请求取消控制器
let currentFetchController = null

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isEmployee = computed(() => userStore.user?.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

const activeTab = ref('jobs') // 默认值
const loading = ref(false)
const favoriteJobs = ref([])
const favoriteResumes = ref([])
const allFavoriteJobs = ref([])
const allFavoriteResumes = ref([])
const totalCount = computed(() => {
  try {
    return (allFavoriteJobs.value?.length || 0) + (allFavoriteResumes.value?.length || 0)
  } catch {
    return 0
  }
})

// 文件夹相关
const folders = ref([])
const selectedFolderId = ref(null)
const folderDialogVisible = ref(false)
const folderFormRef = ref()
const folderForm = reactive({ id: null, name: '' })
const folderFormRules = {
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { max: 20, message: '名称不能超过20个字符', trigger: 'blur' },
  ]
}
const editingFolder = ref(false)
const movingItemId = ref(null)
const moveDialogVisible = ref(false)

const fetchFolders = async () => {
  try {
    const res = await getFolders()
    console.log('[Favorites] fetchFolders result:', res)
    // 处理不同的响应格式
    let folderData = []
    if (Array.isArray(res)) {
      folderData = res
    } else if (res && Array.isArray(res.data)) {
      folderData = res.data
    }
    folders.value = folderData
    console.log('[Favorites] folders after update:', folders.value)
  } catch (error) {
    folders.value = []
    console.warn('加载文件夹失败:', error?.message || error)
  }
}

const selectFolder = (id) => {
  selectedFolderId.value = id
  fetchFavorites()
}

const fetchFavorites = async () => {
  // 取消之前的请求（如果存在）
  if (currentFetchController) {
    currentFetchController.abort()
  }
  currentFetchController = new AbortController()

  loading.value = true
  try {
    // 始终加载全部收藏用于计算 totalCount，同时加载当前选中文件夹的数据
    const [allJobsRes, allResumesRes, jobsRes, resumesRes] = await Promise.allSettled([
      getFavoriteJobs(null),
      getFavoriteResumes(null),
      getFavoriteJobs(selectedFolderId.value),
      getFavoriteResumes(selectedFolderId.value)
    ])

    // 处理全部收藏数据
    const processResult = (result) => {
      if (result.status === 'fulfilled') {
        const data = result.value
        if (Array.isArray(data)) return data
        if (data && Array.isArray(data.data)) return data.data
        if (data && Array.isArray(data.records)) return data.records
      }
      return []
    }

    allFavoriteJobs.value = processResult(allJobsRes)
    allFavoriteResumes.value = processResult(allResumesRes)
    favoriteJobs.value = processResult(jobsRes)
    favoriteResumes.value = processResult(resumesRes)

    console.log('[Favorites] fetchFavorites completed:', {
      allJobs: allFavoriteJobs.value.length,
      allResumes: allFavoriteResumes.value.length,
      currentJobs: favoriteJobs.value.length,
      currentResumes: favoriteResumes.value.length
    })
  } catch (error) {
    favoriteJobs.value = []
    favoriteResumes.value = []
    allFavoriteJobs.value = []
    allFavoriteResumes.value = []
    if (error?.response?.status !== 401 && error?.name !== 'AbortError') {
      console.error('获取收藏列表失败:', error)
    }
  } finally {
    loading.value = false
    currentFetchController = null
  }
}

// 文件夹 CRUD
const openFolderDialog = (folder = null) => {
  editingFolder.value = !!folder
  folderForm.id = folder?.id || null
  folderForm.name = folder?.name || ''
  folderDialogVisible.value = true
}

const submitFolder = async () => {
  if (!folderFormRef.value) return
  const valid = await folderFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingFolder.value) {
      await renameFolder(folderForm.id, folderForm.name)
      ElMessage.success('文件夹重命名成功')
    } else {
      await createFolder(folderForm.name)
      ElMessage.success('文件夹创建成功')
    }
    folderDialogVisible.value = false
    await fetchFolders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.error || '操作失败，请重试')
  }
}

const handleDeleteFolder = async (folder) => {
  try {
    await ElMessageBox.confirm(`确定删除文件夹「${folder.name}」吗？文件夹内的收藏将移至「未分类」。`, '提示', { type: 'warning' })
    await deleteFolder(folder.id)
    ElMessage.success('文件夹已删除')
    if (selectedFolderId.value === folder.id) {
      selectedFolderId.value = null
    }
    // 同时刷新文件夹和收藏列表
    await Promise.all([fetchFolders(), fetchFavorites()])
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 移动收藏到文件夹
const openMoveDialog = (item) => {
  console.log('[Favorites] openMoveDialog item:', item)
  movingItemId.value = item.id
  moveDialogVisible.value = true
}

const confirmMove = async (folderId) => {
  console.log('[Favorites] confirmMove favoriteId:', movingItemId.value, 'folderId:', folderId)
  try {
    const result = await moveToFolder(movingItemId.value, folderId)
    console.log('[Favorites] moveToFolder result:', result)
    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    fetchFavorites()
    fetchFolders()
  } catch (error) {
    console.error('[Favorites] moveToFolder error:', error)
    ElMessage.error(error.response?.data?.error || '移动失败')
  }
}

const removeJobFavorite = async (item) => {
  try {
    await ElMessageBox.confirm('确定取消收藏该职位？', '提示', { type: 'warning' })
    await request.delete('/favorite', { params: { targetType: 1, targetId: item.target_id || item.job_id } })
    ElMessage.success('已取消收藏')
    // 同时刷新文件夹和收藏列表
    await Promise.all([fetchFavorites(), fetchFolders()])
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

const removeResumeFavorite = async (item) => {
  try {
    await ElMessageBox.confirm('确定取消收藏该简历？', '提示', { type: 'warning' })
    await request.delete('/favorite', { params: { targetType: 2, targetId: item.target_id || item.resume_id } })
    ElMessage.success('已取消收藏')
    // 同时刷新文件夹和收藏列表
    await Promise.all([fetchFavorites(), fetchFolders()])
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

// 查看简历详情
const viewResumeDetail = (item) => {
  try {
    // 跳转到简历详情页面，使用新的企业用户专用路由
    if (item?.target_id) {
      router.push(`/r/resume/${item.target_id}`)
    } else {
      ElMessage.warning('简历ID不存在')
    }
  } catch (error) {
    console.error('跳转失败:', error)
    ElMessage.error('无法跳转到简历详情')
  }
}

const formatSalary = (min, max) => {
  try {
    if (!min && !max) return '面议'
    const fmt = (v) => {
      if (!v || isNaN(v)) return '0'
      return (v / 1000).toFixed(0) + 'K'
    }
    if (min && max) return `${fmt(min)} - ${fmt(max)}`
    if (min) return `${fmt(min)}起`
    return `最高${fmt(max)}`
  } catch {
    return '面议'
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

const parseSkills = (skills) => {
  try {
    if (!skills) return []
    if (Array.isArray(skills)) return skills
    if (typeof skills === 'string') {
      const parsed = JSON.parse(skills)
      return Array.isArray(parsed) ? parsed : []
    }
    return []
  } catch (error) {
    console.warn('解析技能失败:', error)
    return []
  }
}

onMounted(() => {
  try {
    fetchFolders()
    fetchFavorites()
  } catch (error) {
    console.error('初始化失败:', error)
  }
})

// 组件被 keep-alive 激活时重新获取数据
onActivated(() => {
  fetchFolders()
  fetchFavorites()
})

// 监听路由变化，进入收藏页面时刷新数据
watch(() => route.path, (newPath) => {
  if (newPath === '/favorites') {
    fetchFolders()
    fetchFavorites()
  }
})

// 监听用户状态变化，如果用户登出则清空数据
watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (!loggedIn) {
    favoriteJobs.value = []
    favoriteResumes.value = []
    allFavoriteJobs.value = []
    allFavoriteResumes.value = []
    folders.value = []
    selectedFolderId.value = null
  }
})

// 监听用户类型变化，重置activeTab
watch(() => userStore.user?.userType, (userType) => {
  if (userType === 'EMPLOYER') {
    activeTab.value = 'resumes'
  } else if (userType === 'EMPLOYEE') {
    activeTab.value = 'jobs'
  }
})

// 组件销毁前关闭所有 dialog，避免 DOM 残留
onBeforeUnmount(() => {
  folderDialogVisible.value = false
  moveDialogVisible.value = false
  // 取消正在进行的请求
  if (currentFetchController) {
    currentFetchController.abort()
  }
})
</script>

<style scoped>
/* ── 根容器（确保 transition 单根节点） ──── */
.favorites-root {
  width: 100%;
}

/* ── 页面容器 ─────────────────────────────── */
.favorites-page {
  padding: var(--space-6) var(--space-5);
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

/* ── 页面标题区 ────────────────────────────── */
.page-header {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.header-main h2 {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  letter-spacing: -0.02em;
}

.header-desc {
  margin: var(--space-1) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* ── 布局 ──────────────────────────────────── */
.favorites-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: var(--space-6);
  align-items: start;
}

/* ── 文件夹侧边栏 ──────────────────────────── */
.folder-sidebar {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-4) var(--space-3);
  position: sticky;
  top: calc(56px + var(--space-4));
  box-shadow: var(--shadow-xs);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
  padding: 0 var(--space-2);
}

.sidebar-header h4 {
  margin: 0;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--gray-400);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.folder-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: var(--text-sm);
  color: var(--gray-700);
  position: relative;
}

.folder-item:hover {
  background: var(--gray-50);
  color: var(--gray-900);
}

.folder-item.active {
  background: linear-gradient(135deg, var(--gray-900) 0%, var(--gray-800) 100%);
  color: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.folder-item .el-icon {
  font-size: var(--text-base);
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out);
}

.folder-item:hover .el-icon {
  transform: scale(1.05);
}

.folder-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-count {
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  color: var(--gray-400);
  background: var(--gray-100);
  padding: 1px 7px;
  border-radius: var(--radius-full);
  min-width: 20px;
  text-align: center;
  transition: all var(--duration-fast) var(--ease-out);
}

.folder-item.active .folder-count {
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-surface);
}

.folder-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.folder-item:hover .folder-actions {
  opacity: 1;
}

.action-icon {
  font-size: var(--text-sm);
  padding: 3px;
  border-radius: var(--radius-sm);
  color: var(--gray-500);
  transition: all var(--duration-fast) var(--ease-out);
}

.action-icon:hover {
  background: var(--gray-200);
  color: var(--gray-700);
  transform: scale(1.1);
}

.action-icon.delete:hover {
  background: var(--danger-50);
  color: var(--danger-600);
}

.folder-item.active .action-icon {
  color: rgba(255, 255, 255, 0.7);
}

.folder-item.active .action-icon:hover {
  background: rgba(255, 255, 255, 0.15);
  color: var(--color-surface);
}

/* ── 主内容区 ──────────────────────────────── */
.favorites-main {
  min-width: 0;
}

/* ── 类型切换 ─────────────────────────────── */
.type-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  padding: var(--space-1);
  background: var(--gray-50);
  border-radius: var(--radius-lg);
  width: fit-content;
}

.type-tab {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-5);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-500);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
}

.type-tab:hover {
  color: var(--gray-800);
  background: rgba(255, 255, 255, 0.8);
}

.type-tab.active {
  background: var(--color-surface);
  color: var(--gray-900);
  box-shadow: var(--shadow-sm);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--gray-200);
  color: var(--gray-600);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  transition: all var(--duration-fast) var(--ease-out);
}

.type-tab.active .tab-badge {
  background: var(--gray-900);
  color: var(--color-surface);
}

/* ── 卡片列表 ─────────────────────────────── */
.card-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.fav-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6);
  transition: all var(--duration-normal) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.fav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, var(--gray-900) 0%, var(--gray-600) 100%);
  opacity: 0;
  transition: opacity var(--duration-normal) var(--ease-out);
}

.fav-card:hover {
  border-color: var(--gray-200);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.fav-card:hover::before {
  opacity: 1;
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-5);
}

.card-main {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-lg);
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
  padding: 2px 0;
}

.meta-item .el-icon {
  font-size: var(--text-sm);
  color: var(--gray-400);
}

.skills {
  display: flex;
  gap: var(--space-1);
  flex-wrap: wrap;
  margin-bottom: var(--space-2);
}

.skills .el-tag {
  border-radius: var(--radius-sm);
  font-weight: var(--weight-medium);
}

.collect-time {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.collect-time .el-icon {
  font-size: var(--text-xs);
}

.card-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  opacity: 0.85;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.fav-card:hover .card-actions {
  opacity: 1;
}

/* ── 移动对话框 ────────────────────────────── */
.move-folder-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-2) 0;
}

.move-folder-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.move-folder-item:hover {
  background: var(--gray-50);
  color: var(--gray-900);
  transform: translateX(2px);
}

.move-folder-item .el-icon {
  font-size: var(--text-lg);
  color: var(--gray-400);
  transition: color var(--duration-fast) var(--ease-out);
}

.move-folder-item:hover .el-icon {
  color: var(--gray-600);
}

/* ── 空状态优化 ────────────────────────────── */
:deep(.el-empty) {
  padding: var(--space-8) 0;
}

:deep(.el-empty__description) {
  color: var(--color-text-muted);
}

/* ── 响应式 ────────────────────────────────── */
@media (max-width: 768px) {
  .favorites-layout {
    grid-template-columns: 1fr;
  }

  .folder-sidebar {
    position: static;
    margin-bottom: var(--space-4);
  }

  .folder-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  .folder-item {
    flex: 0 0 auto;
  }

  .folder-actions {
    opacity: 1;
  }

  .card-body {
    flex-direction: column;
    gap: var(--space-3);
  }

  .card-actions {
    flex-direction: row;
    width: 100%;
    opacity: 1;
  }

  .card-actions .el-button {
    flex: 1;
  }

  .type-tabs {
    width: 100%;
  }

  .type-tab {
    flex: 1;
    justify-content: center;
  }
}
</style>
