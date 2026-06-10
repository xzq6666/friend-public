<template>
  <div class="jobs-page">
    <!-- 搜索与筛选 -->
    <div class="search-card">
      <div class="search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索职位名称、公司、技能关键词..."
          clearable
          size="large"
          @keyup.enter="fetchJobs"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" @click="fetchJobs">搜索</el-button>
        <el-button size="large" @click="resetFilters">
          <el-icon><RefreshLeft /></el-icon> 重置
        </el-button>
      </div>

      <div class="filter-row">
        <el-cascader
          v-model="filters.location"
          :options="regionOptions"
          placeholder="工作地点"
          clearable
          filterable
          @change="fetchJobs"
          class="filter-item"
        />
        <el-select v-model="filters.experience" placeholder="经验要求" clearable @change="fetchJobs" class="filter-item">
          <el-option label="不限" value="" />
          <el-option label="应届生" value="应届生" />
          <el-option label="1-3年" value="1-3年" />
          <el-option label="3-5年" value="3-5年" />
          <el-option label="5-10年" value="5-10年" />
        </el-select>
        <el-select v-model="filters.education" placeholder="学历要求" clearable @change="fetchJobs" class="filter-item">
          <el-option label="不限" value="" />
          <el-option label="大专" value="大专" />
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
        <el-select v-model="filters.workType" placeholder="工作类型" clearable @change="fetchJobs" class="filter-item">
          <el-option label="不限" value="" />
          <el-option label="远程" value="remote" />
          <el-option label="现场" value="onsite" />
          <el-option label="混合" value="hybrid" />
        </el-select>
      </div>

      <div class="slider-row">
        <div class="slider-group">
          <span class="slider-label">薪资 {{ formatSalaryRange(filters.salaryRange) }}</span>
          <el-slider
            v-model="filters.salaryRange"
            range
            :min="0"
            :max="100"
            :step="5"
            :format-tooltip="formatSalaryTooltip"
          />
        </div>
        <div class="slider-group">
          <span class="slider-label">经验 {{ formatExpRange(filters.expRange) }}</span>
          <el-slider
            v-model="filters.expRange"
            range
            :min="0"
            :max="20"
            :step="1"
            :format-tooltip="formatExpTooltip"
          />
        </div>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && jobs.length === 0" description="暂无职位信息" />
      <div v-else class="jobs-grid">
        <div v-for="job in jobs" :key="job.id" class="job-card">
          <div class="card-header">
            <h3 class="job-title" @click="openDetailDialog(job)">{{ job.title }}</h3>
            <div class="job-salary">{{ formatSalary(job.salaryMin, job.salaryMax) }}</div>
          </div>
          <div class="company-info" v-if="job.companyInfo">
            <el-tag v-if="job.companyInfo.verified === 2" type="success" size="small" effect="light">
              <el-icon><CircleCheck /></el-icon> 已认证
            </el-tag>
            <span class="company-name clickable" @click.stop="$router.push(`/company/${job.employerId}`)">{{ job.companyInfo.companyName }}</span>
            <span class="company-scale" v-if="job.companyInfo.companyScale">| {{ job.companyInfo.companyScale }}</span>
          </div>
          <div class="job-meta">
            <span class="meta-item"><el-icon><Location /></el-icon>{{ job.location || '未设置' }}</span>
            <span class="meta-item" v-if="job.experienceRequired"><el-icon><Timer /></el-icon>{{ job.experienceRequired }}</span>
            <span class="meta-item" v-if="job.educationRequired"><el-icon><Reading /></el-icon>{{ job.educationRequired }}</span>
          </div>
          <p class="job-desc">{{ job.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span class="post-time">{{ formatDate(job.createTime) }}</span>
            <div class="footer-actions">
              <el-button :type="favoritedJobs.has(job.id) ? 'warning' : 'default'" size="small" @click="toggleFavorite(job)">
                <el-icon><Star /></el-icon>{{ favoritedJobs.has(job.id) ? '已收藏' : '收藏' }}
              </el-button>
              <el-button v-if="appliedJobs.has(job.id)" type="success" size="small" disabled>已投递</el-button>
              <el-button v-else type="primary" size="small" @click="openApplyDialog(job)">投递简历</el-button>
              <el-button type="success" size="small" plain @click="startChat(job)">
                <el-icon><ChatDotRound /></el-icon> 立即沟通
              </el-button>
              <el-button type="info" size="small" plain @click="$router.push(`/forum/${job.id}`)">
                <el-icon><ChatDotSquare /></el-icon> 论坛
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchJobs" @current-change="fetchJobs" />
    </div>

    <el-dialog v-model="detailDialogVisible" title="职位详情" width="700px">
      <div class="detail-header">
        <h3>{{ jobDetail?.title }}</h3>
        <div class="detail-salary">{{ formatSalary(jobDetail?.salaryMin, jobDetail?.salaryMax) }}</div>
      </div>
      
      <!-- 企业信息 -->
      <div class="company-detail-info" v-if="jobDetail?.companyInfo">
        <div class="company-header">
          <el-tag v-if="jobDetail.companyInfo.verified === 2" type="success" size="small" effect="light">
            <el-icon><CircleCheck /></el-icon> 已认证
          </el-tag>
          <span class="company-name clickable" @click="$router.push(`/company/${jobDetail.employerId}`)">{{ jobDetail.companyInfo.companyName }}</span>
        </div>
        <div class="company-meta">
          <span v-if="jobDetail.companyInfo.industry"><el-icon><OfficeBuilding /></el-icon>{{ jobDetail.companyInfo.industry }}</span>
          <span v-if="jobDetail.companyInfo.companyScale"><el-icon><User /></el-icon>{{ jobDetail.companyInfo.companyScale }}</span>
          <span v-if="jobDetail.companyInfo.address"><el-icon><Location /></el-icon>{{ jobDetail.companyInfo.address }}</span>
        </div>
        <p class="company-description" v-if="jobDetail.companyInfo.description">{{ jobDetail.companyInfo.description }}</p>
      </div>
      
      <div class="detail-meta">
        <span class="meta-item"><el-icon><Location /></el-icon>{{ jobDetail?.location || '未设置' }}</span>
        <span class="meta-item" v-if="jobDetail?.experienceRequired"><el-icon><Timer /></el-icon>{{ jobDetail?.experienceRequired }}</span>
        <span class="meta-item" v-if="jobDetail?.educationRequired"><el-icon><Reading /></el-icon>{{ jobDetail?.educationRequired }}</span>
        <span class="meta-item" v-if="jobDetail?.createTime"><el-icon><Clock /></el-icon>发布于 {{ formatDate(jobDetail.createTime) }}</span>
      </div>
      <el-divider />
      <div class="detail-section"><h4>职位描述</h4><p>{{ jobDetail?.description || '暂无描述' }}</p></div>
      <el-divider />
      <div class="detail-section"><h4>任职要求</h4><p>{{ jobDetail?.requirements || '暂无要求' }}</p></div>
      <el-divider />
      <div class="detail-section"><h4>福利待遇</h4><p>{{ jobDetail?.benefits || '暂无说明' }}</p></div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="info" plain @click="detailDialogVisible = false; $router.push(`/forum/${jobDetail?.id}`)">
          <el-icon><ChatDotSquare /></el-icon> 进入论坛
        </el-button>
        <el-button type="danger" plain @click="showReportDialog(jobDetail)">
          <el-icon><WarnTriangleFilled /></el-icon> 举报
        </el-button>
        <el-button v-if="!appliedJobs.has(jobDetail?.id)" type="primary" @click="detailDialogVisible = false; openApplyDialog(jobDetail)">投递简历</el-button>
        <el-button v-else type="success" disabled>已投递</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyDialogVisible" title="投递简历" width="500px">
      <div class="apply-job-info">
        <h4>{{ selectedJob?.title }}</h4>
        <p>{{ selectedJob?.location }} | {{ formatSalary(selectedJob?.salaryMin, selectedJob?.salaryMax) }}</p>
      </div>
      <el-form label-width="80px">
        <el-form-item label="选择简历" required>
          <el-select v-model="selectedResumeId" placeholder="请选择要投递的简历" style="width: 100%" :loading="loadingResumes">
            <el-option
              v-for="resume in myResumes"
              :key="resume.id"
              :label="resume.resumeName || resume.name + '的简历'"
              :value="resume.id"
            >
              <span>{{ resume.resumeName || resume.name + '的简历' }}</span>
              <el-tag v-if="resume.isDefault === 1" type="success" size="small" style="margin-left: 8px">默认</el-tag>
            </el-option>
          </el-select>
          <div class="form-tip" v-if="myResumes.length === 0">
            暂无简历，请先<router-link to="/my-resume">创建简历</router-link>
          </div>
        </el-form-item>
        <el-form-item label="求职信">
          <el-input v-model="coverLetter" type="textarea" :rows="4" placeholder="请输入求职信（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply" :loading="applying" :disabled="!selectedResumeId">确认投递</el-button>
      </template>
    </el-dialog>

    <!-- 举报对话框 -->
    <el-dialog v-model="reportDialogVisible" title="举报职位" width="500px">
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="100px">
        <el-form-item label="职位名称">
          <el-input :value="reportTarget?.title" disabled />
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
            <el-option label="虚假信息" value="虚假信息" />
            <el-option label="欺诈行为" value="欺诈行为" />
            <el-option label="违规内容" value="违规内容" />
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

<script>
export default {
  name: 'BrowseJobs'
}
</script>

<script setup>
import { ref, reactive, onMounted, onActivated, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Location, Timer, Reading, Star, Clock, ChatDotRound, ChatDotSquare, CircleCheck, OfficeBuilding, User, WarnTriangleFilled, RefreshLeft } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const jobs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filters = reactive({
  location: [],
  experience: '',
  education: '',
  salaryRange: [0, 100],
  workType: '',
  expRange: [0, 20],
})
const applyDialogVisible = ref(false)
const selectedJob = ref(null)
const coverLetter = ref('')
const applying = ref(false)
const appliedJobs = ref(new Set())
const favoritedJobs = ref(new Set())
const detailDialogVisible = ref(false)
const jobDetail = ref(null)

// 请求缓存
const requestCache = new Map()
const CACHE_TTL = 30000 // 30秒缓存

const getCachedData = (key) => {
  const cached = requestCache.get(key)
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    console.log('[Jobs] 使用缓存:', key)
    return cached.data
  }
  return null
}

const setCachedData = (key, data) => {
  requestCache.set(key, { data, timestamp: Date.now() })
}

// 简历相关
const myResumes = ref([])
const selectedResumeId = ref(null)
const loadingResumes = ref(false)

// 举报相关
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const submittingReport = ref(false)
const reportFormRef = ref()
const reportForm = reactive({
  reason: '',
  description: ''
})
const reportRules = {
  reason: [
    { required: true, message: '请选择举报原因', trigger: 'change' }
  ]
}

// 用于防止竞态条件的请求序列号
let fetchJobsSeq = 0

const regionOptions = [
  { value: '北京', label: '北京', children: [{ value: '北京', label: '北京' }] },
  { value: '上海', label: '上海', children: [{ value: '上海', label: '上海' }] },
  { value: '广东', label: '广东', children: [{ value: '广州', label: '广州' }, { value: '深圳', label: '深圳' }, { value: '珠海', label: '珠海' }, { value: '佛山', label: '佛山' }, { value: '东莞', label: '东莞' }] },
  { value: '江苏', label: '江苏', children: [{ value: '南京', label: '南京' }, { value: '苏州', label: '苏州' }, { value: '无锡', label: '无锡' }, { value: '常州', label: '常州' }] },
  { value: '浙江', label: '浙江', children: [{ value: '杭州', label: '杭州' }, { value: '宁波', label: '宁波' }, { value: '温州', label: '温州' }] },
  { value: '四川', label: '四川', children: [{ value: '成都', label: '成都' }, { value: '绵阳', label: '绵阳' }] },
  { value: '湖北', label: '湖北', children: [{ value: '武汉', label: '武汉' }, { value: '宜昌', label: '宜昌' }] },
  { value: '山东', label: '山东', children: [{ value: '济南', label: '济南' }, { value: '青岛', label: '青岛' }] },
  { value: '河南', label: '河南', children: [{ value: '郑州', label: '郑州' }, { value: '洛阳', label: '洛阳' }] },
  { value: '湖南', label: '湖南', children: [{ value: '长沙', label: '长沙' }, { value: '株洲', label: '株洲' }] },
  { value: '安徽', label: '安徽', children: [{ value: '合肥', label: '合肥' }, { value: '芜湖', label: '芜湖' }] },
  { value: '福建', label: '福建', children: [{ value: '福州', label: '福州' }, { value: '厦门', label: '厦门' }] },
  { value: '陕西', label: '陕西', children: [{ value: '西安', label: '西安' }, { value: '咸阳', label: '咸阳' }] },
  { value: '辽宁', label: '辽宁', children: [{ value: '沈阳', label: '沈阳' }, { value: '大连', label: '大连' }] },
  { value: '河北', label: '河北', children: [{ value: '石家庄', label: '石家庄' }, { value: '保定', label: '保定' }] },
  { value: '重庆', label: '重庆', children: [{ value: '重庆', label: '重庆' }] },
  { value: '天津', label: '天津', children: [{ value: '天津', label: '天津' }] },
  { value: '云南', label: '云南', children: [{ value: '昆明', label: '昆明' }] },
  { value: '贵州', label: '贵州', children: [{ value: '贵阳', label: '贵阳' }] },
  { value: '广西', label: '广西', children: [{ value: '南宁', label: '南宁' }] },
  { value: '海南', label: '海南', children: [{ value: '海口', label: '海口' }, { value: '三亚', label: '三亚' }] },
  { value: '黑龙江', label: '黑龙江', children: [{ value: '哈尔滨', label: '哈尔滨' }] },
  { value: '吉林', label: '吉林', children: [{ value: '长春', label: '长春' }] },
  { value: '山西', label: '山西', children: [{ value: '太原', label: '太原' }] },
  { value: '内蒙古', label: '内蒙古', children: [{ value: '呼和浩特', label: '呼和浩特' }] },
  { value: '新疆', label: '新疆', children: [{ value: '乌鲁木齐', label: '乌鲁木齐' }] },
  { value: '西藏', label: '西藏', children: [{ value: '拉萨', label: '拉萨' }] },
  { value: '宁夏', label: '宁夏', children: [{ value: '银川', label: '银川' }] },
  { value: '甘肃', label: '甘肃', children: [{ value: '兰州', label: '兰州' }] },
  { value: '青海', label: '青海', children: [{ value: '西宁', label: '西宁' }] },
  { value: '江西', label: '江西', children: [{ value: '南昌', label: '南昌' }] }
]

const fetchJobs = async () => {
  const currentSeq = ++fetchJobsSeq
  loading.value = true

  // 生成缓存key
  const cacheKey = `jobs_${currentPage.value}_${pageSize.value}_${searchKeyword.value}_${JSON.stringify(filters)}`
  
  // 检查缓存
  const cachedData = getCachedData(cacheKey)
  if (cachedData && currentSeq === fetchJobsSeq) {
    jobs.value = cachedData.jobs
    total.value = cachedData.total
    loading.value = false
    return
  }

  try {
    const locationParam = Array.isArray(filters.location) && filters.location.length > 0
      ? filters.location[filters.location.length - 1] : ''
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      location: locationParam,
      experience: filters.experience,
      education: filters.education,
      salaryMin: filters.salaryRange[0] > 0 ? filters.salaryRange[0] * 1000 : undefined,
      salaryMax: filters.salaryRange[1] < 100 ? filters.salaryRange[1] * 1000 : undefined,
      workType: filters.workType || undefined,
      expMin: filters.expRange[0] > 0 ? filters.expRange[0] : undefined,
      expMax: filters.expRange[1] < 20 ? filters.expRange[1] : undefined,
    }
    const res = await request.get('/job', { params, skipGlobalLoading: true })

    // 只处理最新请求的结果，忽略过期响应（竞态条件防护）
    if (currentSeq !== fetchJobsSeq) return

    jobs.value = (res.records || []).filter(j => j.status === 1)
    total.value = res.total || 0

    // 保存到缓存
    setCachedData(cacheKey, { jobs: jobs.value, total: total.value })

    // 获取职位列表后自动检查收藏和投递状态
    if (jobs.value.length > 0) {
      checkAppliedStatus()
    }
  } catch (e) {
    // 只对最新请求显示错误
    if (currentSeq === fetchJobsSeq) {
      ElMessage.error('获取职位列表失败')
    }
  } finally {
    if (currentSeq === fetchJobsSeq) {
      loading.value = false
    }
  }
}

