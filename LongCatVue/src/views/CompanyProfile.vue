<template>
  <div class="company-profile-page">
    <!-- 状态横幅 -->
    <div class="status-banner" :class="statusClass">
      <div class="status-icon">{{ statusIcon }}</div>
      <div class="status-info">
        <div class="status-title">{{ statusTitle }}</div>
        <div class="status-desc">{{ statusDesc }}</div>
      </div>
    </div>

    <!-- 基本信息 -->
    <div class="form-section">
      <div class="section-header">
        <div class="section-icon" style="color: #409eff;">&#127970;</div>
        <div class="section-title">企业基本信息</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入公司全称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业" prop="industry">
              <el-select v-model="form.industry" placeholder="请选择行业" style="width: 100%">
                <el-option label="信息技术/互联网" value="信息技术/互联网" />
                <el-option label="金融/会计" value="金融/会计" />
                <el-option label="教育/培训" value="教育/培训" />
                <el-option label="医疗/健康" value="医疗/健康" />
                <el-option label="制造/工程" value="制造/工程" />
                <el-option label="销售/市场" value="销售/市场" />
                <el-option label="行政/人事" value="行政/人事" />
                <el-option label="建筑/房地产" value="建筑/房地产" />
                <el-option label="传媒/设计" value="传媒/设计" />
                <el-option label="服务业" value="服务业" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司规模" prop="companyScale">
              <el-select v-model="form.companyScale" placeholder="请选择规模" style="width: 100%">
                <el-option label="1-20人" value="1-20人" />
                <el-option label="20-99人" value="20-99人" />
                <el-option label="100-499人" value="100-499人" />
                <el-option label="500-999人" value="500-999人" />
                <el-option label="1000人以上" value="1000人以上" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司官网">
              <el-input v-model="form.website" placeholder="https://www.example.com" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公司地址">
          <el-input v-model="form.address" placeholder="请输入公司详细地址" />
        </el-form-item>
        <el-form-item label="公司简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请介绍公司业务范围、发展历程、企业文化等" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
    </div>

    <!-- 联系人信息 -->
    <div class="form-section">
      <div class="section-header">
        <div class="section-icon" style="color: #e6a23c;">&#128222;</div>
        <div class="section-title">联系人信息</div>
      </div>
      <el-form :model="form" label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="联系人姓名" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 企业认证材料 -->
    <div class="form-section">
      <div class="section-header">
        <div class="section-icon" style="color: #67c23a;">&#128196;</div>
        <div class="section-title">企业认证材料</div>
        <el-tag size="small" type="info" effect="plain" round>选填</el-tag>
      </div>
      <el-form label-position="top">
        <el-form-item label="营业执照">
          <el-upload
            class="license-uploader"
            action="/api/company/upload/license"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleLicenseSuccess"
            :before-upload="beforeLicenseUpload"
          >
            <img v-if="form.businessLicense" :src="form.businessLicense" class="license-image" />
            <div v-else class="license-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传营业执照</div>
              <div class="upload-hint">支持 JPG、PNG 格式，大小不超过 5MB</div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作按钮 -->
    <div class="form-actions">
      <el-button @click="resetForm" size="large">重置</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="saving" size="large">
        {{ companyInfo?.verified === 3 ? '重新提交认证' : companyInfo?.businessLicense ? '更新信息' : '保存并提交认证' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { OfficeBuilding, User, Document, Plus } from '@element-plus/icons-vue'

const statusClass = computed(() => {
  const v = companyInfo.value?.verified
  if (v === 2) return 'status-success'
  if (v === 1) return 'status-warning'
  if (v === 3) return 'status-error'
  return 'status-empty'
})
const statusIcon = computed(() => {
  const v = companyInfo.value?.verified
  if (v === 2) return '✓'
  if (v === 1) return '⏳'
  if (v === 3) return '✕'
  return '📋'
})
const statusTitle = computed(() => {
  const v = companyInfo.value?.verified
  if (v === 2) return '已认证'
  if (v === 1) return '待审核'
  if (v === 3) return '认证未通过'
  return '未提交认证'
})
const statusDesc = computed(() => {
  const v = companyInfo.value?.verified
  if (v === 2) return '您的企业认证已通过审核，求职者将看到认证标识'
  if (v === 1) return '您的认证申请正在审核中，请耐心等待'
  if (v === 3) return companyInfo.value?.verifyRemark || '请修改后重新提交认证'
  return '完善企业信息并提交认证，提升求职者信任度'
})

const formRef = ref()
const saving = ref(false)
const companyInfo = ref(null)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}))

