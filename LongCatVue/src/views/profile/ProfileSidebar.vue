<template>
<div class="profile-card">
  <div class="profile-identity">
    <div class="avatar-wrapper">
      <div class="avatar-letter">
        {{ data.user.value?.username?.charAt(0)?.toUpperCase() || 'A' }}
      </div>
    </div>
    <div class="identity-info">
      <h3>{{ data.user.value?.username || '-' }}</h3>
      <span class="role-tag">{{ data.getRoleName(data.user.value?.userType) }}</span>
    </div>
  </div>
  <div class="info-rows">
    <div class="info-row">
      <el-icon class="info-icon"><User /></el-icon>
      <span class="info-label">用户名</span>
      <span class="info-value">{{ data.user.value?.username || '-' }}</span>
    </div>
    <div class="info-row">
      <el-icon class="info-icon"><Message /></el-icon>
      <span class="info-label">邮箱</span>
      <span class="info-value">{{ data.user.value?.email || '未设置' }}</span>
    </div>
    <div class="info-row">
      <el-icon class="info-icon"><Phone /></el-icon>
      <span class="info-label">手机号</span>
      <span class="info-value">{{ data.user.value?.phone || '未设置' }}</span>
    </div>
    <div class="info-row">
      <el-icon class="info-icon"><Calendar /></el-icon>
      <span class="info-label">注册时间</span>
      <span class="info-value">{{ data.formatTime(data.user.value?.createTime) }}</span>
    </div>
  </div>
  <div class="profile-actions">
    <el-button class="btn-primary" @click="data.showEditDialog"><el-icon><Edit /></el-icon> 编辑资料</el-button>
    <el-button @click="data.showPasswordDialog"><el-icon><Lock /></el-icon> 修改密码</el-button>
  </div>

  <!-- 编辑资料弹窗 -->
  <el-dialog v-model="data.editDialogVisible.value" title="编辑个人资料" width="500px">
    <el-form ref="editFormRef" :model="data.editForm" :rules="data.editRules" label-width="80px">
      <el-form-item label="用户名"><el-input :value="data.user.value?.username" disabled /></el-form-item>
      <el-form-item label="邮箱" prop="email"><el-input v-model="data.editForm.email" placeholder="请输入邮箱地址" /></el-form-item>
      <el-form-item label="手机号" prop="phone"><el-input v-model="data.editForm.phone" placeholder="请输入手机号" maxlength="11" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="data.editDialogVisible.value = false">取消</el-button>
      <el-button class="btn-primary" @click="onUpdateProfile" :loading="data.updating.value">保存修改</el-button>
    </template>
  </el-dialog>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="data.passwordDialogVisible.value" title="修改密码" width="500px">
    <el-form ref="passwordFormRef" :model="data.passwordForm" :rules="data.passwordRules" label-width="100px">
      <el-form-item label="原密码" prop="oldPassword"><el-input v-model="data.passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="data.passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password /></el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="data.passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="data.passwordDialogVisible.value = false">取消</el-button>
      <el-button class="btn-primary" @click="onChangePassword" :loading="data.changingPassword.value">确认修改</el-button>
    </template>
  </el-dialog>
</div>
</template>

<script setup>
import { ref, inject, watch } from 'vue'
import { Edit, Lock, User, Message, Phone, Calendar } from '@element-plus/icons-vue'

const data = inject('profileData')

const editFormRef = ref()
const passwordFormRef = ref()

watch(editFormRef, (el) => { data.editFormRef.value = el }, { immediate: true })
watch(passwordFormRef, (el) => { data.passwordFormRef.value = el }, { immediate: true })

const onUpdateProfile = () => { data.updateProfile() }
const onChangePassword = () => { data.changePassword() }
</script>

<style scoped>
.profile-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}
.profile-identity {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--gray-100);
}
.avatar-wrapper { flex-shrink: 0; }
.avatar-letter {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--gray-900);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: var(--weight-semibold);
}
.identity-info h3 {
  font-size: 18px;
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-1);
}
.role-tag { font-size: 13px; color: var(--gray-500); }

.info-rows { margin-bottom: var(--space-5); }
.info-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--gray-100);
}
.info-row:last-child { border-bottom: none; }
.info-icon { font-size: var(--text-base); color: var(--gray-400); flex-shrink: 0; }
.info-label { width: 70px; font-size: 13px; color: var(--gray-500); flex-shrink: 0; }
.info-value { flex: 1; font-size: 13px; color: var(--gray-900); }

.profile-actions { display: flex; gap: var(--space-3); }

.btn-primary {
  background: var(--gray-900);
  border-color: var(--gray-900);
  color: var(--color-surface);
}
.btn-primary:hover {
  background: var(--gray-800);
  border-color: var(--gray-800);
}
</style>