// 单独的状态检查方法，只在需要时手动调用
const checkAppliedStatus = async () => {
  // 重置状态，确保数据同步
  const newApplied = new Set()
  const newFavorited = new Set()

  // 使用 Promise.all 并行请求，减少请求次数
  const checkPromises = jobs.value.map(async (job) => {
    try {
      const [appRes, favRes] = await Promise.all([
        request.get(`/application/check/${job.id}`, { skipErrorNotification: true }),
        request.get('/favorite/check', { params: { targetType: 1, targetId: job.id }, skipErrorNotification: true })
      ])
      if (appRes?.hasApplied) newApplied.add(job.id)
      if (favRes?.favorited) newFavorited.add(job.id)
    } catch { /* ignore */ }
  })
  await Promise.all(checkPromises)

  // 更新状态
  appliedJobs.value = newApplied
  favoritedJobs.value = newFavorited
}

const resetFilters = () => {
  Object.assign(filters, {
    location: '',
    experience: '',
    education: '',
    salaryRange: [0, 100],
    workType: '',
    expRange: [0, 20],
  })
  searchKeyword.value = ''
  currentPage.value = 1
  fetchJobs()
}

const formatSalaryTooltip = (v) => `¥${v}k`
const formatExpTooltip = (v) => `${v}年`

