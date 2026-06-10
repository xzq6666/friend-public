import { defineStore } from 'pinia'
import { ref, computed, onUnmounted } from 'vue'
import { getUnreadCount, getNotifications, markAsRead, markAllRead, deleteNotification } from '../api/notification'

/**
 * 通知 Store
 * 支持：轮询获取未读数 + 页面可见性检测 + 手动刷新
 */
export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const pollTimer = ref(null)
  const isPollingActive = ref(false)

  // 未读通知列表
  const unreadList = computed(() => notifications.value.filter(n => !n.isRead))

  // 按时间排序
  const sortedNotifications = computed(() => {
    return [...notifications.value].sort((a, b) => {
      return new Date(b.createTime || 0) - new Date(a.createTime || 0)
    })
  })

  // 获取未读数
  const fetchUnreadCount = async () => {
    // 先检查 token 是否存在，避免无意义的请求
    const token = localStorage.getItem('token')
    if (!token) {
      stopPolling()
      return
    }
    try {
      const res = await getUnreadCount()
      // request.js 拦截器返回 response.data，所以直接取 count
      unreadCount.value = res?.count || 0
    } catch (error) {
      // 401 表示 token 失效，停止轮询并清除登录状态
      if (error?.response?.status === 401) {
        stopPolling()
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        return
      }
      console.warn('[NotificationStore] fetchUnreadCount failed:', error?.response?.status, error?.response?.data || error?.message)
    }
  }

  // 获取通知列表
  const fetchNotifications = async (params = {}) => {
    loading.value = true
    try {
      const res = await getNotifications(params)
      console.log('[NotificationStore] fetchNotifications response:', res)
      // 后端返回的是 IPage 对象，records 字段包含列表
      const records = res?.records || res?.list || []
      notifications.value = Array.isArray(records) ? records : []
      // 如果响应中有未读数，也更新
      if (res?.unreadCount !== undefined) {
        unreadCount.value = res.unreadCount
      }
    } catch (error) {
      if (error?.response?.status !== 401) {
        console.warn('[NotificationStore] fetchNotifications failed:', error?.response?.status, error?.response?.data || error?.message)
      }
      notifications.value = []
    } finally {
      loading.value = false
    }
  }

  // 标记已读
  const readNotification = async (id) => {
    try {
      await markAsRead(id)
      const item = notifications.value.find(n => n.id === id)
      if (item) item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // 静默失败
    }
  }

  // 全部已读
  const readAll = async () => {
    try {
      await markAllRead()
      notifications.value.forEach(n => (n.isRead = true))
      unreadCount.value = 0
    } catch {
      // 静默失败
    }
  }

  // 删除通知
  const removeNotification = async (id) => {
    try {
      await deleteNotification(id)
      const item = notifications.value.find(n => n.id === id)
      if (item && !item.isRead) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value = notifications.value.filter(n => n.id !== id)
    } catch {
      // 静默失败
    }
  }

  // 页面可见性变化处理
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      console.log('[NotificationStore] 页面变为可见，立即刷新通知')
      fetchUnreadCount()
    }
  }

  // 启动轮询
  const startPolling = () => {
    if (isPollingActive.value) {
      console.log('[NotificationStore] 轮询已在运行，跳过')
      return
    }
    isPollingActive.value = true

    console.log('[NotificationStore] 启动通知轮询')

    // 立即获取一次
    fetchUnreadCount()

    // 30秒轮询一次（从60秒改为30秒，更快响应）
    pollTimer.value = setInterval(() => {
      fetchUnreadCount()
    }, 30000)

    // 监听页面可见性变化
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  // 停止轮询
  const stopPolling = () => {
    if (pollTimer.value) {
      clearInterval(pollTimer.value)
      pollTimer.value = null
    }
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    isPollingActive.value = false
    console.log('[NotificationStore] 停止通知轮询')
  }

  return {
    // 状态
    notifications,
    unreadCount,
    loading,
    isPollingActive,
    // 计算属性
    unreadList,
    sortedNotifications,
    // 方法
    fetchUnreadCount,
    fetchNotifications,
    readNotification,
    readAll,
    removeNotification,
    startPolling,
    stopPolling
  }
})
