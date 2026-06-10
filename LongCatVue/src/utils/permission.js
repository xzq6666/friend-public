export const USER_ROLES = {
  ADMIN: 'ADMIN',
  EMPLOYER: 'EMPLOYER',
  EMPLOYEE: 'EMPLOYEE'
}

export const PERMISSIONS = {
  // 用户管理
  USER_VIEW: 'user:view',
  USER_CREATE: 'user:create',
  USER_EDIT: 'user:edit',
  USER_DELETE: 'user:delete',
  USER_DISABLE: 'user:disable',

  // 简历管理
  RESUME_VIEW: 'resume:view',
  RESUME_CREATE: 'resume:create',
  RESUME_EDIT: 'resume:edit',
  RESUME_DELETE: 'resume:delete',
  RESUME_AI_ANALYZE: 'resume:ai-analyze',

  // 职位管理
  JOB_VIEW: 'job:view',
  JOB_CREATE: 'job:create',
  JOB_EDIT: 'job:edit',
  JOB_DELETE: 'job:delete',
  JOB_PUBLISH: 'job:publish',

  // 匹配管理
  MATCH_VIEW: 'match:view',
  MATCH_CREATE: 'match:create',
  MATCH_DELETE: 'match:delete',

  // 系统管理
  SYSTEM_STATS: 'system:stats',
  SYSTEM_CONFIG: 'system:config'
}

// 角色权限映射
export const ROLE_PERMISSIONS = {
  [USER_ROLES.ADMIN]: [
    PERMISSIONS.USER_VIEW,
    PERMISSIONS.USER_CREATE,
    PERMISSIONS.USER_EDIT,
    PERMISSIONS.USER_DELETE,
    PERMISSIONS.USER_DISABLE,
    PERMISSIONS.RESUME_VIEW,
    PERMISSIONS.RESUME_EDIT,
    PERMISSIONS.RESUME_DELETE,
    PERMISSIONS.RESUME_AI_ANALYZE,
    PERMISSIONS.JOB_VIEW,
    PERMISSIONS.JOB_EDIT,
    PERMISSIONS.JOB_DELETE,
    PERMISSIONS.JOB_PUBLISH,
    PERMISSIONS.MATCH_VIEW,
    PERMISSIONS.MATCH_DELETE,
    PERMISSIONS.SYSTEM_STATS,
    PERMISSIONS.SYSTEM_CONFIG
  ],
  [USER_ROLES.EMPLOYER]: [
    PERMISSIONS.RESUME_VIEW,
    PERMISSIONS.RESUME_AI_ANALYZE,
    PERMISSIONS.JOB_VIEW,
    PERMISSIONS.JOB_CREATE,
    PERMISSIONS.JOB_EDIT,
    PERMISSIONS.JOB_DELETE,
    PERMISSIONS.JOB_PUBLISH,
    PERMISSIONS.MATCH_VIEW,
    PERMISSIONS.MATCH_CREATE,
    PERMISSIONS.SYSTEM_STATS
  ],
  [USER_ROLES.EMPLOYEE]: [
    PERMISSIONS.RESUME_VIEW,
    PERMISSIONS.RESUME_CREATE,
    PERMISSIONS.RESUME_EDIT,
    PERMISSIONS.JOB_VIEW,
    PERMISSIONS.MATCH_VIEW,
    PERMISSIONS.SYSTEM_STATS
  ]
}

// 权限检查工具
export class PermissionUtils {
  static hasPermission(userRole, permission) {
    if (!userRole || !permission) return false
    return ROLE_PERMISSIONS[userRole]?.includes(permission) || false
  }

  static hasAnyPermission(userRole, permissions) {
    if (!userRole || !permissions || !Array.isArray(permissions)) return false
    return permissions.some(permission => this.hasPermission(userRole, permission))
  }

  static hasAllPermissions(userRole, permissions) {
    if (!userRole || !permissions || !Array.isArray(permissions)) return false
    return permissions.every(permission => this.hasPermission(userRole, permission))
  }

  static isAdmin(userRole) {
    return userRole === USER_ROLES.ADMIN
  }

  static isEmployer(userRole) {
    return userRole === USER_ROLES.EMPLOYER
  }

  static isEmployee(userRole) {
    return userRole === USER_ROLES.EMPLOYEE
  }

  static canAccessRoute(userRole, routeMeta) {
    if (!routeMeta) return true

    // 检查是否需要认证
    if (routeMeta.requiresAuth && !userRole) return false

    // 检查角色权限
    if (routeMeta.allowedRoles && Array.isArray(routeMeta.allowedRoles)) {
      return routeMeta.allowedRoles.includes(userRole)
    }

    // 检查具体权限
    if (routeMeta.requiredPermissions && Array.isArray(routeMeta.requiredPermissions)) {
      return this.hasAnyPermission(userRole, routeMeta.requiredPermissions)
    }

    return true
  }
}

// Vue指令 - 权限控制
export const PermissionDirective = {
  mounted(el, binding, vnode) {
    const { value } = binding
    const userStore = vnode.appContext.app.config.globalProperties.$pinia?.state?.value?.user
    const userRole = userStore?.user?.userType

    if (!value) return

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
}