const formatSalaryRange = ([min, max]) => {
  if (min === 0 && max === 100) return '不限'
  if (min === 0) return `¥${max}k以下`
  if (max === 100) return `¥${min}k以上`
  return `¥${min}k - ¥${max}k`
}

const formatExpRange = ([min, max]) => {
  if (min === 0 && max === 20) return '不限'
  if (min === 0) return `${max}年以下`
  if (max === 20) return `${min}年以上`
  return `${min} - ${max}年`
}

const openDetailDialog = async (job) => {
  try { jobDetail.value = await request.get(`/job/${job.id}`); detailDialogVisible.value = true }
  catch { ElMessage.error('获取职位详情失败') }
}

const openApplyDialog = async (job) => { 
  selectedJob.value = job 
  coverLetter.value = ''
  selectedResumeId.value = null
  
  // 加载用户简历列表
  await loadMyResumes()
  
  // 如果有默认简历，自动选择
  const defaultResume = myResumes.value.find(r => r.isDefault === 1)
  if (defaultResume) {
    selectedResumeId.value = defaultResume.id
  } else if (myResumes.value.length > 0) {
    selectedResumeId.value = myResumes.value[0].id
  }
  
  applyDialogVisible.value = true 
}

const loadMyResumes = async () => {
  loadingResumes.value = true
  try {
    const res = await request.get('/resume/my/list', {
      skipErrorNotification: true
    })
    myResumes.value = res || []
  } catch (e) {
    if (e.response?.status !== 401) {
      console.error('加载简历列表失败', e)
      myResumes.value = []
    }
  } finally {
    loadingResumes.value = false
  }
}

