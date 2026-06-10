import { PermissionUtils } from '../utils/permission'
import { useUserStore } from '../stores/user'

export default {
  install(app) {
    app.directive('permission', {
      mounted(el, binding, vnode) {
        const { value } = binding

        // 使用getCurrentInstance获取userStore
        const instance = vnode.component?.instance || vnode.ctx
        let userStore
        try {
          userStore = useUserStore()
        } catch (error) {
          console.warn('权限指令无法获取用户状态:', error)
          return
        }

        const userRole = userStore?.userType

        if (!value || !userRole) return

        let hasPermission = false

        if (typeof value === 'string') {
          // 单个权限检查
          hasPermission = PermissionUtils.hasPermission(userRole, value)
        } else if (Array.isArray(value)) {
          // 多个权限，满足任意一个即可
          hasPermission = PermissionUtils.hasAnyPermission(userRole, value)
        } else if (typeof value === 'object') {
          // 复杂权限对象
          const { any, all, role } = value
          if (role) {
            hasPermission = userRole === role
          } else if (any) {
            hasPermission = PermissionUtils.hasAnyPermission(userRole, any)
          } else if (all) {
            hasPermission = PermissionUtils.hasAllPermissions(userRole, all)
          }
        }

        if (!hasPermission) {
          el.parentNode?.removeChild(el)
        }
      }
    })

    app.directive('role', {
      mounted(el, binding, vnode) {
        const { value } = binding

        let userStore
        try {
          userStore = useUserStore()
        } catch (error) {
          console.warn('角色指令无法获取用户状态:', error)
          return
        }

        const userRole = userStore?.userType

        if (!value || userRole !== value) {
          el.parentNode?.removeChild(el)
        }
      }
    })
  }
}