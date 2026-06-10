<template>
<div class="page-container">
  <div class="page-header-row">
    <h2>人才市场</h2>
  </div>

  <div class="filter-bar">
    <el-input v-model="searchKeyword" placeholder="搜索姓名、技能..." :prefix-icon="Search" clearable style="width: 260px;" @clear="fetchTalent" @keyup.enter="fetchTalent"/>
    <el-select v-model="filterEducation" placeholder="学历要求" clearable style="width: 140px;" @change="fetchTalent">
      <el-option label="不限" value="" />
      <el-option label="大专" value="大专" />
      <el-option label="本科" value="本科" />
      <el-option label="硕士" value="硕士" />
      <el-option label="博士" value="博士" />
    </el-select>
    <el-button class="btn-primary" @click="fetchTalent">搜索</el-button>
  </div>

  <div class="talent-grid" v-loading="loading">
    <el-empty v-if="!loading && talents.length === 0" description="暂无人才数据" />
    <div v-else class="talent-card" v-for="talent in talents" :key="talent.id">
      <div class="card-header">
        <div class="talent-avatar">{{ talent.name?.charAt(0) || '未' }}</div>
        <div class="talent-info">
          <h3 class="clickable" @click="$router.push(`/r/resume/${talent.id}`)">{{ talent.name || '未知' }}</h3>
          <span class="talent-age">{{ talent.age || '-' }}岁 · {{ talent.education || '-' }}</span>
        </div>
        <el-button 
          class="favorite-btn" 
          :type="talent.isFavorited ? 'warning' : 'default'" 
          :icon="talent.isFavorited ? Star : StarFilled" 
          circle 
          size="small"
          @click="toggleFavorite(talent)"
        />
      </div>
      <div class="card-body">
        <div class="info-row">
          <span class="label">期望薪资</span>
          <span class="value">{{ talent.expectedSalary ? (talent.expectedSalary / 1000).toFixed(0) + 'K' : '面议' }}</span>
        </div>
        <div class="info-row">
          <span class="label">工作年限</span>
          <span class="value">{{ talent.experience || '-' }}</span>
        </div>
        <div class="skill-tags" v-if="parseSkills(talent.skills).length">
          <el-tag v-for="skill in parseSkills(talent.skills).slice(0, 5)" :key="skill" size="small" class="skill-tag">{{ skill }}</el-tag>
          <span v-if="parseSkills(talent.skills).length > 5" class="more-skills">+{{ parseSkills(talent.skills).length - 5 }}</span>
        </div>
      </div>
      <div class="card-footer">
        <el-button type="primary" plain size="small" @click="viewTalent(talent)">
          <el-icon><View /></el-icon> 查看详情
        </el-button>
        <el-button type="success" size="small" @click="startChat(talent)">
          <el-icon><ChatDotRound /></el-icon> 立即沟通
        </el-button>
        <el-button type="warning" plain size="small" @click="openReportDialog(talent.userId, talent.name)">
          <el-icon><WarnTriangleFilled /></el-icon> 举报
        </el-button>
      </div>
    </div>
  </div>

  <div class="pagination-wrapper" v-if="total > 0">
    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[12, 24, 48]" layout="total, sizes, prev, pager, next" @size-change="fetchTalent" @current-change="fetchTalent"/>
  </div>

  <el-dialog v-model="detailVisible" title="人才详情" width="700px" destroy-on-close>
    <div v-if="currentTalent" class="talent-detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名"><span class="clickable" @click="$router.push(`/r/resume/${currentTalent.id}`)">{{ currentTalent.name }}</span></el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentTalent.age || '-' }}岁</el-descriptions-item>
        <el-descriptions-item label="学历">{{ currentTalent.education || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望薪资">{{ currentTalent.expectedSalary ? (currentTalent.expectedSalary / 1000).toFixed(0) + 'K' : '面议' }}</el-descriptions-item>
        <el-descriptions-item label="技能" :span="2">{{ currentTalent.skills || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作年限" :span="2">{{ currentTalent.experience || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="currentTalent.workExperience" class="work-exp-section">
        <h4>工作经历</h4>
        <div v-for="(exp, i) in parseWorkExp(currentTalent.workExperience)" :key="i" class="work-exp-detail">
          <div class="exp-header"><strong>{{ exp.company || '未知公司' }}</strong><span class="exp-position">{{ exp.position || '' }}</span></div>
          <div class="exp-date">{{ exp.startDate || '' }} ~ {{ exp.current ? '至今' : (exp.endDate || '') }}</div>
          <p v-if="exp.description" class="exp-desc">{{ exp.description }}</p>
          <el-divider v-if="i < parseWorkExp(currentTalent.workExperience).length - 1" style="margin: 12px 0"/>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button type="warning" plain @click="openReportDialog(currentTalent.userId, currentTalent.name)"><el-icon><WarnTriangleFilled /></el-icon> 举报</el-button>
      <el-button type="success" @click="startChat(currentTalent)"><el-icon><ChatDotRound /></el-icon> 发起沟通</el-button>
    </template>
  </el-dialog>

  <!-- 举报对话框 -->
  <el-dialog v-model="reportDialogVisible" title="举报用户" width="500px">
    <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-width="100px">
      <el-form-item label="被举报用户">
        <el-input :value="reportTargetName" disabled />
      </el-form-item>
      <el-form-item label="举报原因" prop="reason">
        <el-select v-model="reportForm.reason" placeholder="请选择举报原因" style="width: 100%">
          <el-option label="虚假简历" value="虚假简历" />
          <el-option label="欺诈行为" value="欺诈行为" />
          <el-option label="违规内容" value="违规内容" />
          <el-option label="人身攻击" value="人身攻击" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Search, View, ChatDotRound, Star, StarFilled, WarnTriangleFilled } from '@element-plus/icons-vue'

const router = useRouter()
const talents = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const searchKeyword = ref('')
const filterEducation = ref('')

const detailVisible = ref(false)
const currentTalent = ref(null)

const fetchTalent = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterEducation.value) params.education = filterEducation.value

    const res = await request.get('/resume/talent-market', { params })
    const records = res.records || []
    
    // 检查每个简历的收藏状态
    for (const talent of records) {
      try {
        // 查询是否已收藏该简历
        const checkRes = await request.get(`/favorite/check?targetType=2&targetId=${talent.id}`)
        talent.isFavorited = checkRes.favorited || false
      } catch {
        talent.isFavorited = false
      }
    }
    
    talents.value = records
    total.value = res.total || 0
  } catch {
    talents.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const viewTalent = (row) => {
  currentTalent.value = row
  detailVisible.value = true
}

const startChat = async (talent) => {
  if (!talent) return
  
  try {
    // 创建一个临时的面试记录用于聊天
    const chatData = {
      applicationId: null,
      interviewTime: new Date().toISOString(),
      interviewLocation: '在线沟通',
      interviewType: 2,
      contactPerson: '企业HR',
      contactPhone: '',
      notes: `与 ${talent.name} 的在线沟通`,
      resumeId: talent.id,
      directChat: true
    }
    
    const res = await request.post('/interview', chatData)
    ElMessage.success('沟通通道已建立')
    router.push({ name: 'InterviewChat', params: { id: res.id } })
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '建立沟通失败')
  }
}

const parseSkills = (skills) => {
  if (!skills) return []
  try {
    return typeof skills === 'string' ? JSON.parse(skills) : skills
  } catch {
    return []
  }
}

const parseWorkExp = (exp) => {
  if (!exp) return []
  try {
    return typeof exp === 'string' ? JSON.parse(exp) : exp
  } catch {
    return []
  }
}

// 切换收藏状态
const toggleFavorite = async (talent) => {
  try {
    if (talent.isFavorited) {
      // 取消收藏
      await request.delete('/favorite', {
        params: {
          targetType: 2,
          targetId: talent.id
        }
      })
      talent.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      // 添加收藏
      await request.post('/favorite', {
        targetType: 2, // 2表示简历
        targetId: talent.id
      })
      talent.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '操作失败')
  }
}

// 举报相关
const reportDialogVisible = ref(false)
const reportFormRef = ref()
const reportTargetId = ref(null)
const reportTargetName = ref('')
const submittingReport = ref(false)
const reportForm = reactive({
  reason: '',
  description: ''
})
const reportRules = {
  reason: [{ required: true, message: '请选择举报原因', trigger: 'change' }]
}

const openReportDialog = (userId, name) => {
  reportTargetId.value = userId
  reportTargetName.value = name || ''
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
      reportedId: reportTargetId.value,
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

onMounted(() => {
  fetchTalent()
})
</script>

<style scoped>
.filter-bar .el-input { width: 300px; }

.talent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.talent-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  padding: var(--space-6);
  transition: all var(--duration-normal) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.talent-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gray-900);
  opacity: 0;
  transition: opacity var(--duration-normal) var(--ease-out);
}

