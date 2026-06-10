<template>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <GlobalLoading />
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import GlobalLoading from './components/GlobalLoading.vue'
import { useNotificationStore } from './stores/notification'
import { useUserStore } from './stores/user'

// 全局启动通知轮询（确保所有页面都能收到通知）
const notificationStore = useNotificationStore()
const userStore = useUserStore()

// 监听用户登录状态，只有登录后才启动通知轮询
watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    console.log('[App] 用户已登录，启动通知轮询')
    notificationStore.startPolling()
  } else {
    console.log('[App] 用户未登录/已登出，停止通知轮询')
    notificationStore.stopPolling()
  }
}, { immediate: true })

onUnmounted(() => {
  notificationStore.stopPolling()
})
</script>

<style>
/* 引入现代化主题样式 */
@import './assets/theme.css';

/* 引入页面增强样式 */
@import './assets/page-enhancements.css';

/* 引入管理后台增强样式 */
@import './assets/admin-enhancements.css';

/* 引入全局布局工具类 */
@import './assets/styles/layout-utilities.css';

/* 页面切换动效 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 150ms var(--ease-out);
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
}
</style>
