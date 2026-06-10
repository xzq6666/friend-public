<template>
<div class="company-public-page" v-loading="loading">
  <!-- 返回按钮 -->
  <div class="back-bar">
    <el-button text @click="goBack">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>
  </div>

  <el-empty v-if="!loading && !company" description="未找到企业信息" />

  <template v-if="company">
    <!-- 头部横幅 -->
    <div class="profile-banner">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <div class="company-logo">
          <img v-if="company.logoUrl" :src="company.logoUrl" alt="logo" />
          <span v-else class="logo-letter">{{ (company.companyName || '企').charAt(0) }}</span>
        </div>
        <div class="header-info">
          <div class="name-row">
            <h1>{{ company.companyName || '未知企业' }}</h1>
            <el-tag v-if="company.verified === 2" type="success" size="small" effect="dark" round>
              <el-icon><CircleCheck /></el-icon> 已认证
            </el-tag>
            <el-tag v-else-if="company.verified === 1" type="warning" size="small" effect="dark" round>审核中</el-tag>
            <el-button
              v-if="company.userId !== currentUserId"
              type="danger"
              size="small"
              plain
              round
              @click="openReportDialog"
              style="margin-left: auto; background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: #fff;"
            >
              <el-icon><WarnTriangleFilled /></el-icon> 举报
            </el-button>
          </div>
          <div class="meta-row">
            <span v-if="company.industry" class="meta-item">
              <el-icon><OfficeBuilding /></el-icon>{{ company.industry }}
            </span>
            <span v-if="company.companyScale" class="meta-item">
              <el-icon><User /></el-icon>{{ company.companyScale }}
            </span>
            <span v-if="company.address" class="meta-item">
              <el-icon><Location /></el-icon>{{ company.address }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="profile-body">
      <!-- 公司简介 -->
      <div class="info-card" v-if="company.description">
        <div class="card-header">
          <div class="card-icon" style="color: #409eff;">&#128209;</div>
          <div class="card-title">公司简介</div>
        </div>
        <div class="card-content description-text">{{ company.description }}</div>
      </div>

      <!-- 基本信息 + 联系方式并排 -->
      <div class="info-row">
        <div class="info-card">
          <div class="card-header">
            <div class="card-icon" style="color: #67c23a;">&#127970;</div>
            <div class="card-title">基本信息</div>
          </div>
          <div class="info-list">
            <div class="info-list-item">
              <span class="info-label">所属行业</span>
              <span class="info-value">{{ company.industry || '未填写' }}</span>
            </div>
            <div class="info-list-item">
              <span class="info-label">公司规模</span>
              <span class="info-value">{{ company.companyScale || '未填写' }}</span>
            </div>
            <div class="info-list-item">
              <span class="info-label">公司地址</span>
              <span class="info-value">{{ company.address || '未填写' }}</span>
            </div>
            <div class="info-list-item">
              <span class="info-label">公司网站</span>
              <span class="info-value">
                <a v-if="company.website" :href="company.website" target="_blank" class="website-link">{{ company.website }}</a>
                <span v-else>未填写</span>
              </span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="card-header">
            <div class="card-icon" style="color: #e6a23c;">&#128222;</div>
            <div class="card-title">联系方式</div>
          </div>
          <div class="info-list">
            <div class="info-list-item">
              <span class="info-label">联系人</span>
              <span class="info-value">{{ company.contactPerson || '未填写' }}</span>
            </div>
            <div class="info-list-item">
              <span class="info-label">联系电话</span>
              <span class="info-value">{{ company.contactPhone || '未填写' }}</span>
            </div>
            <div class="info-list-item">
              <span class="info-label">联系邮箱</span>
              <span class="info-value">{{ company.contactEmail || '未填写' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 企业发布的岗位 -->
      <div class="info-card">
        <div class="card-header">
          <div class="card-icon" style="color: #f56c6c;">&#128188;</div>
          <div class="card-title">在招岗位</div>
          <el-tag v-if="companyJobs.length > 0" size="small" type="primary" round effect="plain">{{ companyJobs.length }} 个职位</el-tag>
        </div>
        <div v-loading="loadingJobs">
          <el-empty v-if="!loadingJobs && companyJobs.length === 0" description="暂无在招岗位" :image-size="60" />
          <div v-else class="company-jobs-list">
            <div v-for="job in companyJobs" :key="job.id" class="company-job-item" @click="openJobDetail(job)">
              <div class="job-main">
                <div class="job-title-row">
                  <span class="cj-title">{{ job.title }}</span>
                  <span class="cj-salary">{{ formatSalary(job.salaryMin, job.salaryMax) }}</span>
                </div>
                <div class="job-meta-row">
                  <span v-if="job.location" class="job-meta-tag"><el-icon><Location /></el-icon>{{ job.location }}</span>
                  <span v-if="job.experienceRequired" class="job-meta-tag"><el-icon><Timer /></el-icon>{{ job.experienceRequired }}</span>
                  <span v-if="job.educationRequired" class="job-meta-tag"><el-icon><Reading /></el-icon>{{ job.educationRequired }}</span>
                  <el-tag v-if="job.workType" size="small" type="primary" effect="plain" round>{{ workTypeLabel(job.workType) }}</el-tag>
                </div>
                <p class="cj-desc">{{ job.description || '暂无描述' }}</p>
              </div>
              <span class="cj-time">{{ formatDate(job.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>

  <!-- 举报对话框 -->
  <el-dialog v-model="reportDialogVisible" title="举报企业" width="500px">
    <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="100px">
      <el-form-item label="被举报企业">
        <el-input :value="company?.companyName" disabled />
      </el-form-item>
      <el-form-item label="举报原因" prop="reason">
        <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
          <el-option label="虚假信息" value="虚假信息" />
          <el-option label="欺诈行为" value="欺诈行为" />
          <el-option label="违规内容" value="违规内容" />
          <el-option label="虚假招聘" value="虚假招聘" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="详细描述" prop="description">
        <el-input
          v-model="reportForm.description"
          type="textarea"
          :rows="4"
          placeholder="请详细描述举报原因（选填）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reportDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="submitReport" :loading="submittingReport">
        <el-icon><WarnTriangleFilled /></el-icon> 提交举报
      </el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { CircleCheck, OfficeBuilding, User, Location, Timer, Reading, ArrowLeft, WarnTriangleFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)
const loading = ref(false)
const company = ref(null)

// 企业岗位相关
const companyJobs = ref([])
const loadingJobs = ref(false)

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/browse-jobs')
  }
}

const fetchCompany = async () => {
  const userId = route.params.id
  if (!userId) return
  loading.value = true
  try {
    const res = await request.get(`/company/${userId}`)
    company.value = res
    // 获取到企业信息后，加载该企业的岗位
    fetchCompanyJobs(userId)
  } catch {
    company.value = null
  } finally {
    loading.value = false
  }
}

const fetchCompanyJobs = async (employerId) => {
  loadingJobs.value = true
  try {
    const res = await request.get('/job', {
      params: { employerId, page: 1, size: 50 },
      skipGlobalLoading: true
    })
    companyJobs.value = (res.records || []).filter(j => j.status === 1)
  } catch {
    companyJobs.value = []
  } finally {
    loadingJobs.value = false
  }
}

const openJobDetail = (job) => {
  router.push('/browse-jobs')
}

const formatSalary = (min, max) => {
  if (!min && !max) return '面议'
  const f = v => (v / 1000).toFixed(0) + 'K'
  if (min && max) return `${f(min)} - ${f(max)}`
  return min ? `${f(min)}起` : `最高${f(max)}`
}

const formatDate = d => d ? new Date(d).toLocaleDateString('zh-CN') : ''

const workTypeLabel = (type) => {
  const map = { remote: '远程', onsite: '现场', hybrid: '混合' }
  return map[type] || type
}

// 举报相关
const reportDialogVisible = ref(false)
const reportFormRef = ref()
const submittingReport = ref(false)
const reportForm = reactive({
  reason: '',
  description: ''
})
const reportRules = {
  reason: [{ required: true, message: '请选择举报原因', trigger: 'change' }]
}

const openReportDialog = () => {
  reportForm.reason = ''
  reportForm.description = ''
  reportDialogVisible.value = true
}

const submitReport = async () => {
  const valid = await reportFormRef.value.validate().catch(() => false)
  if (!valid) return
  submittingReport.value = true
  try {
    await request.post('/report', {
      reportedType: 3,
      reportedId: company.value?.userId,
      reason: reportForm.reason,
      description: reportForm.description || null
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    reportDialogVisible.value = false
  } catch (error) {
    ElMessage.error('提交失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    submittingReport.value = false
  }
}

onMounted(() => { fetchCompany() })
</script>

<style scoped>
.company-public-page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-5);
}

/* ── 返回按钮 ──────────────────────────────── */
.back-bar {
  margin-bottom: var(--space-3);
}
.back-bar .el-button {
  font-size: var(--text-sm);
  color: var(--gray-600);
}
.back-bar .el-button:hover {
  color: var(--color-primary);
}

/* ── 头部横幅 ──────────────────────────────── */
.profile-banner {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: var(--space-5);
}

.banner-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 50%, #a0cfff 100%);
  z-index: 0;
}

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-8) var(--space-6);
}

.company-logo {
  width: 88px;
  height: 88px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255,255,255,0.4);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}
.company-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.logo-letter {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.header-info { flex: 1; }

.name-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}
.name-row h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-sm);
  color: rgba(255,255,255,0.85);
}
.meta-item .el-icon {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
}