.talent-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}

.talent-card:hover::before {
  opacity: 1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  position: relative;
}

.favorite-btn {
  margin-left: auto;
  transition: all var(--duration-slow) var(--ease-out);
}

.favorite-btn:hover {
  transform: scale(1.1);
}

.talent-avatar {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--gray-900) 0%, var(--gray-700) 100%);
  color: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  flex-shrink: 0;
  box-shadow: var(--shadow-md);
}

.talent-info h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.clickable {
  cursor: pointer;
  transition: color var(--duration-fast);
}
.clickable:hover {
  color: var(--primary-600);
}

.talent-age {
  font-size: 13px;
  color: var(--gray-500);
}

.card-body {
  margin-bottom: var(--space-4);
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
  font-size: var(--text-base);
}

.info-row .label {
  color: var(--gray-500);
}

.info-row .value {
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.skill-tag {
  background: var(--gray-50);
  border-color: var(--gray-200);
  color: var(--gray-700);
}

.more-skills {
  font-size: var(--text-sm);
  color: var(--gray-500);
  line-height: var(--space-6);
}

.card-footer {
  display: flex;
  gap: var(--space-2);
  padding-top: var(--space-4);
  border-top: 1px solid var(--gray-100);
}

.talent-detail {
  padding: var(--space-4) 0;
}

.work-exp-section {
  margin-top: var(--space-5);
}

.work-exp-section h4 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-md);
  color: var(--gray-800);
}

.work-exp-detail {
  padding: var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-3);
  border: 1px solid var(--gray-100);
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-1);
}

.exp-position {
  font-size: 13px;
  color: var(--gray-600);
}

.exp-date {
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin-bottom: var(--space-2);
}

.exp-desc {
  font-size: 13px;
  color: var(--gray-700);
  line-height: 1.6;
  margin: 0;
}
</style>
