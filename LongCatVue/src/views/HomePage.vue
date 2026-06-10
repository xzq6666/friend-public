<template>
<div class="home-page">
  <!-- 顶部导航 -->
  <header class="home-header" :class="{ scrolled: isScrolled }">
    <div class="header-inner">
      <div class="logo-area">
        <div class="logo-mark">
          <div class="mark-dot"></div>
          <div class="mark-dot delay"></div>
        </div>
        <span class="logo-text">智能招聘匹配</span>
      </div>
      <nav class="header-nav" ref="navRef">
        <a v-for="item in navItems" :key="item.id" :href="'#' + item.id"
           class="nav-link" :class="{ active: activeSection === item.id }"
           @click.prevent="scrollToSection(item.id)">
          {{ item.label }}
        </a>
      </nav>
      <div class="header-actions">
        <template v-if="isLoggedIn">
          <el-button type="primary" @click="goDashboard">进入工作台</el-button>
        </template>
        <template v-else>
          <el-button @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">免费注册</el-button>
        </template>
      </div>
    </div>
  </header>

  <!-- Hero 区域 -->
  <section class="hero-section" id="hero">
    <div class="hero-bg">
      <div class="hero-orb orb-1"></div>
      <div class="hero-orb orb-2"></div>
      <div class="hero-orb orb-3"></div>
      <div class="hero-grid"></div>
    </div>
    <div class="hero-content" v-slide-in>
      <div class="hero-badge">
        <span class="badge-dot"></span>
        AI 驱动的智能招聘平台
      </div>
      <h1 class="hero-title">
        连接<span class="text-gradient">人才</span>与<span class="text-gradient">机会</span>
      </h1>
      <p class="hero-desc">
        基于人工智能的精准匹配算法，让求职者找到理想工作，让企业发现最合适的人才
      </p>
      <div class="hero-actions">
        <template v-if="isLoggedIn">
          <el-button type="primary" size="large" @click="goDashboard">
            进入工作台
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="large" @click="$router.push('/register')">
            免费注册
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <el-button size="large" @click="scrollToSection('features')">
          了解更多
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
      </div>
      <div class="hero-stats">
        <div class="stat-item" v-for="stat in stats" :key="stat.label">
          <span class="stat-value">{{ stat.displayValue }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>
    <div class="hero-scroll-hint" @click="scrollToSection('features')">
      <div class="scroll-mouse">
        <div class="scroll-wheel"></div>
      </div>
      <span>向下滚动探索</span>
    </div>
  </section>

  <!-- 功能特性 -->
  <section class="features-section" id="features">
    <div class="section-inner">
      <div class="section-header" v-slide-in>
        <span class="section-tag">核心功能</span>
        <h2 class="section-title">平台核心优势</h2>
        <p class="section-desc">为求职者和企业提供全方位的智能招聘服务</p>
      </div>

      <div class="features-grid">
        <div class="feature-card" v-for="(feature, index) in features" :key="feature.title"
             v-slide-in="{ delay: index * 100 }">
          <div class="feature-icon" :style="{ background: feature.bg }">
            <el-icon :size="24" :color="feature.color"><component :is="feature.icon" /></el-icon>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
          <div class="feature-card-glow" :style="{ background: feature.bg }"></div>
        </div>
      </div>
    </div>
  </section>

  <!-- 使用流程 -->
  <section class="process-section" id="process">
    <div class="section-inner">
      <div class="section-header" v-slide-in>
        <span class="section-tag">简单高效</span>
        <h2 class="section-title">三步开启智能招聘之旅</h2>
        <p class="section-desc">简洁流畅的操作流程，让招聘与求职变得轻松高效</p>
      </div>

      <div class="process-timeline">
        <div class="process-step" v-for="(step, index) in processSteps" :key="step.title"
             v-slide-in="{ delay: index * 150 }">
          <div class="step-connector" v-if="index < processSteps.length - 1">
            <div class="connector-line"></div>
            <el-icon class="connector-arrow" :size="16"><ArrowRight /></el-icon>
          </div>
          <div class="step-card">
            <div class="step-number">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="step-icon" :style="{ background: step.bg }">
              <el-icon :size="28" :color="step.color"><component :is="step.icon" /></el-icon>
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- 双端服务 -->
  <section class="audience-section" id="audience">
    <div class="section-inner">
      <div class="section-header" v-slide-in>
        <span class="section-tag">双端服务</span>
        <h2 class="section-title">为每一方量身定制</h2>
        <p class="section-desc">无论您是求职者还是企业，都能获得专属的智能服务体验</p>
      </div>

      <div class="audience-grid">
        <div class="audience-card seeker" v-slide-in="{ delay: 0 }">
          <div class="card-decoration"></div>
          <div class="audience-icon">
            <el-icon :size="32"><User /></el-icon>
          </div>
          <h3>求职者</h3>
          <ul>
            <li v-for="item in seekerFeatures" :key="item">
              <el-icon class="check-icon" :size="14"><Select /></el-icon>
              {{ item }}
            </li>
          </ul>
          <el-button type="primary" @click="isLoggedIn ? goDashboard() : $router.push('/register')">求职者注册</el-button>
        </div>
        <div class="audience-card employer" v-slide-in="{ delay: 150 }">
          <div class="card-decoration"></div>
          <div class="audience-icon">
            <el-icon :size="32"><OfficeBuilding /></el-icon>
          </div>
          <h3>企业</h3>
          <ul>
            <li v-for="item in employerFeatures" :key="item">
              <el-icon class="check-icon" :size="14"><Select /></el-icon>
              {{ item }}
            </li>
          </ul>
          <el-button type="primary" @click="isLoggedIn ? goDashboard() : $router.push('/register')">企业注册</el-button>
        </div>
      </div>
    </div>
  </section>

  <!-- CTA 行动号召 -->
  <section class="cta-section" id="cta">
    <div class="cta-bg">
      <div class="cta-orb cta-orb-1"></div>
      <div class="cta-orb cta-orb-2"></div>
    </div>
    <div class="cta-content" v-slide-in>
      <h2>准备好开启智能招聘之旅了吗？</h2>
      <p>立即注册，体验 AI 驱动的精准人岗匹配，让招聘与求职都变得更简单</p>
      <div class="cta-actions">
        <template v-if="isLoggedIn">
          <el-button type="primary" size="large" @click="goDashboard" class="cta-btn">
            进入工作台
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="large" @click="$router.push('/register')" class="cta-btn">
            立即免费注册
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" @click="$router.push('/login')" class="cta-btn-outline">
            已有账号？登录
          </el-button>
        </template>
      </div>
    </div>
  </section>

  <!-- 底部 -->
  <footer class="home-footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand">
          <div class="logo-mark">
            <div class="mark-dot"></div>
            <div class="mark-dot delay"></div>
          </div>
          <span class="footer-logo-text">智能招聘匹配平台</span>
          <p class="footer-slogan">AI 驱动，让招聘更智能</p>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <h4>平台服务</h4>
            <a @click.prevent="scrollToSection('features')">核心功能</a>
            <a @click.prevent="scrollToSection('process')">使用流程</a>
            <a @click.prevent="scrollToSection('audience')">双端服务</a>
          </div>
          <div class="footer-col">
            <h4>快速入口</h4>
            <a @click.prevent="$router.push('/login')">用户登录</a>
            <a @click.prevent="$router.push('/register')">免费注册</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p class="footer-copy">&copy; {{ new Date().getFullYear() }} Smart Recruitment. All rights reserved.</p>
      </div>
    </div>
  </footer>
</div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight, ArrowDown, User, OfficeBuilding, Search, Connection,
  ChatDotRound, DataLine, Bell, Trophy, Select, Upload, Promotion, TrendCharts
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const goDashboard = () => {
  const role = userStore.user?.userType
  if (role === 'ADMIN') {
    router.push('/admin/dashboard')
  } else {
    router.push('/dashboard')
  }
}

// ── 导航 ──
const navItems = [
  { id: 'features', label: '核心功能' },
  { id: 'process', label: '使用流程' },
  { id: 'audience', label: '双端服务' },
]

const activeSection = ref('')
const isScrolled = ref(false)
const navRef = ref(null)

// ── 数字动画 ──
const stats = ref([
  { label: '活跃职位', target: 1000, suffix: '+', displayValue: '0+' },
  { label: '合作企业', target: 500, suffix: '+', displayValue: '0+' },
  { label: '匹配满意度', target: 98, suffix: '%', displayValue: '0%' },
])

function animateCounters() {
  const duration = 2000
  const steps = 60
  const interval = duration / steps
  let currentStep = 0

  const timer = setInterval(() => {
    currentStep++
    const progress = currentStep / steps
    const eased = 1 - Math.pow(1 - progress, 3)

    stats.value.forEach(stat => {
      const current = Math.round(stat.target * eased)
      stat.displayValue = current + stat.suffix
    })

    if (currentStep >= steps) clearInterval(timer)
  }, interval)
}

// ── 滚动处理 ──
function scrollToSection(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 20

  // 高亮当前导航
  const sections = navItems.map(n => n.id)
  for (let i = sections.length - 1; i >= 0; i--) {
    const el = document.getElementById(sections[i])
    if (el && el.getBoundingClientRect().top <= 120) {
      activeSection.value = sections[i]
      return
    }
  }
  activeSection.value = ''
}

// ── 滚动动画指令 ──
const vSlideIn = {
  mounted(el, binding) {
    el.style.opacity = '0'
    el.style.transform = 'translateY(30px)'
    el.style.transition = `opacity 0.6s cubic-bezier(0.16,1,0.3,1) ${binding.value?.delay || 0}ms, transform 0.6s cubic-bezier(0.16,1,0.3,1) ${binding.value?.delay || 0}ms`

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.style.opacity = '1'
          el.style.transform = 'translateY(0)'
          observer.unobserve(el)
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    observer.observe(el)
  }
}

// ── 数据 ──
const features = [
  { icon: 'Connection', title: 'AI 智能匹配', desc: '基于深度学习的算法，精准分析求职者技能与职位需求，实现高效人岗匹配', color: '#4e6496', bg: 'rgba(78,100,150,0.1)' },
  { icon: 'Search', title: '精准搜索', desc: '多维度筛选条件，关键词、薪资、地点、学历等，快速定位目标', color: '#3ea15d', bg: 'rgba(62,161,93,0.1)' },
  { icon: 'ChatDotRound', title: '即时沟通', desc: '求职者与企业在线实时聊天，简历直传，高效沟通零距离', color: '#c08a2e', bg: 'rgba(192,138,46,0.1)' },
  { icon: 'DataLine', title: '数据洞察', desc: '可视化数据分析面板，投递统计、匹配分析、求职进度一目了然', color: '#c03939', bg: 'rgba(192,57,57,0.1)' },
  { icon: 'Bell', title: '智能通知', desc: '面试提醒、匹配推送、消息通知，重要信息不错过', color: '#78726a', bg: 'rgba(120,114,106,0.1)' },
  { icon: 'Trophy', title: '安全可靠', desc: '企业认证审核机制，数据加密存储，保障用户隐私与信息安全', color: '#b88230', bg: 'rgba(184,130,48,0.1)' },
]

const processSteps = [
  { icon: 'Upload', title: '注册并创建档案', desc: '快速注册账号，完善个人或企业信息，上传简历或发布职位', color: '#4e6496', bg: 'rgba(78,100,150,0.1)' },
  { icon: 'TrendCharts', title: 'AI 智能匹配', desc: '系统基于深度学习算法，自动分析技能与需求，推荐最匹配的人选或职位', color: '#3ea15d', bg: 'rgba(62,161,93,0.1)' },
  { icon: 'Promotion', title: '高效沟通入职', desc: '在线即时沟通、安排面试，快速达成合作意向，完成入职', color: '#c08a2e', bg: 'rgba(192,138,46,0.1)' },
]

const seekerFeatures = [
  'AI 智能职位推荐',
  '在线简历管理与优化',
  '投递进度实时追踪',
  '与企业在线即时沟通',
  '求职看板可视化管理',
]

const employerFeatures = [
  '精准人才匹配推荐',
  '职位发布与管理',
  '简历筛选与收藏',
  '面试安排与沟通',
  '人才市场主动搜索',
]

// ── 生命周期 ──
onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
  // 延迟启动数字动画，等页面加载完
  setTimeout(animateCounters, 500)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--color-surface);
  scroll-behavior: smooth;
}