const toggleFavorite = async (job) => {
  try {
    if (favoritedJobs.value.has(job.id)) {
      await request.delete('/favorite', { params: { targetType: 1, targetId: job.id } })
      favoritedJobs.value.delete(job.id); ElMessage.success('已取消收藏')
    } else {
      await request.post('/favorite', { targetType: 1, targetId: job.id })
      favoritedJobs.value.add(job.id); ElMessage.success('收藏成功')
    }
    // 清除请求缓存，确保下次加载时获取最新数据
    requestCache.clear()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') }
}

const submitApply = async () => {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择要投递的简历')
    return
  }
  
  applying.value = true
  try {
    await request.post('/application', { 
      jobId: selectedJob.value.id, 
      resumeId: selectedResumeId.value,
      coverLetter: coverLetter.value || null 
    })
    ElMessage.success('投递成功！')
    appliedJobs.value.add(selectedJob.value.id)
    applyDialogVisible.value = false
  } catch (e) { 
    ElMessage.error(e.response?.data?.error || '投递失败') 
  } finally { 
    applying.value = false 
  }
}

const startChat = async (job) => {
  try {
    // 检查是否已有与该企业的聊天记录
    const myChats = await request.get('/interview/my-chats', {
      skipGlobalLoading: true,
      skipErrorNotification: true
    })
    const existing = myChats?.find(chat => chat.employer_id === job.employerId)
    
    if (existing?.id) {
      router.push(`/interview-chat/${existing.id}`)
    } else {
      // 创建新的沟通记录
      const res = await request.post('/interview/direct-chat', {
        jobId: job.id,
        title: job.title,
        employerId: job.employerId
      })
      ElMessage.success('沟通通道已建立')
      router.push(`/interview-chat/${res.id}`)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '发起沟通失败')
  }
}