/* ── 内容区 ────────────────────────────────── */
.profile-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
}

.info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: box-shadow 0.2s;
}
.info-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--gray-100);
}

.card-icon {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gray-50);
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.card-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--gray-900);
}

.description-text {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: 1.8;
  white-space: pre-wrap;
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-list-item {
  display: flex;
  align-items: center;
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--gray-100);
}
.info-list-item:last-child { border-bottom: none; }

.info-label {
  width: 80px;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.info-value {
  font-size: var(--text-sm);
  color: var(--gray-800);
  font-weight: 500;
}

.website-link {
  color: var(--primary-600);
  text-decoration: none;
}
.website-link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .banner-content { flex-direction: column; text-align: center; }
  .name-row { justify-content: center; }
  .meta-row { justify-content: center; }
  .info-row { grid-template-columns: 1fr; }
}

/* ── 企业岗位列表 ──────────────────────────── */
.company-jobs-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.company-job-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--space-4);
  border: 1px solid var(--gray-100);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}
.company-job-item:hover {
  border-color: var(--primary-300);
  background: var(--primary-50, var(--gray-50));
  box-shadow: 0 2px 8px rgba(64,158,255,0.1);
}

.job-main { flex: 1; min-width: 0; }

.job-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}
.cj-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--gray-900);
}
.cj-salary {
  font-size: var(--text-sm);
  font-weight: 700;
  color: #f56c6c;
  white-space: nowrap;
}

.job-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.job-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--gray-500);
  background: var(--gray-50);
  padding: 2px 8px;
  border-radius: 10px;
}
.job-meta-tag .el-icon { font-size: 12px; }

.cj-desc {
  font-size: var(--text-xs);
  color: var(--gray-400);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cj-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
  white-space: nowrap;
  margin-left: var(--space-4);
  padding-top: 2px;
}
</style>