/* ── 头部 ──────────────────────────────────── */
.home-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(251, 250, 248, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid transparent;
  transition: all 0.3s ease;
}
.home-header.scrolled {
  background: rgba(251, 250, 248, 0.92);
  border-bottom-color: var(--gray-100);
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo-area {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.logo-mark {
  display: flex;
  gap: 5px;
}
.mark-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gray-900);
  animation: dotPulse 2.5s ease-in-out infinite;
}
.mark-dot.delay { background: var(--gray-400); animation-delay: 0.3s; }
@keyframes dotPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}
.logo-text {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
}
.header-nav {
  display: flex;
  gap: var(--space-6);
}
.nav-link {
  font-size: var(--text-sm);
  color: var(--gray-500);
  text-decoration: none;
  font-weight: var(--weight-medium);
  position: relative;
  padding: 4px 0;
  transition: color 0.2s ease;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--gray-900);
  border-radius: 1px;
  transition: width 0.25s ease;
}
.nav-link:hover,
.nav-link.active {
  color: var(--gray-900);
}
.nav-link:hover::after,
.nav-link.active::after {
  width: 100%;
}
.header-actions {
  display: flex;
  gap: var(--space-2);
}

/* ── Hero ──────────────────────────────────── */
.hero-section {
  position: relative;
  padding: 160px var(--space-6) 100px;
  text-align: center;
  overflow: hidden;
  min-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, var(--gray-50) 0%, var(--color-surface) 100%);
}
.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.orb-1 {
  width: 500px;
  height: 500px;
  top: -100px;
  left: -100px;
  background: radial-gradient(circle, rgba(78,100,150,0.2) 0%, transparent 70%);
  animation: orbFloat 8s ease-in-out infinite;
}
.orb-2 {
  width: 400px;
  height: 400px;
  bottom: -50px;
  right: -80px;
  background: radial-gradient(circle, rgba(62,161,93,0.15) 0%, transparent 70%);
  animation: orbFloat 10s ease-in-out infinite reverse;
}
.orb-3 {
  width: 300px;
  height: 300px;
  top: 30%;
  right: 20%;
  background: radial-gradient(circle, rgba(192,138,46,0.1) 0%, transparent 70%);
  animation: orbFloat 12s ease-in-out infinite 2s;
}
@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0); }
  33% { transform: translate(20px, -30px); }
  66% { transform: translate(-15px, 20px); }
}
.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(26,24,20,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26,24,20,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
}
.hero-content {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: var(--gray-900);
  color: #fff;
  font-size: 12px;
  font-weight: var(--weight-medium);
  border-radius: var(--radius-full);
  margin-bottom: var(--space-6);
  letter-spacing: 0.05em;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
  animation: dotPulse 1.5s ease-in-out infinite;
}
.hero-title {
  font-size: 56px;
  font-weight: 800;
  color: var(--gray-900);
  margin: 0 0 var(--space-5);
  line-height: 1.15;
  letter-spacing: -0.03em;
}
.text-gradient {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-700) 50%, var(--success-500) 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradientShift 4s ease infinite;
}
@keyframes gradientShift {
  0%, 100% { background-position: 0% center; }
  50% { background-position: 100% center; }
}
.hero-desc {
  font-size: var(--text-lg);
  color: var(--gray-500);
  line-height: 1.8;
  margin: 0 0 var(--space-8);
  max-width: 560px;
  margin-left: auto;
  margin-right: auto;
}
.hero-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-10);
}
.hero-stats {
  display: flex;
  justify-content: center;
  gap: var(--space-10);
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--gray-900);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: var(--text-sm);
  color: var(--gray-400);
  margin-top: 4px;
}
.hero-scroll-hint {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  animation: hintBounce 2s ease-in-out infinite;
  z-index: 1;
}
.hero-scroll-hint span {
  font-size: var(--text-xs);
  color: var(--gray-400);
  letter-spacing: 0.05em;
}
.scroll-mouse {
  width: 24px;
  height: 36px;
  border: 2px solid var(--gray-300);
  border-radius: 12px;
  display: flex;
  justify-content: center;
  padding-top: 6px;
}
.scroll-wheel {
  width: 3px;
  height: 8px;
  background: var(--gray-400);
  border-radius: 2px;
  animation: scrollWheel 1.5s ease-in-out infinite;
}
@keyframes scrollWheel {
  0% { transform: translateY(0); opacity: 1; }
  100% { transform: translateY(8px); opacity: 0; }
}
@keyframes hintBounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-6px); }
}

