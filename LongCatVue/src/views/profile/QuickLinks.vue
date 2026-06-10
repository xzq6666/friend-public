<template>
<div class="content-card">
  <div class="card-title">快捷入口</div>
  <div class="quick-links">
    <router-link
      v-for="link in quickLinks"
      :key="link.to"
      :to="link.to"
      class="quick-link"
    >
      <el-icon><component :is="link.icon" /></el-icon>
      <span class="link-title">{{ link.title }}</span>
      <span class="link-desc">{{ link.desc }}</span>
    </router-link>
  </div>
</div>
</template>

<script setup>
import { computed, inject } from 'vue'
import { Document, Search, Connection, Briefcase, User, Star } from '@element-plus/icons-vue'

const data = inject('profileData')

const quickLinks = computed(() => {
  const links = []
  if (data.isEmployee.value) {
    links.push(
      { to: '/my-resume', icon: Document, title: '我的简历', desc: '管理您的求职简历' },
      { to: '/browse-jobs', icon: Search, title: '浏览职位', desc: '发现匹配职位' },
      { to: '/match', icon: Connection, title: '智能匹配', desc: 'AI 推荐最适合的职位' },
    )
  }
  if (data.isEmployer.value) {
    links.push(
      { to: '/my-jobs', icon: Briefcase, title: '职位管理', desc: '发布和管理招聘职位' },
      { to: '/my-candidates', icon: User, title: '候选人', desc: '查看收到的简历' },
    )
  }
  links.push({ to: '/favorites', icon: Star, title: '我的收藏', desc: '查看收藏的职位和简历' })
  return links
})
</script>

<style scoped>
.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}
.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--gray-100);
}
.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-3);
}
.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  text-decoration: none;
  color: var(--gray-700);
  transition: all var(--duration-fast) var(--ease-out);
}
.quick-link:hover {
  border-color: var(--gray-300);
  background: var(--gray-50);
  color: var(--gray-900);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.quick-link .el-icon {
  font-size: var(--text-xl);
  color: var(--gray-500);
  transition: color var(--duration-fast) var(--ease-out);
}
.quick-link:hover .el-icon { color: var(--gray-700); }
.link-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}
.link-desc {
  font-size: var(--text-xs);
  color: var(--gray-400);
  text-align: center;
  line-height: 1.4;
}
</style>
