<template>
  <div class="register-page">
    <!-- 背景 -->
    <div class="bg-gradient"></div>
    <canvas class="particle-canvas" ref="canvasRef"></canvas>
    <div class="floating-particles">
      <div class="particle" v-for="i in 20" :key="i" :style="{ left: Math.random() * 100 + '%', animationDelay: Math.random() * 10 + 's', animationDuration: (10 + Math.random() * 15) + 's' }"></div>
    </div>

    <div class="register-inner animate-enter">
      <!-- 左侧：表单 -->
      <div class="register-form-area animate-slide-left">
        <div class="form-header">
          <router-link to="/login" class="brand-logo">
            <div class="brand-mark">
              <div class="mark-dot"></div>
              <div class="mark-dot delay"></div>
            </div>
            <span class="brand-name">智能招聘匹配</span>
          </router-link>
          <h2>创建账户</h2>
          <p>注册以使用智能招聘管理系统</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
          class="register-form"
        >
          <div class="form-row">
            <div class="form-field">
              <label class="field-label">用户名</label>
              <el-input v-model="form.username" placeholder="请输入用户名" maxlength="20" />
            </div>
            <div class="form-field">
              <label class="field-label">手机号</label>
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </div>
          </div>

          <div class="form-field">
            <label class="field-label">邮箱</label>
            <el-input v-model="form.email" placeholder="请输入邮箱地址" maxlength="50" />
          </div>

          <div class="form-row">
            <div class="form-field">
              <label class="field-label">密码</label>
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password maxlength="30" />
            </div>
            <div class="form-field">
              <label class="field-label">确认密码</label>
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password maxlength="30" />
            </div>
          </div>

          <div class="form-field">
            <label class="field-label">账户类型</label>
            <div class="role-selector">
              <button
                type="button"
                class="role-btn"
                :class="{ active: form.userType === 'EMPLOYEE' }"
                @click="form.userType = 'EMPLOYEE'"
              >
                <span class="role-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                </span>
                <span class="role-name">求职者</span>
                <span class="role-hint">寻找工作机会</span>
              </button>
              <button
                type="button"
                class="role-btn"
                :class="{ active: form.userType === 'EMPLOYER' }"
                @click="form.userType = 'EMPLOYER'"
              >
                <span class="role-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
                </span>
                <span class="role-name">企业招聘</span>
                <span class="role-hint">发布职位招人才</span>
              </button>
            </div>
          </div>

          <div class="form-agreement">
            <el-checkbox v-model="form.agreement" size="small">
              我已阅读并同意 <a href="#">用户协议</a> 和 <a href="#">隐私政策</a>
            </el-checkbox>
          </div>

          <el-button type="primary" size="large" class="submit-btn" :loading="loading" native-type="submit">
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>

          <div class="form-footer">
            已有账户？<router-link to="/login">立即登录</router-link>
          </div>
        </el-form>
      </div>

      <!-- 右侧：信息区 -->
      <div class="register-info animate-slide-right">
        <div class="info-content">
          <h3>为什么选择我们？</h3>
          <div class="info-list">
            <div class="info-item">
              <div class="info-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              </div>
              <div>
                <strong>高效匹配</strong>
                <span>AI 算法精准推荐，节省 70% 筛选时间</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              </div>
              <div>
                <strong>简历智能解析</strong>
                <span>自动提取关键信息，生成专业评估</span>
              </div>
            </div>
            <div class="info-item">
              <div class="info-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </div>
              <div>
                <strong>全流程管理</strong>
                <span>从投递到录用，一站式跟踪管理</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const canvasRef = ref(null)
let animationId = null

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  userType: 'EMPLOYEE',
  agreement: false
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  agreement: [{ required: true, message: '请阅读并同意用户协议', trigger: 'change' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const success = await userStore.register(form)
    if (success) {
      ElMessage.success('注册成功，即将跳转登录')
      setTimeout(() => router.push('/login'), 1500)
    } else {
      ElMessage.error('注册失败，用户名可能已存在')
    }
  } catch {
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initParticleCanvas()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})

const initParticleCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = []
  const particleCount = 40
  const connectionDistance = 150

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      radius: Math.random() * 2 + 1
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < connectionDistance) {
          const opacity = (1 - distance / connectionDistance) * 0.12
          ctx.beginPath()
          ctx.strokeStyle = `rgba(78, 100, 150, ${opacity})`
          ctx.lineWidth = 1
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }

    particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > canvas.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas.height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(78, 100, 150, 0.25)'
      ctx.fill()
    })

    animationId = requestAnimationFrame(animate)
  }

  animate()

  window.addEventListener('resize', () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gray-50);
  padding: var(--space-8) var(--space-5);
  position: relative;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(78, 100, 150, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(61, 80, 120, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 40% 20%, rgba(92, 158, 120, 0.04) 0%, transparent 50%);
  z-index: 0;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.floating-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 2;
  overflow: hidden;
}

