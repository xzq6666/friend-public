<template>
  <div class="favorites-page">
    <div class="page-header">
      <h2>我的收藏</h2>
      <p class="header-desc">管理您收藏的职位和简历</p>
    </div>

    <div v-loading="loading">
      <!-- 类型切换 -->
      <div class="type-tabs">
        <button
          v-if="isEmployee || isAdmin"
          class="type-tab"
          :class="{ active: activeTab === 'jobs' }"
          @click="activeTab = 'jobs'"
        >
          <el-icon><Briefcase /></el-icon>
          收藏职位 ({{ favoriteJobs.length }})
        </button>
        <button
          v-if="isEmployer || isAdmin"
          class="type-tab"
          :class="{ active: activeTab === 'resumes' }"
          @click="activeTab = 'resumes'"
        >
          <el-icon><Document /></el-icon>
          收藏简历 ({{ favoriteResumes.length }})
        </button>
      </div>

      <!-- 收藏职位 -->
      <div v-if="activeTab === 'jobs'">
        <el-empty v-if="favoriteJobs.length === 0" description="暂无收藏职位">
          <el-button type="primary" @click="$router.push('/browse-jobs')">去浏览职位</el-button>
        </el-empty>

        <div v-else class="card-list">
          <div v-for="item in favoriteJobs" :key="item.id" class="fav-card">
            <div class="card-body">
              <div class="card-main">
                <h3 class="title">{{ item.job_title || '未知职位' }}</h3>
                <div class="meta-row">
                  <span class="meta-item">
                    <el-icon><Location /></el-icon>{{ item.job_location || '未知地点' }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Money /></el-icon>{{ formatSalary(item.salary_min, item.salary_max) }}
                  </span>
                  <span class="meta-item">
                    <el-icon><OfficeBuilding /></el-icon>{{ item.employer_name || '未知企业' }}
                  </span>
                </div>
              </div>
              <div class="card-actions">
                <el-button type="danger" plain size="small" @click="removeJobFavorite(item)">
                  <el-icon><Delete /></el-icon> 取消收藏
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 收藏简历 -->
      <div v-if="activeTab === 'resumes'">
        <el-empty v-if="favoriteResumes.length === 0" description="暂无收藏简历" />

        <div v-else class="card-list">
          <div v-for="item in favoriteResumes" :key="item.id" class="fav-card">
            <div class="card-body">
              <div class="card-main">
                <h3 class="title">{{ item.resume_name || '未知简历' }}</h3>
                <div class="meta-row">
                  <span class="meta-item">
                    <el-icon><User /></el-icon>求职者：{{ item.applicant_name || '未知' }}
                  </span>
                  <span class="meta-item">
                    <el-icon><School /></el-icon>{{ item.resume_education || '未知' }}
                  </span>
                </div>
              </div>
              <div class="card-actions">
                <el-button type="primary" plain size="small" @click="viewResumeDetail(item)">
                  <el-icon><View /></el-icon> 查看详情
                </el-button>
                <el-button type="danger" plain size="small" @click="removeResumeFavorite(item)">
                  <el-icon><Delete /></el-icon> 取消收藏
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Favorites'
}
</script>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Money, User, Document, Briefcase, OfficeBuilding, School, Delete, View } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()
const isEmployee = computed(() => userStore.user?.userType === 'EMPLOYEE')
const isEmployer = computed(() => userStore.user?.userType === 'EMPLOYER')
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

const activeTab = ref('jobs')
const loading = ref(false)
const favoriteJobs = ref([])
const favoriteResumes = ref([])

// 获取收藏职位
const fetchFavoriteJobs = async () => {
  try {
    const res = await request.get('/favorite/jobs')
    favoriteJobs.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('获取收藏职位失败:', error)
    favoriteJobs.value = []
  }
}

// 获取收藏简历
const fetchFavoriteResumes = async () => {
  try {
    const res = await request.get('/favorite/resumes')
    favoriteResumes.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('获取收藏简历失败:', error)
    favoriteResumes.value = []
  }
}

// 加载所有收藏数据
const fetchFavorites = async () => {
  loading.value = true
  try {
    await Promise.all([fetchFavoriteJobs(), fetchFavoriteResumes()])
  } catch (error) {
    console.error('加载收藏数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 取消收藏职位
const removeJobFavorite = async (item) => {
  try {
    await ElMessageBox.confirm('确定取消收藏该职位？', '提示', { type: 'warning' })
    await request.delete('/favorite', { 
      params: { targetType: 1, targetId: item.target_id || item.job_id } 
    })
    ElMessage.success('已取消收藏')
    await fetchFavoriteJobs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

// 取消收藏简历
const removeResumeFavorite = async (item) => {
  try {
    await ElMessageBox.confirm('确定取消收藏该简历？', '提示', { type: 'warning' })
    await request.delete('/favorite', { 
      params: { targetType: 2, targetId: item.target_id || item.resume_id } 
    })
    ElMessage.success('已取消收藏')
    await fetchFavoriteResumes()
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

// 格式化薪资
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

onMounted(() => {
  console.log('[FavoritesSimple] 组件已挂载')
  fetchFavorites()
})

// keep-alive 激活时重新获取数据
onActivated(() => {
  console.log('[FavoritesSimple] 组件被激活，刷新数据')
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page {
  padding: var(--space-6);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
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

/* 类型切换 */
.type-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.type-tab {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-600);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.type-tab:hover {
  border-color: var(--gray-400);
  color: var(--gray-800);
}

.type-tab.active {
  background: var(--gray-900);
  border-color: var(--gray-900);
  color: var(--color-surface);
}

/* 卡片列表 */
.card-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.fav-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  transition: all var(--duration-fast);
}

.fav-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
}

.card-main {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--text-sm);
  color: var(--color-text-sub);
}

.meta-item .el-icon {
  font-size: 14px;
  color: var(--gray-400);
}

.card-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}
</style>