/* ── 通用 Section ──────────────────────────── */
.section-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.section-header {
  text-align: center;
  margin-bottom: var(--space-10);
}
.section-tag {
  display: inline-block;
  padding: 4px 14px;
  background: var(--primary-50);
  color: var(--primary-600);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  border-radius: var(--radius-full);
  margin-bottom: var(--space-3);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.section-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--gray-900);
  margin: 0 0 var(--space-2);
  letter-spacing: -0.02em;
}
.section-desc {
  font-size: var(--text-base);
  color: var(--gray-500);
  margin: 0;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

/* ── 功能特性 ──────────────────────────────── */
.features-section {
  padding: 100px var(--space-6);
  background: var(--gray-50);
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}
.feature-card {
  position: relative;
  background: var(--color-surface);
  border: 1px solid var(--gray-100);
  border-radius: var(--radius-lg);
  padding: var(--space-7);
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
  overflow: hidden;
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  border-color: var(--gray-200);
}
.feature-card-glow {
  position: absolute;
  bottom: -40px;
  right: -40px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0;
  filter: blur(40px);
  transition: opacity 0.3s ease;
}
.feature-card:hover .feature-card-glow {
  opacity: 0.6;
}
.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-4);
}
.feature-card h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-2);
}
.feature-card p {
  font-size: var(--text-sm);
  color: var(--gray-500);
  line-height: 1.8;
  margin: 0;
}

