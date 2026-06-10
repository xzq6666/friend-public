import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { PermissionUtils } from '../utils/permission'
import { ErrorHandler } from '../utils/errorHandler'

// 布局组件
import AdminLayout from '../layouts/AdminLayout.vue'
import UserLayout from '../layouts/UserLayout.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomePage.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/unauthorized',
    name: 'Unauthorized',
    component: () => import('../views/Unauthorized.vue')
  },
  // 管理员路由 — 侧边栏布局
  {
    path: '/admin',
    component: AdminLayout,
    meta: { layout: 'admin' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true, allowedRoles: ['ADMIN'], layout: 'admin' }
      },
      {
        path: 'jobs',
        name: 'AdminJobs',
        component: () => import('../views/ManageJobs.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER', 'ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'resumes',
        name: 'AdminResumes',
        component: () => import('../views/ManageResumes.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN', 'EMPLOYER'],
          layout: 'admin'
        }
      },
      {
        path: 'candidates',
        name: 'AdminCandidates',
        component: () => import('../views/ManageCandidates.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER', 'ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'manage-interviews',
        name: 'AdminManageInterviews',
        component: () => import('../views/ManageInterviews.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER', 'ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'data-analysis',
        name: 'DataAnalysis',
        component: () => import('../views/DataAnalysis.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('../views/profile/ProfilePage.vue'),
        meta: { requiresAuth: true, layout: 'admin' }
      },
      {
        path: 'operation-logs',
        name: 'OperationLogs',
        component: () => import('../views/OperationLogs.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'data-export',
        name: 'DataExport',
        component: () => import('../views/DataExport.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        path: 'manage-keywords',
        name: 'ManageKeywords',
        component: () => import('../views/ManageKeywords.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        // 企业认证审核页面（仅管理员）
        path: 'company-verify',
        name: 'CompanyVerifyManage',
        component: () => import('../views/admin/CompanyVerifyManage.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        // 举报管理页面（仅管理员）
        path: 'report-manage',
        name: 'ReportManage',
        component: () => import('../views/admin/ReportManage.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        // 公告管理页面（仅管理员）
        path: 'announcement-manage',
        name: 'AnnouncementManage',
        component: () => import('../views/admin/AnnouncementManage.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      },
      {
        // 论坛管理页面（管理员+企业）
        path: 'forum-manage',
        name: 'AdminForumManage',
        component: () => import('../views/ManageForum.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER', 'ADMIN'],
          layout: 'admin'
        }
      },
      {
        // 敏感词管理页面（仅管理员）
        path: 'sensitive-word-manage',
        name: 'SensitiveWordManage',
        component: () => import('../views/admin/SensitiveWordManage.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['ADMIN'],
          layout: 'admin'
        }
      }
    ]
  },
  // 求职者/企业用户路由 — 顶部横排导航布局
  {
    path: '/',
    component: UserLayout,
    meta: { layout: 'user' },
    children: [
      {
        path: 'dashboard',
        name: 'UserDashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAuth: true, allowedRoles: ['EMPLOYEE', 'EMPLOYER'], layout: 'user' }
      },
      // 求职者专属
      {
        path: 'my-resume',
        name: 'MyResume',
        component: () => import('../views/MyResume.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'my-applications',
        name: 'MyApplications',
        component: () => import('../views/MyApplications.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'my-interviews',
        name: 'MyInterviews',
        component: () => import('../views/MyInterviews.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'browse-jobs',
        name: 'BrowseJobs',
        component: () => import('../views/Jobs.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'skill-graph',
        name: 'SkillGraph',
        component: () => import('../views/SkillGraph.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'application-board',
        name: 'ApplicationBoard',
        component: () => import('../views/ApplicationBoard.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      {
        path: 'job-subscription',
        name: 'JobSubscription',
        component: () => import('../views/JobSubscription.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE'],
          layout: 'user'
        }
      },
      // 企业用户专属
      {
        path: 'my-jobs',
        name: 'EmployerJobs',
        component: () => import('../views/ManageJobs.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'my-resumes-pool',
        name: 'EmployerResumes',
        component: () => import('../views/ManageResumes.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'my-candidates',
        name: 'EmployerCandidates',
        component: () => import('../views/ManageCandidates.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'my-manage-interviews',
        name: 'EmployerManageInterviews',
        component: () => import('../views/ManageInterviews.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'talent-market',
        name: 'TalentMarket',
        component: () => import('../views/TalentMarket.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER', 'ADMIN'],
          layout: 'user'
        }
      },
      {
        // 简历详情查看页面
        path: 'r/resume/:id',
        name: 'ResumeDetail',
        component: () => import('../views/MyResume.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER', 'ADMIN'],
          layout: 'user'
        }
      },
      // 求职者 + 企业用户共有
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('../views/FavoritesSimple.vue'), // 使用简化版本
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'match',
        name: 'Match',
        component: () => import('../views/Match.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/profile/ProfilePage.vue'),
        meta: { requiresAuth: true, layout: 'user' }
      },
      {
        // 企业信息认证页面（仅企业用户）
        path: 'company-profile',
        name: 'CompanyProfile',
        component: () => import('../views/CompanyProfile.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'company/:id',
        name: 'CompanyPublicProfile',
        component: () => import('../views/CompanyPublicProfile.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER', 'ADMIN'],
          layout: 'user'
        }
      },
      {
        path: 'interview-chat/:id',
        name: 'InterviewChat',
        component: () => import('../views/InterviewChat.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'chat-history',
        name: 'ChatHistory',
        component: () => import('../views/ChatHistory.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'interview-calendar',
        name: 'InterviewCalendar',
        component: () => import('../views/InterviewCalendar.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'visit-history',
        name: 'VisitHistory',
        component: () => import('../views/VisitHistory.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        path: 'report-history',
        name: 'ReportHistory',
        component: () => import('../views/ReportHistory.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER'],
          layout: 'user'
        }
      },
      {
        // 职位论坛页面
        path: 'forum/:jobId',
        name: 'JobForum',
        component: () => import('../views/JobForum.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYEE', 'EMPLOYER', 'ADMIN'],
          layout: 'user'
        }
      },
      {
        // 企业论坛管理页面
        path: 'my-forum',
        name: 'ManageForum',
        component: () => import('../views/ManageForum.vue'),
        meta: {
          requiresAuth: true,
          allowedRoles: ['EMPLOYER'],
          layout: 'user'
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
let isFetchingUserInfo = false

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  // 已登录用户访问登录/注册页面，重定向到仪表盘（但允许访问首页）
  if (to.meta.noAuth && to.name !== 'Home' && token) {
    next('/dashboard')
    return
  }

  // 未登录用户访问需要认证的页面
  if (to.meta.requiresAuth && !token) {
    ErrorHandler.info('请先登录')
    next('/login')
    return
  }

  // 已登录用户，检查权限
  if (to.meta.requiresAuth && token) {
    // 如果用户信息未加载，先获取用户信息（防止重复调用）
    if (!userStore.user && !isFetchingUserInfo) {
      isFetchingUserInfo = true
      try {
        const userInfo = await userStore.fetchUserInfo()
        if (!userInfo) {
          console.warn('路由守卫：无法获取用户信息，但保留登录状态')
        }
      } catch (error) {
        console.warn('路由守卫：获取用户信息异常:', error?.message)
        return
      } finally {
        isFetchingUserInfo = false
      }
    }

    // 再次检查 token（可能在 fetchUserInfo 过程中被清除）
    const currentToken = localStorage.getItem('token')
    if (!currentToken) {
      return
    }

    const userRole = userStore.user?.userType

    // 检查权限
    if (userRole && !PermissionUtils.canAccessRoute(userRole, to.meta)) {
      // ADMIN 用户访问普通用户页面时，自动跳转到管理后台
      if (userRole === 'ADMIN' && !to.path.startsWith('/admin')) {
        next('/admin/dashboard')
        return
      }
      console.warn('[路由守卫] 权限拦截:', {
        userRole,
        allowedRoles: to.meta.allowedRoles,
        path: to.path,
        name: to.name
      })
      ErrorHandler.warning('您没有权限访问此页面')
      next({
        path: '/unauthorized',
        query: { from: to.fullPath }
      })
      return
    }
  }

  next()
})

// 路由错误处理
router.onError((error, to, from) => {
  console.error('路由错误:', error)
  ErrorHandler.handle(error, 'silent')
})

export default router
