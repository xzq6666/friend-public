<template>
  <div class="company-verify-page">
    <div class="page-header">
      <h2>企业认证审核</h2>
      <p class="header-desc">审核企业的认证申请，确保平台信息安全可靠</p>
    </div>

    <div class="stats-section">
      <div 
        v-for="stat in statItems" 
        :key="stat.key"
        class="stat-card"
        :class="{ active: activeStatus === stat.key, clickable: true }"
        @click="setStatusFilter(stat.key)"
      >
        <div class="stat-icon-wrap" :style="{ background: stat.bgColor }">
          <el-icon :size="20" :style="{ color: stat.iconColor }"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-count" :style="{ color: stat.iconColor }">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div v-if="activeStatus === stat.key" class="stat-indicator"></div>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">认证状态</span>
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 150px" @change="fetchCompanies">
            <el-option label="全部" :value="null" />
            <el-option label="未认证" :value="0" />
            <el-option label="待审核" :value="1" />
            <el-option label="已认证" :value="2" />
            <el-option label="认证失败" :value="3" />
          </el-select>
        </div>
        <div class="filter-group">
          <span class="filter-label">企业名称</span>
          <el-input v-model="filterName" placeholder="输入企业名称" style="width: 200px" @keyup.enter="fetchCompanies" />
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="fetchCompanies" class="action-btn">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilter" class="action-btn">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </div>
      </div>
    </div>

    <div class="table-wrapper">
      <div class="table-header-row">
        <span class="table-title">企业认证列表</span>
        <div class="table-actions">
          <el-button 
            v-if="selectedCompanies.length > 0" 
            type="danger" 
            size="small"
            @click="batchReject"
          >
            <el-icon><Close /></el-icon> 批量拒绝 ({{ selectedCompanies.length }})
          </el-button>
        </div>
      </div>

      <div class="table-card">
        <el-table 
          v-loading="loading" 
          :data="filteredCompanies" 
          stripe 
          style="width: 100%"
          :default-sort="{ prop: 'createTime', order: 'descending' }"
          @select="handleSelect"
          @select-all="handleSelectAll"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="企业名称" min-width="200">
            <template #default="{ row }">
              <div class="company-name-cell">
                <el-icon :size="18" style="color: var(--primary-500)"><OfficeBuilding /></el-icon>
                <span class="company-name">{{ row.companyName }}</span>
                <el-tag :type="getVerifyTagType(row.verified)" size="small" effect="plain" class="verify-tag">
                  {{ getVerifyStatusText(row.verified) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="industry" label="行业" width="150" />
          <el-table-column prop="companyScale" label="规模" width="120" />
          <el-table-column label="联系人" width="150">
            <template #default="{ row }">
              <div class="contact-info">
                <span class="contact-name">{{ row.contactPerson }}</span>
                <span class="contact-phone">{{ row.contactPhone }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
          <el-table-column label="提交时间" width="180" prop="createTime" :sortable="true">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="viewDetail(row)" class="action-btn-detail">
                  <el-icon><View /></el-icon> 详情
                </el-button>
                <el-button 
                  v-if="row.verified === 1" 
                  type="success" 
                  link 
                  size="small" 
                  @click="showApproveDialog(row)"
                  class="action-btn-approve"
                >
                  <el-icon><Check /></el-icon> 通过
                </el-button>
                <el-button 
                  v-if="row.verified === 1" 
                  type="danger" 
                  link 
                  size="small" 
                  @click="showRejectDialog(row)"
                  class="action-btn-reject"
                >
                  <el-icon><Close /></el-icon> 拒绝
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <span class="pagination-info">共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="sizes, prev, pager, next"
            @size-change="fetchCompanies"
            @current-change="fetchCompanies"
          />
        </div>
      </div>
    </div>

    <el-dialog 
      v-model="detailDialogVisible" 
      width="720px" 
      class="company-detail-dialog"
    >
      <div v-if="currentCompany" class="report-detail">
        <div class="detail-header">
          <div class="detail-title-row">
            <div class="detail-title-group">
              <el-icon :size="28" style="color: var(--primary-500); margin-right: var(--space-2)"><OfficeBuilding /></el-icon>
              <span class="detail-id">企业ID: {{ currentCompany.id }}</span>
            </div>
            <el-tag :type="getVerifyTagType(currentCompany.verified)" class="detail-status">
              {{ getVerifyStatusText(currentCompany.verified) }}
            </el-tag>
          </div>
          <h3 class="detail-name">{{ currentCompany.companyName }}</h3>
        </div>
        
        <div class="detail-content">
          <div class="detail-section">
            <h4 class="section-title">基本信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">公司规模</span>
                <span class="info-value">{{ currentCompany.companyScale || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">所属行业</span>
                <span class="info-value">{{ currentCompany.industry || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">公司地址</span>
                <span class="info-value">{{ currentCompany.address || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">公司官网</span>
                <span class="info-value">
                  <a v-if="currentCompany.website" :href="currentCompany.website" target="_blank" class="website-link">{{ currentCompany.website }}</a>
                  <span v-else>-</span>
                </span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4 class="section-title">联系信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">联系人</span>
                <span class="info-value">{{ currentCompany.contactPerson || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">联系电话</span>
                <span class="info-value">{{ currentCompany.contactPhone || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">联系邮箱</span>
                <span class="info-value">{{ currentCompany.contactEmail || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4 class="section-title">公司简介</h4>
            <div class="description-box">{{ currentCompany.description || '暂无简介' }}</div>
          </div>

          <div class="detail-section">
            <h4 class="section-title">营业执照</h4>
            <div v-if="currentCompany.businessLicense" class="license-preview">
              <el-image
                :src="currentCompany.businessLicense"
                :preview-src-list="[currentCompany.businessLicense]"
                fit="cover"
                class="license-image"
              />
            </div>
            <div v-else class="no-license">未上传营业执照</div>
          </div>

          <div v-if="currentCompany.verifyRemark" class="detail-section">
            <h4 class="section-title">审核备注</h4>
            <div class="remark-box">{{ currentCompany.verifyRemark }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <div class="footer-actions">
          <el-button
            v-if="currentCompany?.verified === 1"
            type="success"
            @click="detailDialogVisible = false; showApproveDialog(currentCompany)"
          >
            <el-icon><Check /></el-icon> 通过认证
          </el-button>
          <el-button
            v-if="currentCompany?.verified === 1"
            type="danger"
            @click="detailDialogVisible = false; showRejectDialog(currentCompany)"
          >
            <el-icon><Close /></el-icon> 拒绝认证
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="approveDialogVisible" title="通过认证" width="500px" class="handle-dialog">
      <div class="handle-form-container">
        <div class="form-section">
          <div class="form-info-row">
            <span class="info-badge">企业ID: {{ currentCompany?.id }}</span>
            <el-tag type="success">{{ currentCompany?.companyName }}</el-tag>
          </div>
        </div>

        <el-form :model="approveForm" class="handle-form">
          <el-form-item label="审核备注">
            <el-input 
              v-model="approveForm.remark" 
              type="textarea" 
              :rows="3"
              placeholder="可选，填写审核备注"
              class="result-textarea"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleApprove" :loading="submitting" class="submit-btn">
          <el-icon><Check /></el-icon> 确认通过
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝认证" width="500px" class="handle-dialog">
      <div class="handle-form-container">
        <div class="form-section">
          <div class="form-info-row">
            <span class="info-badge">企业ID: {{ currentCompany?.id }}</span>
            <el-tag type="danger">{{ currentCompany?.companyName }}</el-tag>
          </div>
        </div>

        <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" class="handle-form">
          <el-form-item label="拒绝原因" prop="remark">
            <el-input 
              v-model="rejectForm.remark" 
              type="textarea" 
              :rows="4"
              placeholder="请填写拒绝原因，将通知企业用户"
              class="result-textarea"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="submitting" class="submit-btn">
          <el-icon><Close /></el-icon> 确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Clock, CircleCheck, CircleClose, OfficeBuilding, Search, Refresh, 
  View, Check, Close
} from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const companies = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)
const filterName = ref('')
const activeStatus = ref('all')
const selectedCompanies = ref([])

const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0,
  total: 0
})

const statItems = computed(() => [
  { 
    key: 'all', 
    label: '全部', 
    value: stats.total, 
    icon: OfficeBuilding, 
    iconColor: '#4e6496',
    bgColor: 'rgba(78, 100, 150, 0.1)'
  },
  { 
    key: 'pending', 
    label: '待审核', 
    value: stats.pending, 
    icon: Clock, 
    iconColor: '#c08a2e',
    bgColor: 'rgba(192, 138, 46, 0.1)'
  },
  { 
    key: 'approved', 
    label: '已通过', 
    value: stats.approved, 
    icon: CircleCheck, 
    iconColor: '#3ea15d',
    bgColor: 'rgba(62, 161, 93, 0.1)'
  },
  { 
    key: 'rejected', 
    label: '已拒绝', 
    value: stats.rejected, 
    icon: CircleClose, 
    iconColor: '#c03939',
    bgColor: 'rgba(192, 57, 57, 0.1)'
  }
])

const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentCompany = ref(null)

const approveForm = reactive({
  remark: ''
})

const rejectForm = reactive({
  remark: ''
})

const rejectFormRef = ref()
const rejectRules = {
  remark: [
    { required: true, message: '请填写拒绝原因', trigger: 'blur' },
    { min: 5, message: '拒绝原因至少5个字符', trigger: 'blur' }
  ]
}

const filteredCompanies = computed(() => {
  let result = companies.value
  if (activeStatus.value !== 'all') {
    const statusMap = { pending: 1, approved: 2, rejected: 0 }
    const targetStatus = statusMap[activeStatus.value]
    if (targetStatus !== undefined) {
      result = result.filter(c => c.verified === targetStatus)
    }
  }
  if (filterName.value) {
    result = result.filter(c => c.companyName?.toLowerCase().includes(filterName.value.toLowerCase()))
  }
  return result
})

const setStatusFilter = (status) => {
  activeStatus.value = status
}

const handleSelect = (val, row) => {
  const index = selectedCompanies.value.findIndex(c => c.id === row.id)
  if (index > -1) {
    selectedCompanies.value.splice(index, 1)
  } else {
    selectedCompanies.value.push(row)
  }
}

const handleSelectAll = (val) => {
  if (val) {
    selectedCompanies.value = [...filteredCompanies.value]
  } else {
    selectedCompanies.value = []
  }
}

const batchReject = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝选中的 ${selectedCompanies.value.length} 家企业的认证申请吗？`, 
      '提示', 
      { type: 'warning' }
    )
    for (const company of selectedCompanies.value) {
      await request.put(`/admin/company-verify/${company.id}/reject`, {
        remark: '批量拒绝认证申请'
      })
    }
    ElMessage.success('批量拒绝成功')
    selectedCompanies.value = []
    fetchCompanies()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.error || '操作失败')
    }
  }
}

const fetchCompanies = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value !== null) {
      params.verified = filterStatus.value
    }
    if (filterName.value) {
      params.companyName = filterName.value
    }
    
    const res = await request.get('/admin/company-verify/list', { params })
    companies.value = res.records || []
    total.value = res.total || 0
    
    updateStats()
  } catch (error) {
    ElMessage.error('加载失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    loading.value = false
  }
}

const updateStats = async () => {
  try {
    const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
      request.get('/admin/company-verify/list', { params: { page: 1, size: 1, verified: 1 } }),
      request.get('/admin/company-verify/list', { params: { page: 1, size: 1, verified: 2 } }),
      request.get('/admin/company-verify/list', { params: { page: 1, size: 1, verified: 3 } })
    ])
    
    stats.pending = pendingRes.total || 0
    stats.approved = approvedRes.total || 0
    stats.rejected = rejectedRes.total || 0
    stats.total = total.value
  } catch {
    // 忽略统计错误
  }
}

const viewDetail = async (company) => {
  try {
    const res = await request.get(`/admin/company-verify/${company.id}`)
    currentCompany.value = res
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载详情失败')
  }
}

const showApproveDialog = (company) => {
  currentCompany.value = company
  approveForm.remark = ''
  approveDialogVisible.value = true
}

const showRejectDialog = (company) => {
  currentCompany.value = company
  rejectForm.remark = ''
  rejectDialogVisible.value = true
}

const handleApprove = async () => {
  submitting.value = true
  try {
    await request.put(`/admin/company-verify/${currentCompany.value.id}/approve`, {
      remark: approveForm.remark || '认证通过'
    })
    ElMessage.success('审核通过')
    approveDialogVisible.value = false
    fetchCompanies()
  } catch (error) {
    ElMessage.error('操作失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const handleReject = async () => {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await request.put(`/admin/company-verify/${currentCompany.value.id}/reject`, {
      remark: rejectForm.remark
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchCompanies()
  } catch (error) {
    ElMessage.error('操作失败：' + (error.response?.data?.error || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const resetFilter = () => {
  filterStatus.value = null
  filterName.value = ''
  activeStatus.value = 'all'
  currentPage.value = 1
  selectedCompanies.value = []
  fetchCompanies()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const getVerifyStatusText = (status) => {
  const map = {
    0: '未认证',
    1: '待审核',
    2: '已认证',
    3: '认证失败'
  }
  return map[status] || '未知'
}

const getVerifyTagType = (status) => {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchCompanies()
})
</script>

<style scoped>
.company-verify-page {
  padding: var(--space-6);
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--space-6);
}

.page-header h2 {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin-bottom: var(--space-2);
}

.header-desc {
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  transition: all var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--gray-300);
}

.stat-card.active {
  background: var(--gray-50);
  border-color: var(--primary-300);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-count {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-top: 2px;
}

.stat-indicator {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 32px;
  background: var(--primary-500);
  border-radius: 4px 0 0 4px;
}

.filter-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  margin-bottom: var(--space-5);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
  align-items: center;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.filter-label {
  font-size: var(--text-sm);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}

.filter-actions {
  display: flex;
  gap: var(--space-2);
  margin-left: auto;
}

.action-btn {
  min-width: 90px;
}

.table-wrapper {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.table-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.table-actions {
  display: flex;
  gap: var(--space-2);
}

.table-card {
  padding: var(--space-2);
}

.company-name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.company-name {
  font-weight: var(--weight-medium);
  color: var(--gray-800);
}

.verify-tag {
  font-size: var(--text-xs);
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.contact-name {
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.contact-phone {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.action-btn-detail {
  color: var(--primary-600);
}

.action-btn-approve {
  color: var(--success-600);
}

.action-btn-reject {
  color: var(--danger-600);
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-2);
  border-top: 1px solid var(--color-border);
}

.pagination-info {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.company-detail-dialog,
.handle-dialog {
  padding: 0;
}

.report-detail {
  padding: var(--space-6);
}

.detail-header {
  margin-bottom: var(--space-6);
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.detail-title-group {
  display: flex;
  align-items: center;
}

.detail-id {
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.detail-status {
  font-size: var(--text-sm);
  padding: 4px 12px;
}

.detail-name {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-section {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.section-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  margin-bottom: var(--space-3);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.info-value {
  font-size: var(--text-sm);
  color: var(--gray-700);
}

.website-link {
  color: var(--el-color-primary);
  text-decoration: none;
}

.website-link:hover {
  text-decoration: underline;
}

.description-box {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
}

.license-preview {
  display: flex;
  justify-content: center;
}

.license-image {
  width: 280px;
  height: 200px;
  border-radius: var(--radius-md);
  border: 2px solid var(--color-border);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.license-image:hover {
  border-color: var(--primary-400);
  transform: scale(1.02);
}

.no-license {
  padding: var(--space-5);
  text-align: center;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.remark-box {
  font-size: var(--text-sm);
  color: var(--gray-700);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
}

.footer-actions {
  display: flex;
  gap: var(--space-2);
}

.handle-form-container {
  padding: var(--space-6);
}

.form-section {
  margin-bottom: var(--space-5);
}

.form-info-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.info-badge {
  font-size: var(--text-sm);
  color: var(--gray-600);
  font-weight: var(--weight-medium);
}

.handle-form {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  padding: var(--space-5);
}

.result-textarea {
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
}

.submit-btn {
  min-width: 120px;
}

@media (max-width: 768px) {
  .company-verify-page {
    padding: var(--space-4);
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>