/* ── 使用流程 ──────────────────────────────── */
.process-section {
  padding: 100px var(--space-6);
  background: var(--color-surface);
}
.process-timeline {
  display: flex;
  gap: var(--space-6);
  justify-content: center;
  align-items: stretch;
}
.process-step {
  flex: 1;
  max-width: 340px;
  position: relative;
}
.step-connector {
  position: absolute;
  top: 50%;
  right: calc(-1 * var(--space-3) - 8px);
  transform: translate(50%, -50%);
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 1;
}
.connector-line {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, var(--gray-200), var(--gray-300));
}
.connector-arrow {
  color: var(--gray-400);
}
.step-card {
  background: var(--gray-50);
  border: 1px solid var(--gray-100);
  border-radius: var(--radius-xl);
  padding: var(--space-7);
  text-align: center;
  height: 100%;
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
}
.step-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
  border-color: var(--gray-200);
  background: var(--color-surface);
}
.step-number {
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: var(--gray-300);
  letter-spacing: 0.1em;
  margin-bottom: var(--space-3);
}
.step-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-4);
}
.step-card h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-2);
}
.step-card p {
  font-size: var(--text-sm);
  color: var(--gray-500);
  line-height: 1.8;
  margin: 0;
}

/* ── 双端服务 ──────────────────────────────── */
.audience-section {
  padding: 100px var(--space-6);
  background: var(--gray-50);
}
.audience-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
}
.audience-card {
  position: relative;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  text-align: center;
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
  overflow: hidden;
  background: var(--color-surface);
}
.audience-card:hover {
  border-color: var(--gray-300);
  box-shadow: 0 12px 32px rgba(0,0,0,0.06);
  transform: translateY(-4px);
}
.card-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}
.seeker .card-decoration {
  background: linear-gradient(90deg, var(--primary-400), var(--primary-600));
}
.employer .card-decoration {
  background: linear-gradient(90deg, var(--success-500), var(--success-600));
}
.audience-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-5);
}
.seeker .audience-icon { background: var(--primary-50); color: var(--primary-600); }
.employer .audience-icon { background: var(--success-50); color: var(--success-600); }
.audience-card h3 {
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin: 0 0 var(--space-5);
}
.audience-card ul {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--space-6);
  text-align: left;
}
.audience-card li {
  padding: var(--space-2) 0;
  font-size: var(--text-sm);
  color: var(--gray-600);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.check-icon {
  flex-shrink: 0;
}
.seeker .check-icon { color: var(--primary-500); }
.employer .check-icon { color: var(--success-500); }

/* ── CTA ──────────────────────────────────── */
.cta-section {
  position: relative;
  padding: 100px var(--space-6);
  text-align: center;
  overflow: hidden;
  background: var(--gray-900);
}
.cta-bg {
  position: absolute;
  inset: 0;
}
.cta-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}
.cta-orb-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: 10%;
  background: radial-gradient(circle, rgba(78,100,150,0.3) 0%, transparent 70%);
  animation: orbFloat 8s ease-in-out infinite;
}
.cta-orb-2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  right: 15%;
  background: radial-gradient(circle, rgba(62,161,93,0.2) 0%, transparent 70%);
  animation: orbFloat 10s ease-in-out infinite reverse;
}
.cta-content {
  position: relative;
  max-width: 600px;
  margin: 0 auto;
}
.cta-content h2 {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 var(--space-4);
  letter-spacing: -0.02em;
}
.cta-content p {
  font-size: var(--text-base);
  color: var(--gray-400);
  line-height: 1.8;
  margin: 0 0 var(--space-8);
}
.cta-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
}
.cta-btn {
  background: #fff !important;
  border-color: #fff !important;
  color: var(--gray-900) !important;
  font-weight: var(--weight-semibold) !important;
  padding: 12px 28px !important;
}
.cta-btn:hover {
  background: var(--gray-100) !important;
  border-color: var(--gray-100) !important;
}
.cta-btn-outline {
  background: transparent !important;
  border-color: var(--gray-600) !important;
  color: var(--gray-300) !important;
  font-weight: var(--weight-medium) !important;
}
.cta-btn-outline:hover {
  border-color: var(--gray-400) !important;
  color: #fff !important;
}

