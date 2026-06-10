import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import { useLoadingStore } from '../stores/loading'
import { useUserStore } from '../stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000, // 增加到120秒，兼容AI分析等耗时操作
  headers: {
    'Content-Type': 'application/json'
  }
})

// 用于存储请求的自定义配置（解决 error.config 丢失自定义属性的问题）
const requestConfigMap = new Map()

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 存储自定义配置到 Map（使用 config 的引用作为 key）
    if (config.skipErrorNotification || config.skipGlobalLoading || config.skipNotFoundNotification) {
      requestConfigMap.set(config, {
        skipErrorNotification: config.skipErrorNotification,
        skipGlobalLoading: config.skipGlobalLoading,
        skipNotFoundNotification: config.skipNotFoundNotification
      })
    }

    // 全局loading（如果配置了skipGlobalLoading则跳过）
    if (!config.skipGlobalLoading) {
      const loadingStore = useLoadingStore()
      loadingStore.startLoading()
    }

    return config
  },
  (error) => {
    if (!error.config?.skipGlobalLoading) {
      const loadingStore = useLoadingStore()
      loadingStore.stopLoading()
    }
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 清理 Map 中的配置
    requestConfigMap.delete(response.config)

    if (!response.config?.skipGlobalLoading) {
      const loadingStore = useLoadingStore()
      loadingStore.stopLoading()
    }
    return response.data
  },
  (error) => {
    // 从 Map 中获取自定义配置（优先），其次从 error.config 获取
    const customConfig = requestConfigMap.get(error.config) || {}
    const skipErrorNotification = customConfig.skipErrorNotification || error.config?.skipErrorNotification
    const skipGlobalLoading = customConfig.skipGlobalLoading || error.config?.skipGlobalLoading
    const skipNotFoundNotification = customConfig.skipNotFoundNotification || error.config?.skipNotFoundNotification

    // 清理 Map 中的配置
    requestConfigMap.delete(error.config)

    if (!skipGlobalLoading) {
      const loadingStore = useLoadingStore()
      loadingStore.stopLoading()
    }

    // 检查是否跳过错误提示（包括 401）
    if (skipErrorNotification) {
      return Promise.reject(error)
    }

    const { response } = error

    // 超时或网络错误不触发登录跳转
    const isTimeout = error.code === 'ECONNABORTED' || error.message?.includes('timeout')
    const isNetworkError = !response && !isTimeout

    if (response) {
      switch (response.status) {
        case 401:
          // 非静默请求：清除 token 并跳转登录
          // 同步更新 Pinia store，避免状态不一致
          try {
            const userStore = useUserStore()
            userStore.user = null
            userStore.token = ''
          } catch { /* store 可能未初始化 */ }
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // 避免重复跳转
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
          break
        case 403:
          ElNotification.warning({
            title: '权限不足',
            message: '您没有权限访问此资源',
            duration: 4000
          })
          break
        case 404:
          // 404 仅在非静默请求时提示
          if (!skipNotFoundNotification) {
            ElNotification.error({
              title: '未找到数据',
              message: error.response?.data?.error || '暂无相关数据',
              duration: 3000
            })
          }
          break
        case 500:
          ElNotification.error({
            title: '系统异常',
            message: '系统繁忙，请稍后重试',
            duration: 4000
          })
          break
        default:
          ElNotification.error({
            title: '请求失败',
            message: response.data?.error || '操作失败，请重试',
            duration: 3000
          })
      }
    } else if (isTimeout) {
      ElNotification.error({
        title: '请求超时',
        message: '服务器响应超时，请稍后重试',
        duration: 4000
      })
    } else if (isNetworkError) {
      ElNotification.error({
        title: '网络错误',
        message: '网络连接异常，请检查网络设置',
        duration: 4000
      })
    }
    return Promise.reject(error)
  }
)

export default request
