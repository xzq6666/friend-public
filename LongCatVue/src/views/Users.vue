<template>
  <div class="page-container">
    <div class="page-header-row">
      <h2>用户管理</h2>
      <div class="page-header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名、邮箱..."
          :prefix-icon="Search"
          clearable
          style="width: 260px;"
          @clear="fetchUsers"
          @keyup.enter="fetchUsers"
        />
      </div>
    </div>

    <div class="table-card">
      <el-table :data="users" v-loading="loading" stripe size="default">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column v-if="isAdmin" prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="isAdmin" prop="phone" label="手机号" width="140" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.userType)" size="small">{{ getUserTypeLabel(row.userType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 220 : 80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewUser(row)">详情</el-button>
            <template v-if="isAdmin">
              <el-button type="warning" link size="small" @click="openPasswordDialog(row)">改密</el-button>
              <el-popconfirm
                :title="'确定' + (row.status === 1 ? '禁用' : '启用') + '该用户吗？'"
                confirm-button-text="确定"
                @confirm="toggleStatus(row)"
              >
                <template #reference>
                  <el-button :type="row.status === 1 ? 'warning' : 'success'" link size="small">
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm
                title="确定删除该用户吗？此操作不可恢复！"
                confirm-button-text="确定删除"
                cancel-button-text="取消"
                @confirm="deleteUser(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" width="480px" destroy-on-close class="user-detail-dialog" :show-close="false">
      <template #header>
        <div class="detail-banner" :style="{ background: bannerGradient(currentUser) }">
          <button class="detail-close" @click="detailVisible = false">&times;</button>
          <el-avatar :size="64" class="detail-avatar">
            {{ (currentUser?.username || '?').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="detail-name">{{ currentUser?.username || '-' }}</div>
          <div class="detail-meta">
            <el-tag :type="getUserTypeTag(currentUser?.userType)" size="small" effect="dark" round>{{ getUserTypeLabel(currentUser?.userType) }}</el-tag>
            <el-tag :type="currentUser?.status === 1 ? 'success' : 'danger'" size="small" effect="dark" round>
              {{ currentUser?.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </template>
      <div v-if="currentUser" class="user-detail">
        <div class="detail-section">
          <div class="detail-row">
            <div class="row-icon" style="color: #409eff;">&#128100;</div>
            <div class="row-content">
              <span class="row-label">用户名</span>
              <span class="row-value">{{ currentUser.username }}</span>
            </div>
          </div>
          <div class="detail-row">
            <div class="row-icon" style="color: #909399;">#</div>
            <div class="row-content">
              <span class="row-label">用户ID</span>
              <span class="row-value">{{ currentUser.id }}</span>
            </div>
          </div>
          <div class="detail-row">
            <div class="row-icon" style="color: #e6a23c;">&#9993;</div>
            <div class="row-content">
              <span class="row-label">邮箱</span>
              <span class="row-value">{{ currentUser.email || '-' }}</span>
            </div>
          </div>
          <div class="detail-row">
            <div class="row-icon" style="color: #67c23a;">&#9742;</div>
            <div class="row-content">
              <span class="row-label">手机号</span>
              <span class="row-value">{{ currentUser.phone || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-row">
            <div class="row-icon" style="color: #909399;">&#128197;</div>
            <div class="row-content">
              <span class="row-label">注册时间</span>
              <span class="row-value">{{ formatTime(currentUser.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
        <el-form-item label="用户名">
          <el-input :value="passwordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">确定修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.userType === 'ADMIN')

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const detailVisible = ref(false)
const currentUser = ref(null)

const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = ref({
  userId: null,
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const getUserTypeLabel = (type) => {
  const labels = { EMPLOYEE: '求职者', EMPLOYER: '企业', ADMIN: '管理员' }
  return labels[type] || type
}

const getUserTypeTag = (type) => {
  const tags = { EMPLOYEE: 'success', EMPLOYER: 'primary', ADMIN: 'danger' }
  return tags[type] || 'info'
}

const avatarColor = (user) => {
  if (!user) return '#909399'
  const colors = { EMPLOYEE: '#67c23a', EMPLOYER: '#409eff', ADMIN: '#f56c6c' }
  return colors[user.userType] || '#909399'
}

const bannerGradient = (user) => {
  if (!user) return 'linear-gradient(135deg, #909399, #b0b3b6)'
  const gradients = {
    EMPLOYEE: 'linear-gradient(135deg, #67c23a, #85ce61)',
    EMPLOYER: 'linear-gradient(135deg, #409eff, #66b1ff)',
    ADMIN: 'linear-gradient(135deg, #f56c6c, #f89898)'
  }
  return gradients[user.userType] || 'linear-gradient(135deg, #909399, #b0b3b6)'
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await request.get('/user/list', { params })
    users.value = res.records || []
    total.value = res.total || 0
  } catch {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const viewUser = (row) => {
  currentUser.value = row
  detailVisible.value = true
}

const openPasswordDialog = (row) => {
  passwordForm.value = {
    userId: row.id,
    username: row.username,
    newPassword: '',
    confirmPassword: ''
  }
  passwordDialogVisible.value = true
}

const submitPassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await request.put(`/user/${passwordForm.value.userId}/password`, {
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await request.put(`/user/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态已更新')
    fetchUsers()
  } catch {
    ElMessage.error('操作失败')
  }
}

const deleteUser = async (row) => {
  try {
    await request.delete(`/user/${row.id}`)
    ElMessage.success('用户已删除')
    fetchUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '删除失败')
  }
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
/* ── 用户详情弹窗 ─────────────────────────── */
.detail-banner {
  margin: -20px -20px 0;
  padding: var(--space-6) var(--space-5);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  position: relative;
}

.detail-close {
  position: absolute;
  top: 12px;
  right: 16px;
  background: rgba(255,255,255,0.25);
  border: none;
  color: #fff;
  font-size: 22px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.detail-close:hover { background: rgba(255,255,255,0.4); }

.detail-avatar {
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  background: rgba(255,255,255,0.25);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding-top: var(--space-2);
}

.detail-section {
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.detail-row:last-child { border-bottom: none; }

.row-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.row-content {
  display: flex;
  flex-direction: column;
}

.row-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.row-value {
  font-size: var(--text-sm);
  color: var(--gray-800);
  font-weight: 500;
}
</style>
