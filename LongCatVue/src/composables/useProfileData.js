import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { getNotificationPrefs, updateNotificationPrefs } from '../api/user'

export function useProfileData() {
  const userStore = useUserStore()
  const user = computed(() => userStore.user)
  const isEmployer = computed(() => user.value?.userType === 'EMPLOYER')
  const isEmployee = computed(() => user.value?.userType === 'EMPLOYEE')
  const isAdmin = computed(() => user.value?.userType === 'ADMIN')

  // ---- 统计数据 ----
  const stats = ref({ count1: 0, count2: 0, count3: 0, count4: 0 })

  const loadStats = async () => {
    try {
      const res = await request.get('/user/profile/stats')
      if (res) {
        stats.value = {
          count1: res.count1 || 0,
          count2: res.count2 || 0,
          count3: res.count3 || 0,
          count4: res.count4 || 0
        }
      }
    } catch (error) {
      console.error('加载统计数据失败', error)
    }
  }

  const getStatLabel = (index) => {
    const type = user.value?.userType
    if (type === 'ADMIN') {
      return ['管理职位', '管理简历', '平台用户', '投递记录'][index]
    } else if (type === 'EMPLOYER') {
      return ['发布职位', '收到简历', '面试邀请', '已录用'][index]
    } else {
      return ['我的简历', '投递职位', '匹配成功', '被查看'][index]
    }
  }

  // ---- 编辑资料 ----
  const editDialogVisible = ref(false)
  const editFormRef = ref()
  const updating = ref(false)
  const editForm = reactive({ email: '', phone: '' })
  const editRules = {
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
    phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
  }

  const showEditDialog = () => {
    editForm.email = user.value?.email || ''
    editForm.phone = user.value?.phone || ''
    editDialogVisible.value = true
  }

  const updateProfile = async () => {
    const valid = await editFormRef.value.validate().catch(() => false)
    if (!valid) return
    updating.value = true
    try {
      const res = await request.put('/user/profile', {
        email: editForm.email,
        phone: editForm.phone
      })
      userStore.updateUser(res)
      ElMessage.success('个人资料更新成功')
      editDialogVisible.value = false
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '更新失败')
    } finally {
      updating.value = false
    }
  }

  // ---- 修改密码 ----
  const passwordDialogVisible = ref(false)
  const passwordFormRef = ref()
  const changingPassword = ref(false)
  const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

  const validateConfirmPassword = (rule, value, callback) => {
    if (value !== passwordForm.newPassword) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const passwordRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请再次输入新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }

  const showPasswordDialog = () => {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
  }

  const changePassword = async () => {
    const valid = await passwordFormRef.value.validate().catch(() => false)
    if (!valid) return
    changingPassword.value = true
    try {
      await request.put('/user/password', {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      userStore.user = null
      userStore.token = ''
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '密码修改失败')
    } finally {
      changingPassword.value = false
    }
  }

  // ---- 通知偏好 ----
  const prefsLoading = ref(false)
  const prefsSaving = ref(false)
  const notificationPrefs = reactive({
    emailNotify: true,
    smsNotify: false,
    jobMatchNotify: true,
    interviewNotify: true,
    systemNotify: true,
  })

  const loadNotificationPrefs = async () => {
    prefsLoading.value = true
    try {
      const res = await getNotificationPrefs()
      if (res) {
        Object.assign(notificationPrefs, {
          emailNotify: res.emailNotify === 1 || res.emailNotify === true,
          smsNotify: res.smsNotify === 1 || res.smsNotify === true,
          jobMatchNotify: res.jobMatchNotify === 1 || res.jobMatchNotify === true,
          interviewNotify: res.interviewNotify === 1 || res.interviewNotify === true,
          systemNotify: res.systemNotify === 1 || res.systemNotify === true,
        })
      }
    } catch (error) {
      console.warn('加载通知偏好失败:', error)
    } finally {
      prefsLoading.value = false
    }
  }

  const saveNotificationPrefs = async () => {
    prefsSaving.value = true
    try {
      const payload = {
        emailNotify: notificationPrefs.emailNotify ? 1 : 0,
        smsNotify: notificationPrefs.smsNotify ? 1 : 0,
        jobMatchNotify: notificationPrefs.jobMatchNotify ? 1 : 0,
        interviewNotify: notificationPrefs.interviewNotify ? 1 : 0,
        systemNotify: notificationPrefs.systemNotify ? 1 : 0,
      }
      await updateNotificationPrefs(payload)
      ElMessage.success('通知偏好已保存')
    } catch (error) {
      ElMessage.error(error.response?.data?.error || '保存失败')
    } finally {
      prefsSaving.value = false
    }
  }

  // ---- 最近活动 ----
  const recentActivities = ref([])
  const activitiesLoading = ref(false)

  const loadRecentActivities = async () => {
    activitiesLoading.value = true
    try {
      const type = user.value?.userType
      if (type === 'EMPLOYEE') {
        const res = await request.get('/application/my', { params: { page: 1, size: 5 }, skipErrorNotification: true })
        recentActivities.value = (res?.records || res || []).slice(0, 5).map(app => ({
          id: app.id,
          title: app.jobTitle || app.job_title || app.job?.title || (app.jobId ? '职位#' + app.jobId : '未知职位'),
          subtitle: app.employerName || app.employer_name || app.companyName || '',
          time: app.applyTime || app.apply_time || app.createTime || app.create_time,
          status: app.status,
          type: 'application'
        }))
      } else if (type === 'EMPLOYER') {
        const res = await request.get('/job', { params: { page: 1, size: 5 }, skipErrorNotification: true })
        recentActivities.value = (res?.records || res || []).slice(0, 5).map(job => ({
          id: job.id,
          title: job.title || '未知职位',
          subtitle: job.companyName || job.company_name || '',
          time: job.createTime || job.create_time,
          status: job.status,
          type: 'job'
        }))
      } else if (type === 'ADMIN') {
        const res = await request.get('/job', { params: { page: 1, size: 5 }, skipErrorNotification: true })
        recentActivities.value = (res?.records || res || []).slice(0, 5).map(job => ({
          id: job.id,
          title: job.title || '未知职位',
          subtitle: job.companyName || job.company_name || '',
          time: job.createTime || job.create_time,
          status: job.status,
          type: 'job'
        }))
      }
    } catch (error) {
      console.warn('加载最近活动失败:', error)
    } finally {
      activitiesLoading.value = false
    }
  }

  // ---- 辅助函数 ----
  const getRoleName = (type) => {
    const names = { EMPLOYEE: '求职者', EMPLOYER: '企业招聘', ADMIN: '管理员' }
    return names[type] || '未知'
  }

  const formatTime = (t) => {
    if (!t) return '-'
    return new Date(t).toLocaleString('zh-CN')
  }

  // ---- 初始化 ----
  const initProfile = async () => {
    if (!userStore.user) {
      await userStore.fetchUserInfo()
    }
    await Promise.all([loadStats(), loadNotificationPrefs(), loadRecentActivities()])
  }

  return {
    // 用户
    user, isEmployer, isEmployee, isAdmin,
    // 统计
    stats, getStatLabel, loadStats,
    // 编辑资料
    editDialogVisible, editFormRef, updating, editForm, editRules, showEditDialog, updateProfile,
    // 修改密码
    passwordDialogVisible, passwordFormRef, changingPassword, passwordForm, passwordRules, showPasswordDialog, changePassword,
    // 通知偏好
    prefsLoading, prefsSaving, notificationPrefs, loadNotificationPrefs, saveNotificationPrefs,
    // 最近活动
    recentActivities, activitiesLoading, loadRecentActivities,
    // 辅助
    getRoleName, formatTime,
    // 初始化
    initProfile
  }
}
