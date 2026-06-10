<template>
  <div class="login-page">
    <!-- 背景 -->
    <div class="bg-gradient"></div>
    <canvas class="particle-canvas" ref="canvasRef"></canvas>

    <!-- 主卡片 -->
    <div class="login-inner animate-enter">
      <!-- 左侧品牌区 -->
      <div class="login-brand animate-slide-left">
        <router-link to="/" class="brand-logo">
          <div class="brand-mark">
            <div class="mark-dot"></div>
            <div class="mark-dot delay"></div>
          </div>
          <span class="brand-name">智能招聘匹配</span>
        </router-link>

        <h1><span class="title-gradient">连接人才与机会</span></h1>
        <p class="brand-desc">基于 AI 的智能招聘平台，让每一次匹配都更精准</p>

        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
              </svg>
            </div>
            <div class="feature-text">
              <strong>智能匹配</strong>
              <span>AI 算法精准推荐</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <div class="feature-text">
              <strong>简历解析</strong>
              <span>自动提取关键信息</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <div class="feature-text">
              <strong>全流程管理</strong>
              <span>从投递到录用</span>
            </div>
          </div>
        </div>

        <div class="brand-stats">
          <div class="stat-card">
            <div class="stat-num">{{ jobCount }}+</div>
            <div class="stat-text">活跃职位</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ userCount }}+</div>
            <div class="stat-text">注册用户</div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="login-form-area animate-slide-right">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录以继续您的招聘之旅</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin" class="login-form">
          <div class="form-field">
            <label class="field-label">用户名</label>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large" class="field-input" />
          </div>
          <div class="form-field">
            <label class="field-label">密码</label>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password class="field-input" />
          </div>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ErrorHandler } from '../utils/errorHandler'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const jobCount = ref(100)
const userCount = ref(500)
const canvasRef = ref(null)
let animationId = null

const form = reactive({ username: '', password: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const success = await userStore.login({ ...form, remember: false })
    if (success) {
      ErrorHandler.success('登录成功', 'login')
      const userType = userStore.user?.userType
      router.push(userType === 'ADMIN' ? '/admin/dashboard' : '/dashboard')
    } else {
      ErrorHandler.handle(new Error('用户名或密码错误'), 'form')
    }
  } catch (error) {
    ErrorHandler.handle(error, 'login')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await request.get('/auth/stats', { skipErrorNotification: true })
    jobCount.value = res.jobCount || 100
    userCount.value = res.userCount || 500
  } catch (e) {
    console.error('获取统计数据失败:', e)
  }
  initParticleCanvas()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
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
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gray-50);
  padding: var(--space-10) var(--space-5);
  position: relative;
  overflow: hidden;
}

/* ── 背景 ─────────────────────────────────── */
.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(78, 100, 150, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(61, 80, 120, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 40% 20%, rgba(92, 112, 160, 0.04) 0%, transparent 50%);
  z-index: 0;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

/* ── 主卡片 ───────────────────────────────── */
.login-inner {
  display: flex;
  width: 920px;
  min-height: 540px;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl), var(--shadow-md);
  position: relative;
  z-index: 2;
}

/* ── 左侧品牌区 ────────────────────────────── */
.login-brand {
  width: 400px;
  flex-shrink: 0;
  background: linear-gradient(160deg, var(--gray-900) 0%, var(--gray-800) 60%, #1e2a3a 100%);
  color: var(--color-surface);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--space-12) var(--space-10);
  position: relative;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.02'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.6;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--color-surface);
  margin-bottom: var(--space-9);
  position: relative;
}

.brand-mark {
  display: flex;
  gap: 5px;
}

.mark-dot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.9);
}
.mark-dot.delay {
  background: rgba(255, 255, 255, 0.35);
}

.brand-name {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  letter-spacing: 0.02em;
}

.title-gradient {
  background: linear-gradient(135deg, var(--color-surface) 0%, rgba(255, 255, 255, 0.75) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-brand h1 {
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  letter-spacing: -0.02em;
  margin-bottom: var(--space-3);
  line-height: var(--leading-tight);
  position: relative;
}

.brand-desc {
  font-size: var(--text-base);
  color: rgba(255, 255, 255, 0.5);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-8);
  position: relative;
}

/* 特性列表 */
.brand-features {
  display: flex;
  flex-direction: column);
  gap: 14px;
  margin-bottom: var(--space-8);
  position: relative;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.feature-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.07);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.65);
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-out);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.feature-item:hover .feature-icon {
  background: rgba(255, 255, 255, 0.12);
  color: var(--color-surface);
  transform: scale(1.05);
}

.feature-text {
  display: flex;
  flex-direction: column;
}

.feature-text strong {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  margin-bottom: 1px;
}

.feature-text span {
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.4);
}

/* 统计 */
.brand-stats {
  display: flex;
  gap: var(--space-3);
  padding-top: var(--space-6);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
}

.stat-card {
  flex: 1;
  padding: 14px var(--space-4);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  transition: all var(--duration-normal) var(--ease-out);
  cursor: default;
}

.stat-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.stat-num {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--color-surface);
}

.stat-text {
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.4);
  margin-top: var(--space-1);
}

/* ── 右侧表单区 ────────────────────────────── */
.login-form-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--space-12) var(--space-11);
  background: var(--color-surface);
}

.form-header {
  margin-bottom: var(--space-8);
}

.form-header h2 {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--gray-900);
  margin-bottom: var(--space-2);
}

.form-header p {
  font-size: var(--text-base);
  color: var(--gray-500);
}

/* ── 表单 ─────────────────────────────────── */
.login-form {
  margin-bottom: var(--space-6);
}

.form-field {
  margin-bottom: var(--space-5);
}

.field-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-700);
  margin-bottom: var(--space-2);
}

.field-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  padding: 11px 14px;
  box-shadow: 0 0 0 1px var(--gray-200) inset;
  transition: all var(--duration-fast) var(--ease-out);
}

.field-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--gray-300) inset;
}

.field-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--gray-900) inset;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-top: var(--space-2);
  border-radius: var(--radius-md);
  letter-spacing: 0.04em;
  transition: all var(--duration-normal) var(--ease-out);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* ── 页脚 ─────────────────────────────────── */
.form-footer {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--gray-500);
}

.form-footer a {
  color: var(--gray-900);
  font-weight: var(--weight-semibold);
  margin-left: var(--space-1);
  transition: color var(--duration-fast) var(--ease-out);
}

.form-footer a:hover {
  color: var(--primary-600);
}

/* ── 动画 ─────────────────────────────────── */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(var(--space-6)); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-32px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(32px); }
  to { opacity: 1; transform: translateX(0); }
}

.animate-enter {
  animation: fadeInUp 0.5s var(--ease-out) forwards;
}

.animate-slide-left {
  animation: slideInLeft 0.6s var(--ease-out) forwards;
  opacity: 0;
}

.animate-slide-right {
  animation: slideInRight 0.6s var(--ease-out) forwards;
  opacity: 0;
}

/* ── 响应式 ───────────────────────────────── */
@media (max-width: 768px) {
  .login-inner {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }
  .login-brand {
    width: 100%;
    padding: var(--space-8) 28px;
  }
  .brand-features,
  .brand-stats {
    display: none;
  }
  .login-form-area {
    padding: var(--space-8) 28px;
  }
}
</style>
