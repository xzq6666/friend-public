<template>
  <div class="page-container-full">
    <div class="page-header-row">
      <h2>职位订阅管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建订阅
      </el-button>
    </div>

    <!-- 订阅列表 -->
    <div v-loading="loading" class="subscription-list">
      <el-empty v-if="!loading && subscriptions.length === 0" description="暂无订阅，点击右上角创建" />
      
      <el-card v-for="sub in subscriptions" :key="sub.id" class="subscription-card" shadow="hover">
        <div class="card-header">
          <div class="header-left">
            <h3>{{ sub.name }}</h3>
            <el-tag :type="sub.isActive === 1 ? 'success' : 'info'" size="small">
              {{ sub.isActive === 1 ? '已激活' : '已停用' }}
            </el-tag>
            <el-tag size="small">{{ getPushStrategyText(sub.pushStrategy) }}</el-tag>
          </div>
          <div class="header-right">
            <el-switch
              v-model="sub.isActive"
              :active-value="1"
              :inactive-value="0"
              @change="handleToggle(sub)"
            />
          </div>
        </div>

        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="label">关键词：</span>
              <div class="keywords">
                <el-tag v-for="kw in parseKeywords(sub.keywords)" :key="kw" size="small" class="keyword-tag">
                  {{ kw }}
                </el-tag>
              </div>
            </div>
            
            <div class="info-row">
              <div class="info-item">
                <span class="label">薪资范围：</span>
                <span>{{ formatSalary(sub.salaryMin, sub.salaryMax) }}</span>
              </div>
              <div class="info-item">
                <span class="label">工作地点：</span>
                <span>{{ sub.location || '不限' }}</span>
              </div>
            </div>

            <div class="info-row">
              <div class="info-item">
                <span class="label">经验要求：</span>
                <span>{{ sub.experienceRequired || '不限' }}</span>
              </div>
              <div class="info-item">
                <span class="label">学历要求：</span>
                <span>{{ sub.educationRequired || '不限' }}</span>
              </div>
            </div>

            <div class="info-row">
              <div class="info-item">
                <span class="label">累计匹配：</span>
                <span class="match-count">{{ sub.matchCount || 0 }} 个职位</span>
              </div>
              <div class="info-item">
                <span class="label">最后推送：</span>
                <span>{{ sub.lastPushTime ? formatTime(sub.lastPushTime) : '从未' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <el-button size="small" @click="handleEdit(sub)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button size="small" @click="handleViewHistory(sub)">
            <el-icon><Document /></el-icon>
            推送记录
          </el-button>
          <el-button size="small" type="primary" @click="handleTriggerMatch(sub)" :loading="matchingId === sub.id">
            <el-icon><Refresh /></el-icon>
            {{ matchingId === sub.id ? 'AI匹配中...' : '立即匹配' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(sub)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑订阅' : '新建订阅'"
      width="700px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="订阅名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：Java高级开发岗位" />
        </el-form-item>

        <el-form-item label="关键词" prop="keywords">
          <el-select
            v-model="form.keywordList"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入关键词后按回车，如：Java、Spring Boot"
            style="width: 100%"
          >
            <el-option
              v-for="item in form.keywordList"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <div class="form-tip">提示：输入技能、职位名等关键词，系统将匹配包含这些词的职位</div>
        </el-form-item>

        <el-form-item label="行业分类">
          <el-select v-model="form.categoryId" placeholder="选择行业" clearable style="width: 100%">
            <el-option label="IT/互联网" :value="1" />
            <el-option label="金融/会计" :value="2" />
            <el-option label="教育" :value="3" />
            <el-option label="医疗" :value="4" />
            <el-option label="制造" :value="5" />
            <el-option label="销售" :value="6" />
            <el-option label="行政" :value="7" />
            <el-option label="建筑" :value="8" />
            <el-option label="传媒" :value="9" />
            <el-option label="服务" :value="10" />
          </el-select>
        </el-form-item>

        <el-form-item label="薪资范围">
          <div class="salary-range">
            <el-input-number
              v-model="form.salaryMin"
              :min="0"
              :max="100"
              placeholder="最低"
              style="width: 45%"
            />
            <span class="range-separator">-</span>
            <el-input-number
              v-model="form.salaryMax"
              :min="0"
              :max="100"
              placeholder="最高"
              style="width: 45%"
            />
            <span class="unit">K</span>
          </div>
        </el-form-item>

        <el-form-item label="工作地点">
          <el-input v-model="form.location" placeholder="例如：北京、上海" />
        </el-form-item>

        <el-form-item label="经验要求">
          <el-select v-model="form.experienceRequired" placeholder="选择经验要求" clearable style="width: 100%">
            <el-option label="不限" value="" />
            <el-option label="应届生" value="应届生" />
            <el-option label="1-3年" value="1-3年" />
            <el-option label="3-5年" value="3-5年" />
            <el-option label="5-10年" value="5-10年" />
            <el-option label="10年以上" value="10年以上" />
          </el-select>
        </el-form-item>

        <el-form-item label="学历要求">
          <el-select v-model="form.educationRequired" placeholder="选择学历要求" clearable style="width: 100%">
            <el-option label="不限" value="" />
            <el-option label="高中" value="高中" />
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>

        <el-form-item label="推送策略">
          <el-radio-group v-model="form.pushStrategy">
            <el-radio value="realtime">实时推送</el-radio>
            <el-radio value="daily">每日汇总</el-radio>
            <el-radio value="weekly">每周精选</el-radio>
          </el-radio-group>
          <div class="form-tip">
            实时：发现新职位立即通知 | 
            每日：每天早上9点推送 | 
            每周：每周一早上9点推送
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 推送记录对话框 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="推送记录"
      width="800px"
    >
      <el-table :data="pushHistory" v-loading="historyLoading" stripe max-height="450">
        <el-table-column prop="title" label="职位名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link v-if="row.jobId" type="primary" @click="goToJob(row.jobId)">{{ row.title || '-' }}</el-link>
            <span v-else>{{ row.title || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="薪资" width="120">
          <template #default="{ row }">
            {{ row.salaryMin && row.salaryMax ? `${row.salaryMin}-${row.salaryMax}K` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.location || '-' }}</template>
        </el-table-column>
        <el-table-column prop="experienceRequired" label="经验" width="100">
          <template #default="{ row }">{{ row.experienceRequired || '-' }}</template>
        </el-table-column>
        <el-table-column prop="educationRequired" label="学历" width="80">
          <template #default="{ row }">{{ row.educationRequired || '-' }}</template>
        </el-table-column>
        <el-table-column label="推送时间" width="170">
          <template #default="{ row }">{{ formatTime(row.pushTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyLoading && pushHistory.length === 0" description="暂无推送记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Document } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  createSubscription,
  updateSubscription,
  deleteSubscription,
  getMySubscriptions,
  toggleSubscription,
  triggerMatch,
  getPushHistory
} from '@/api/subscription'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const subscriptions = ref([])
const formRef = ref(null)
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const pushHistory = ref([])
const matchingId = ref(null)

const form = reactive({
  id: null,
  name: '',
  keywordList: [],
  categoryId: null,
  salaryMin: null,
  salaryMax: null,
  location: '',
  experienceRequired: '',
  educationRequired: '',
  pushStrategy: 'daily'
})

const rules = {
  name: [{ required: true, message: '请输入订阅名称', trigger: 'blur' }],
  keywordList: [
    { 
      required: true, 
      message: '请至少输入一个关键词', 
      trigger: 'change',
      validator: (rule, value, callback) => {
        if (!value || value.length === 0) {
          callback(new Error('请至少输入一个关键词'))
        } else {
          callback()
        }
      }
    }
  ]
}

// 获取订阅列表
const fetchSubscriptions = async () => {
  loading.value = true
  try {
    const res = await getMySubscriptions()
    subscriptions.value = res.data || []
  } catch (error) {
    ElMessage.error('获取订阅列表失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 显示编辑对话框
const handleEdit = (sub) => {
  isEdit.value = true
  Object.assign(form, {
    id: sub.id,
    name: sub.name,
    keywordList: parseKeywords(sub.keywords),
    categoryId: sub.categoryId,
    salaryMin: sub.salaryMin,
    salaryMax: sub.salaryMax,
    location: sub.location,
    experienceRequired: sub.experienceRequired,
    educationRequired: sub.educationRequired,
    pushStrategy: sub.pushStrategy
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      ...form,
      keywords: JSON.stringify(form.keywordList)
    }
    delete data.keywordList

    if (isEdit.value) {
      await updateSubscription(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createSubscription(data)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    fetchSubscriptions()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 删除订阅
const handleDelete = async (sub) => {
  try {
    await ElMessageBox.confirm(`确定要删除订阅「${sub.name}」吗？`, '提示', {
      type: 'warning'
    })
    await deleteSubscription(sub.id)
    ElMessage.success('删除成功')
    fetchSubscriptions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换激活状态
const handleToggle = async (sub) => {
  try {
    await toggleSubscription(sub.id, sub.isActive)
    ElMessage.success(sub.isActive === 1 ? '已激活' : '已停用')
  } catch (error) {
    sub.isActive = sub.isActive === 1 ? 0 : 1 // 回滚
    ElMessage.error('操作失败')
  }
}

// 手动触发匹配（AI智能匹配）
const handleTriggerMatch = async (sub) => {
  matchingId.value = sub.id
  try {
    const res = await triggerMatch(sub.id)
    if (res.matchCount > 0) {
      ElMessage.success(res.message || `AI匹配到 ${res.matchCount} 个相关职位`)
    } else {
      ElMessage.info(res.message || '暂无新的匹配职位')
    }
    fetchSubscriptions()
  } catch (error) {
    ElMessage.error('匹配失败，请稍后重试')
  } finally {
    matchingId.value = null
  }
}

// 查看推送记录
const handleViewHistory = async (sub) => {
  historyDialogVisible.value = true
  historyLoading.value = true
  pushHistory.value = []
  try {
    const res = await getPushHistory(sub.id)
    pushHistory.value = res.data || []
  } catch (error) {
    ElMessage.error('获取推送记录失败')
  } finally {
    historyLoading.value = false
  }
}

// 跳转到职位详情
const goToJob = (jobId) => {
  router.push(`/browse-jobs?highlight=${jobId}`)
  historyDialogVisible.value = false
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    name: '',
    keywordList: [],
    categoryId: null,
    salaryMin: null,
    salaryMax: null,
    location: '',
    experienceRequired: '',
    educationRequired: '',
    pushStrategy: 'daily'
  })
}

// 解析关键词JSON
const parseKeywords = (keywordsJson) => {
  if (!keywordsJson) return []
  try {
    return JSON.parse(keywordsJson)
  } catch {
    return []
  }
}

// 格式化薪资
const formatSalary = (min, max) => {
  if (min && max) return `${min}-${max}K`
  if (min) return `${min}K以上`
  if (max) return `${max}K以下`
  return '不限'
}

// 获取推送策略文本
const getPushStrategyText = (strategy) => {
  const map = {
    realtime: '实时推送',
    daily: '每日汇总',
    weekly: '每周精选'
  }
  return map[strategy] || strategy
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchSubscriptions()
})
</script>

<style scoped>
.page-container-full {
  padding: var(--space-6);
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.page-header-row h2 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.subscription-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.subscription-card {
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border) !important;
  transition: all var(--duration-normal) var(--ease-out);
}

.subscription-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md) !important;
  border-color: var(--gray-300) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--gray-100);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.header-left h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.card-body {
  margin-bottom: var(--space-4);
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.info-item .label {
  color: var(--color-text-muted);
  min-width: 80px;
  font-weight: var(--weight-medium);
}

.keywords {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.keyword-tag {
  background: var(--gray-50);
  color: var(--gray-700);
  border: 1px solid var(--color-border);
}

.match-count {
  color: var(--success-600);
  font-weight: var(--weight-semibold);
}

.salary-range {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
}

.range-separator {
  color: var(--gray-400);
}

.unit {
  color: var(--gray-500);
  font-size: var(--text-sm);
}

.form-tip {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  line-height: 1.5;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--gray-100);
}
</style>
