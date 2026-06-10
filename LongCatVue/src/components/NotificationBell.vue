<template>
  <div class="notification-bell" ref="bellRef">
    <el-dropdown
      ref="dropdownRef"
      trigger="click"
      placement="bottom-end"
      :popper-class="`notification-dropdown ${dropdownVisible ? 'is-visible' : ''}`"
      @visible-change="handleVisibleChange"
    >
      <div class="bell-trigger" :class="{ active: dropdownVisible }">
        <el-badge :value="unreadCount || 0" :hidden="!unreadCount || unreadCount === 0" :max="99" class="bell-badge">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </div>
      <template #dropdown>
        <NotificationPanel
          :notifications="sortedNotifications"
          :loading="loading"
          @read="handleRead"
          @read-all="handleReadAll"
          @close="handleClose"
          @delete="handleDelete"
        />
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useNotificationStore } from '../stores/notification'
import NotificationPanel from './NotificationPanel.vue'

const store = useNotificationStore()
const dropdownVisible = ref(false)
const bellRef = ref(null)
const dropdownRef = ref(null)

const unreadCount = computed(() => store.unreadCount)
const loading = computed(() => store.loading)
const sortedNotifications = computed(() => store.sortedNotifications)

// 监听未读数变化（用于调试）
watch(unreadCount, (newVal, oldVal) => {
  console.log(`[NotificationBell] 未读数变化: ${oldVal} → ${newVal}`)
})

const handleVisibleChange = (visible) => {
  dropdownVisible.value = visible
  if (visible) {
    // 打开下拉时同时刷新未读数和列表
    store.fetchUnreadCount()
    store.fetchNotifications()
  }
}

const handleRead = (id) => {
  store.readNotification(id)
}

const handleReadAll = () => {
  store.readAll()
}

const handleClose = () => {
  dropdownRef.value?.handleClose()
}

const handleDelete = (id) => {
  store.removeNotification(id)
}

onMounted(() => {
  console.log('[NotificationBell] 组件挂载，启动轮询')
  store.startPolling()
})

onUnmounted(() => {
  console.log('[NotificationBell] 组件卸载，停止轮询')
  store.stopPolling()
})
</script>

<style scoped>
.notification-bell {
  display: flex;
  align-items: center;
}

.bell-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  color: var(--gray-500);
}

.bell-trigger:hover,
.bell-trigger.active {
  background: var(--gray-50);
  color: var(--gray-800);
}

.bell-badge :deep(.el-badge__content) {
  border: none;
  font-size: 10px;
  height: 16px;
  line-height: 16px;
  padding: 0 5px;
}
</style>

<style>
.notification-dropdown {
  width: 360px !important;
  padding: 0 !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1) !important;
  overflow: hidden;
}
</style>
