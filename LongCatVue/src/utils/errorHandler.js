import { ElMessage, ElNotification } from 'element-plus'
import { useUserStore } from '../stores/user'

export class ErrorHandler {
  static handle(error, context = '') {
    console.error(`[Error${context ? ' - ' + context : ''}]`, error)

    if (error.response) {
      // 服务器响应错误
      const { status, data } = error.response
      return this.handleHttpError(status, data, context)
    } else if (error.request) {
      // 网络错误
      return this.handleNetworkError(error.request, context)
    } else {
      // 其他错误
      return this.handleUnknownError(error, context)
    }
  }

  static handleHttpError(status, data, context) {
    const errorMap = {
      400: {
        title: '操作失败',
        message: data?.error || '请求参数错误'
      },
      401: {
        title: '认证失败',
        message: '登录已过期，请重新登录',
        action: () => {
          // 同步更新 Pinia store，避免状态不一致
          try {
            const userStore = useUserStore()
            userStore.user = null
            userStore.token = ''
          } catch { /* store 可能未初始化 */ }
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // 立即跳转，不延迟，避免用户在 token 失效后继续操作
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
        }
      },
      403: {
        title: '权限不足',
        message: '您没有权限执行此操作'
      },
      404: {
        title: '未找到数据',
        message: data?.error || '暂无相关数据'
      },
      500: {
        title: '系统异常',
        message: '系统繁忙，请稍后重试'
      },
      502: {
        title: '服务不可用',
        message: '服务暂时不可用，请稍后重试'
      },
      503: {
        title: '服务维护',
        message: '服务正在维护中，请稍后重试'
      }
    }

    const errorInfo = errorMap[status] || {
      title: '请求失败',
      message: data?.error || '操作失败，请重试'
    }

    this.showError(errorInfo, context)

    if (errorInfo.action) {
      errorInfo.action()
    }

    return errorInfo
  }

  static handleNetworkError(request, context) {
    const errorInfo = {
      title: '网络错误',
      message: '网络连接异常，请检查网络设置'
    }

    this.showError(errorInfo, context)
    return errorInfo
  }

  static handleUnknownError(error, context) {
    const errorInfo = {
      title: '未知错误',
      message: error.message || '操作失败，请重试'
    }

    this.showError(errorInfo, context)
    return errorInfo
  }

  static showError(errorInfo, context) {
    const duration = context === 'login' ? 2000 : 3000

    if (context === 'silent') {
      // 静默错误，只记录到控制台
      console.warn('Silent error:', errorInfo)
      return
    }

    if (context === 'form') {
      // 表单错误，使用ElMessage
      ElMessage.error(errorInfo.message)
    } else {
      // 全局错误，使用ElNotification
      ElNotification.error({
        title: errorInfo.title,
        message: errorInfo.message,
        duration,
        position: 'top-right'
      })
    }
  }

  // 成功提示
  static success(message, context = '') {
    const duration = context === 'login' ? 1500 : 2000

    ElMessage.success({
      message,
      duration,
      type: 'success'
    })
  }

  // 警告提示
  static warning(message, context = '') {
    const duration = context === 'login' ? 2000 : 3000

    ElMessage.warning({
      message,
      duration,
      type: 'warning'
    })
  }

  // 信息提示
  static info(message, context = '') {
    const duration = context === 'login' ? 2000 : 3000

    ElMessage.info({
      message,
      duration,
      type: 'info'
    })
  }
}