const form = reactive({
  companyName: '',
  companyScale: '',
  industry: '',
  description: '',
  address: '',
  website: '',
  businessLicense: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: ''
})

const rules = {
  companyName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' }
  ],
  industry: [
    { required: true, message: '请选择所属行业', trigger: 'change' }
  ],
  companyScale: [
    { required: true, message: '请选择公司规模', trigger: 'change' }
  ],
  contactPerson: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 加载企业信息
const loadCompanyInfo = async () => {
  try {
    const res = await request.get('/company/my')
    if (res) {
      companyInfo.value = res
      Object.assign(form, res)
    }
  } catch (error) {
    console.error('加载企业信息失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await request.post('/company/save', form)
    const msg = companyInfo.value?.verified === 3 ? '认证已重新提交，请等待审核'
      : companyInfo.value?.businessLicense ? '信息更新成功' : '信息保存成功，认证申请已提交'
    ElMessage.success(msg)
    await loadCompanyInfo()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '操作失败')
  } finally {
    saving.value = false
  }
}

// 重置表单
const resetForm = () => {
  formRef.value.resetFields()
  if (companyInfo.value) {
    Object.assign(form, companyInfo.value)
  }
}

// 营业执照上传前校验
const beforeLicenseUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

// 营业执照上传成功
const handleLicenseSuccess = (response) => {
  form.businessLicense = response.url || response.fileUrl
  ElMessage.success('上传成功')
}

onMounted(() => {
  loadCompanyInfo()
})
</script>

<style scoped>
/* ── 页面容器 ─────────────────────────────── */
.company-profile-page {
  padding: var(--space-6);
  max-width: 800px;
  margin: 0 auto;
}

/* ── 状态横幅 ─────────────────────────────── */
.status-banner {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-5);
}

.status-success { background: linear-gradient(135deg, #67c23a, #85ce61); }
.status-warning { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
.status-error { background: linear-gradient(135deg, #f56c6c, #f89898); }
.status-empty { background: linear-gradient(135deg, #909399, #b0b3b6); }

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.status-info { flex: 1; }
.status-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}
.status-desc {
  font-size: 13px;
  color: rgba(255,255,255,0.85);
  margin-top: 4px;
}

/* ── 表单区块 ─────────────────────────────── */
.form-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  margin-bottom: var(--space-4);
  transition: box-shadow 0.2s;
}
.form-section:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--gray-100);
}

.section-icon {
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

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--gray-900);
}

/* ── 营业执照上传 ─────────────────────────── */
.license-uploader {
  width: 100%;
}

.license-image {
  width: 240px;
  height: 160px;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 2px solid var(--color-border);
  transition: border-color 0.2s;
}
.license-image:hover {
  border-color: var(--primary-400);
}

.license-placeholder {
  width: 240px;
  height: 160px;
  border: 2px dashed var(--gray-300);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--gray-50);
}

.license-placeholder:hover {
  border-color: var(--primary-400);
  background: var(--primary-50);
}

.upload-icon {
  font-size: 28px;
  color: var(--gray-400);
  margin-bottom: var(--space-2);
}

.upload-text {
  font-size: var(--text-sm);
  color: var(--gray-600);
}

.upload-hint {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 4px;
}

/* ── 表单操作 ─────────────────────────────── */
.form-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  padding: var(--space-5) 0;
}
</style>