const formatSalary = (min, max) => {
  if (!min && !max) return '面议'
  const f = v => (v / 1000).toFixed(0) + 'K'
  if (min && max) return `${f(min)} - ${f(max)}`
  return min ? `${f(min)}起` : `最高${f(max)}`
}

const formatDate = d => d ? new Date(d).toLocaleDateString('zh-CN') : ''

// 显示举报对话框
const showReportDialog = (job) => {
  reportTarget.value = job
  reportForm.reason = ''
  reportForm.description = ''
  reportDialogVisible.value = true
}

// 提交举报
const submitReport = async () => {
  const valid = await reportFormRef.value.validate().catch(() => false)
  if (!valid) return

  submittingReport.value = true
  try {
    await request.post('/report', {
      reportedType: 4, // 4-职位
      reportedId: reportTarget.value.id,
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

onMounted(async () => {
  console.log('[Jobs] 组件已挂载')
  await fetchJobs()
  // 管理员从举报列表跳转：自动打开指定职位详情
  const highlightJobId = route.query.highlightJob
  if (highlightJobId) {
    const target = jobs.value.find(j => j.id === Number(highlightJobId))
    if (target) {
      openDetailDialog(target)
    }
  }
})

// keep-alive 激活时调用，刷新收藏状态
onActivated(() => {
  console.log('[Jobs] 组件被激活')
  // 刷新收藏状态，确保从其他页面（如收藏页面）取消收藏后状态同步
  if (jobs.value.length > 0) {
    checkAppliedStatus()
  }
})
</script>

<style scoped>
.jobs-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: var(--space-6) var(--space-7) var(--space-8);
  max-width: var(--container-xl);
  margin: 0 auto;
  animation: fadeIn 0.4s var(--ease-out);
}

/* ── 搜索与筛选卡片 ──────────────────────── */
.search-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-5);
}

.search-row {
  display: flex;
  gap: var(--space-2);
}

.search-input {
  flex: 1;
}

.filter-row {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.filter-item {
  flex: 1;
}

.slider-row {
  display: flex;
  gap: var(--space-6);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.slider-group {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.slider-label {
  font-size: var(--text-sm);
  color: var(--gray-500);
  white-space: nowrap;
  min-width: 100px;
}

/* ── 职位网格 ──────────────────────────────── */
.jobs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-5);
}

/* ── 职位卡片 ──────────────────────────────── */
.job-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  transition: all var(--duration-normal) var(--ease-out);
}

.job-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-3);
  gap: var(--space-3);
}