.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: var(--primary-400);
  border-radius: var(--radius-full);
  bottom: -10px;
  animation: floatUp linear infinite;
  opacity: 0.3;
}

.particle:nth-child(3n) { width: 8px; height: 8px; opacity: 0.2; }
.particle:nth-child(4n) { width: 4px; height: 4px; opacity: 0.5; }

@keyframes floatUp {
  0% { transform: translateY(0) rotate(0deg) scale(1); opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.5; }
  100% { transform: translateY(-100vh) rotate(720deg) scale(0.5); opacity: 0; }
}

/* ── 动画 ─────────────────────────────────── */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-40px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes slideInRight {
  from { opacity: 0; transform: translateX(40px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes logoFadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes dotPulse {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.3); opacity: 1; }
}

.animate-enter {
  animation: fadeInUp 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
.animate-slide-left {
  animation: slideInLeft 0.7s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  opacity: 0;
}
.animate-slide-right {
  animation: slideInRight 0.7s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  opacity: 0;
}

/* ── 卡片容器 ─────────────────────────────── */
.register-inner {
  display: flex;
  width: 860px;
  min-height: 580px;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
  position: relative;
  z-index: 3;
}

/* ── 左侧表单区 ────────────────────────────── */
.register-form-area {
  flex: 1;
  padding: var(--space-8) 44px;
  overflow-y: auto;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  margin-bottom: var(--space-6);
  opacity: 0;
  animation: logoFadeIn 0.5s ease-out 0.1s forwards;
}

.brand-mark {
  display: flex;
  gap: var(--space-1);
}

.mark-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--gray-900);
  animation: dotPulse 2s ease-in-out infinite;
}
.mark-dot.delay {
  background: var(--gray-400);
  animation-delay: 0.5s;
}

.brand-name {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}

.form-header h2 {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin-bottom: var(--space-1);
  opacity: 0;
  animation: slideInLeft 0.6s ease-out 0.2s forwards;
}

.form-header p {
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin-bottom: var(--space-7);
  opacity: 0;
  animation: fadeInUp 0.6s ease-out 0.3s forwards;
}

/* ── 表单 ─────────────────────────────────── */
.register-form {
  max-width: 420px;
}

.form-row {
  display: flex;
  gap: var(--space-4);
}

.form-row .form-field {
  flex: 1;
}

.form-field {
  margin-bottom: var(--space-5);
}

.field-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-600);
  margin-bottom: var(--space-1);
}

.register-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  box-shadow: 0 0 0 1px var(--gray-200) inset;
  transition: all var(--duration-fast) var(--ease-out);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--gray-500) inset;
}

/* 角色选择器 */
.role-selector {
  display: flex;
  gap: var(--space-3);
}

.role-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-4) var(--space-3);
  border: 1.5px solid var(--gray-200);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.role-btn:hover {
  border-color: var(--gray-400);
  background: var(--gray-50);
}

.role-btn.active {
  border-color: var(--gray-900);
  background: var(--gray-50);
  box-shadow: 0 0 0 1px var(--gray-900) inset;
}

.role-icon {
  color: var(--gray-500);
  transition: all var(--duration-fast) var(--ease-out);
}

.role-btn.active .role-icon {
  color: var(--gray-900);
}

.role-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-800);
}

.role-hint {
  font-size: var(--text-xs);
  color: var(--gray-400);
}

/* 协议 */
.form-agreement {
  margin-bottom: var(--space-5);
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.form-agreement a {
  color: var(--gray-700);
}

/* 按钮 */
.submit-btn {
  width: 100%;
  height: 42px;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.form-footer {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--gray-500);
  margin-top: var(--space-4);
}

.form-footer a {
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
}

/* ── 右侧信息区 ────────────────────────────── */
.register-info {
  width: 300px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--gray-900) 0%, var(--gray-800) 100%);
  color: var(--color-surface);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--space-8) var(--space-7);
  position: relative;
}

.register-info::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.info-content {
  position: relative;
  z-index: 1;
}

.info-content h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-7);
  color: rgba(255, 255, 255, 0.9);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.info-item {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.6);
}

.info-item strong {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-1);
}

.info-item span {
  font-size: var(--text-sm);
  color: rgba(255, 255, 255, 0.45);
  line-height: var(--leading-normal);
}

/* ── 响应式 ───────────────────────────────── */
@media (max-width: 768px) {
  .register-inner {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }
  .register-info {
    display: none;
  }
  .register-form-area {
    padding: var(--space-7) var(--space-6);
  }
  .form-row {
    flex-direction: column;
    gap: 0;
  }
  .role-selector {
    flex-direction: column;
  }
}
</style>