/* ── 底部 ──────────────────────────────────── */
.home-footer {
  padding: var(--space-10) var(--space-6) var(--space-6);
  border-top: 1px solid var(--gray-100);
  background: var(--color-surface);
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
}
.footer-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-8);
  gap: var(--space-8);
}
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.footer-brand .logo-mark {
  margin-bottom: 0;
}
.footer-logo-text {
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  font-size: var(--text-base);
}
.footer-slogan {
  font-size: var(--text-sm);
  color: var(--gray-400);
  margin: 0;
}
.footer-links {
  display: flex;
  gap: var(--space-10);
}
.footer-col {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.footer-col h4 {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--gray-700);
  margin: 0 0 var(--space-1);
}
.footer-col a {
  font-size: var(--text-sm);
  color: var(--gray-400);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s ease;
}
.footer-col a:hover {
  color: var(--gray-700);
}
.footer-bottom {
  border-top: 1px solid var(--gray-100);
  padding-top: var(--space-5);
  text-align: center;
}
.footer-copy {
  font-size: var(--text-sm);
  color: var(--gray-400);
  margin: 0;
}

/* ── 响应式 ────────────────────────────────── */
@media (max-width: 1024px) {
  .header-nav { display: none; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .process-timeline { flex-direction: column; align-items: center; }
  .step-connector { display: none; }
  .process-step { max-width: 100%; width: 100%; }
}
@media (max-width: 768px) {
  .hero-title { font-size: 36px; }
  .hero-section { padding: 120px var(--space-5) 80px; min-height: auto; }
  .hero-stats { gap: var(--space-5); }
  .stat-value { font-size: 24px; }
  .hero-scroll-hint { display: none; }
  .features-grid { grid-template-columns: 1fr; }
  .audience-grid { grid-template-columns: 1fr; }
  .hero-actions { flex-direction: column; align-items: center; }
  .cta-content h2 { font-size: 24px; }
  .cta-actions { flex-direction: column; align-items: center; }
  .footer-top { flex-direction: column; }
  .footer-links { flex-direction: column; gap: var(--space-5); }
  .section-title { font-size: 24px; }
  .features-section,
  .process-section,
  .audience-section,
  .cta-section { padding: 60px var(--space-5); }
}
</style>