.job-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  cursor: pointer;
  margin: 0;
  flex: 1;
  transition: color var(--duration-fast) var(--ease-out);
}

.job-title:hover {
  color: var(--color-primary);
}

.job-salary {
  font-size: var(--text-md);
  font-weight: var(--weight-bold);
  color: var(--color-primary);
  white-space: nowrap;
}

.company-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-3);
  font-size: var(--text-sm);
}

.company-name {
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
}
.company-name.clickable {
  cursor: pointer;
  transition: color var(--duration-fast);
}
.company-name.clickable:hover {
  color: var(--primary-600);
}

.company-scale {
  color: var(--gray-400);
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.meta-item .el-icon {
  color: var(--gray-400);
}

.job-desc {
  font-size: var(--text-sm);
  color: var(--gray-500);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-4);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.post-time {
  font-size: var(--text-xs);
  color: var(--gray-400);
}

.footer-actions {
  display: flex;
  gap: var(--space-2);
}

.footer-actions .el-button {
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

/* ── 分页 ────────────────────────────────── */
.pagination {
  margin-top: var(--space-6);
  display: flex;
  justify-content: flex-end;
}

/* ── 详情对话框 ────────────────────────────── */
.detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-4); }
.detail-header h3 { margin: 0; font-size: var(--text-xl); font-weight: var(--weight-semibold); color: var(--gray-900); }
.detail-salary { font-size: var(--text-lg); font-weight: var(--weight-semibold); color: var(--color-primary); }

