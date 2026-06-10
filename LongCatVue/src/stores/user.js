import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import { ErrorHandler } from '../utils/errorHandler'
import { USER_ROLES, PermissionUtils } from '../utils/permission'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || '')
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const userRole = computed(() => user.value?.userType)
  const isAdmin = computed(() => userRole.value === USER_ROLES.ADMIN)
  const isEmployer = computed(() => userRole.value === USER_ROLES.EMPLOYER)
  const isEmployee = computed(() => userRole.value === USER_ROLES.EMPLOYEE)

  const setUser = (userData) => {
    user.value = userData
    if (userData) {
      localStorage.setItem('user', JSON.stringify(userData))
    } else {
      localStorage.removeItem('user')
    }
  }

  const updateUser = (userData) => {
    if (userData && user.value) {
      // 合并更新用户信息
      user.value = { ...user.value, ...userData }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  const setToken = (tokenValue) => {
    token.value = tokenValue
    if (tokenValue) {
      localStorage.setItem('token', tokenValue)
    } else {
      localStorage.removeItem('token')
    }
  }

  const login = async (credentials) => {
    loading.value = true
    try {
      const response = await request.post('/auth/login', credentials)
      if (response.token && response.user) {
        setToken(response.token)
        setUser(response.user)
        return true
      }
      return false
    } catch (error) {
      ErrorHandler.handle(error, 'login')
      return false
    } finally {
      loading.value = false
    }
  }

  const register = async (userData) => {
    loading.value = true
    try {
      await request.post('/auth/register', userData)
      return true
    } catch (error) {
      ErrorHandler.handle(error, 'register')
      return false
    } finally {
      loading.value = false
    }
  }

  const logout = (showMessage = true) => {
    user.value = null
    setToken('')
    if (showMessage) {
      ErrorHandler.info('已退出登录')
    }
  }

  const fetchUserInfo = async () => {
    if (!token.value) return null
    try {
      // 使用静默模式，避免401时强制跳转
      const data = await request.get('/user/profile', {
        skipErrorNotification: true,
        skipGlobalLoading: true
      })
      setUser(data)
      return data
    } catch (error) {
      // 静默失败，不自动登出，让调用方决定如何处理
      console.warn('获取用户信息失败:', error)
      return null
    }
  }

  // 权限检查方法
  const hasPermission = (permission) => {
    return PermissionUtils.hasPermission(userRole.value, permission)
  }

  const hasAnyPermission = (permissions) => {
    return PermissionUtils.hasAnyPermission(userRole.value, permissions)
  }

  const hasAllPermissions = (permissions) => {
    return PermissionUtils.hasAllPermissions(userRole.value, permissions)
  }

  // 初始化用户信息
  const initializeAuth = async () => {
    if (token.value && !user.value) {
      await fetchUserInfo()
    }
  }

  return {
    // 状态
    user,
    token,
    loading,
    // 计算属性
    isLoggedIn,
    userRole,
    isAdmin,
    isEmployer,
    isEmployee,
    // 方法
    setUser,
    updateUser,
    setToken,
    login,
    register,
    logout,
    fetchUserInfo,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    initializeAuth
  }
})
