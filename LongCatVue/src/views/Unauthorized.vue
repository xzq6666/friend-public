<template>
<div class="unauthorized-page">
  <div class="unauthorized-card">
    <div class="error-icon"><el-icon size="48"><Lock /></el-icon></div>
    <h1 class="error-title">访问被拒绝</h1>
    <p class="error-message">抱歉，您没有权限访问此页面。<br/>如果您认为这是一个错误，请联系系统管理员。</p>
    
    <!-- 诊断信息 -->
    <div v-if="debugInfo" class="debug-info">
      <h3>权限诊断信息</h3>
      <div class="debug-item">
        <span class="debug-label">当前角色：</span>
        <span :class="['debug-value', userStore.userRole ? 'valid' : 'invalid']">
          {{ userStore.userRole || '未检测到（请重新登录）' }}
        </span>
      </div>
      <div class="debug-item">
        <span class="debug-label">来源页面：</span>
        <span class="debug-value">{{ fromPath || '未知' }}</span>
      </div>
      <el-alert v-if="!userStore.userRole" type="warning" :closable="false" show-icon style="margin-top: 12px;">
        <template #title>用户角色为空</template>
        可能是登录态已过期或用户信息未正确加载，建议重新登录。
      </el-alert>
    </div>
    
    <div class="error-actions">
      <el-button class="btn-primary" @click="goHome">返回首页</el-button>
      <el-button @click="goBack">返回上一页</el-button>
      <el-button v-if="!userStore.userRole" type="warning" @click="handleRelogin">重新登录</el-button>
    </div>
  </div>
</div></template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const debugInfo = ref(true)
const fromPath = ref(route.query.from || document.referrer || '未知')

const goHome = () => {
  const userRole = userStore.user?.userType
  if (userRole === 'ADMIN') {
    router.push('/admin/dashboard')
  } else {
    router.push('/dashboard')
  }
}

const goBack = () => {
  router.back()
}

const handleRelogin = () => {
  userStore.logout(false)
  router.push('/login')
}

onMounted(() => {
  console.warn('[Unauthorized] 权限拦截诊断:', {
    userType: userStore.user?.userType,
    user: userStore.user,
    token: localStorage.getItem('token') ? '存在' : '不存在',
    fromPath: fromPath.value
  })
})
</script>

<style scoped>
.unauthorized-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-5);
  background: var(--gray-50);
}

.unauthorized-card {
  text-align: center;
  max-width: 500px;
  background: var(--color-surface);
  padding: var(--space-10) var(--space-8);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
}

.error-icon {
  color: var(--gray-400);
  margin-bottom: var(--space-5);
}

.error-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0 0 var(--space-3);
}

.error-message {
  font-size: 14px;
  color: var(--gray-500);
  line-height: 1.8;
  margin: 0 0 var(--space-6);
}

.error-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary {
  background: var(--gray-900);
  border-color: var(--gray-900);
  color: var(--color-surface);
}

.btn-primary:hover {
  background: var(--gray-800);
  border-color: var(--gray-800);
}

.debug-info {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px dashed var(--gray-300);
  text-align: left;
}

.debug-info h3 {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-700);
  margin: 0 0 var(--space-3);
}

.debug-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-size: 12px;
}

.debug-label {
  color: var(--gray-500);
  white-space: nowrap;
}

.debug-value {
  color: var(--gray-700);
  font-weight: 500;
  font-family: monospace;
}

.debug-value.valid {
  color: var(--score-excellent);
}

.debug-value.invalid {
  color: var(--score-poor);
}
</style>