.company-detail-info {
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}
.company-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}
.company-header .company-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}
.company-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
  font-size: var(--text-sm);
  color: var(--gray-600);
}
.company-meta span {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}
.company-description {
  font-size: var(--text-sm);
  color: var(--gray-600);
  line-height: var(--leading-relaxed);
  margin: 0;
}
.detail-meta { display: flex; flex-wrap: wrap; gap: var(--space-4); margin-bottom: var(--space-4); }
.detail-meta .meta-item { color: var(--gray-600); }
.detail-section { margin-bottom: var(--space-4); }
.detail-section h4 { font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--gray-800); margin: 0 0 var(--space-2); }
.detail-section p { font-size: var(--text-sm); color: var(--gray-600); line-height: var(--leading-relaxed); margin: 0; white-space: pre-wrap; }
.apply-job-info { margin-bottom: var(--space-4); padding: var(--space-4); background: var(--gray-50); border-radius: var(--radius-md); }
.apply-job-info h4 { margin: 0 0 var(--space-1); font-size: var(--text-md); font-weight: var(--weight-semibold); color: var(--gray-900); }
.apply-job-info p { margin: 0; font-size: var(--text-sm); color: var(--gray-500); }

/* ── 响应式 ──────────────────────────────── */
@media (max-width: 768px) {
  .jobs-page {
    padding: var(--space-4);
  }
  .search-card {
    padding: var(--space-4);
  }
  .search-row {
    flex-wrap: wrap;
  }
  .search-input {
    min-width: 100%;
    margin-bottom: var(--space-2);
  }
  .filter-row {
    flex-wrap: wrap;
  }
  .filter-item {
    min-width: calc(50% - var(--space-2));
  }
  .slider-row {
    flex-direction: column;
    gap: var(--space-3);
  }
  .jobs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
