<template>
  <div class="chat-page">
    <!-- 头部 -->
    <div class="chat-header">
      <el-button @click="goBack" link class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <el-avatar
        v-if="otherProfileRoute"
        :size="40"
        class="header-avatar clickable-avatar"
        @click="$router.push(otherProfileRoute)"
      >
        {{ otherName ? otherName.charAt(0).toUpperCase() : '?' }}
      </el-avatar>
      <div class="header-info">
        <h2>{{ headerTitle }}</h2>
        <p class="header-subtitle">
          <span v-if="otherProfileRoute && otherName" class="clickable-name" @click="$router.push(otherProfileRoute)">
            {{ otherName }}
          </span>
          <span class="header-sep" v-if="otherProfileRoute && otherName">·</span>
          <el-tag v-if="isDirectChat" type="success" size="small">沟通中</el-tag>
          <el-tag v-else :type="getStatusType(interview.status)" size="small">
            {{ getStatusText(interview.status) }}
          </el-tag>
          <span class="interview-time">{{ headerSubTitle }}</span>
        </p>
      </div>
      <div class="header-actions">
        <el-button
          v-if="!userStore.isEmployer && otherProfileRoute"
          type="primary"
          plain
          size="small"
          @click="$router.push(otherProfileRoute)"
        >
          <el-icon><OfficeBuilding /></el-icon>
          查看企业
        </el-button>
        <el-button
          v-if="isDirectChat && userStore.isEmployer"
          type="primary"
          size="small"
          @click="openInterviewDialog"
        >
          <el-icon><Calendar /></el-icon>
          发起面试
        </el-button>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <!-- 空状态 -->
      <div v-if="messages.length === 0 && !isLoadingMessages" class="empty-messages">
        <div class="empty-icon">💬</div>
        <p class="empty-text">暂无聊天记录</p>
        <p class="empty-hint">发送第一条消息开始沟通吧</p>
      </div>

      <!-- 消息列表 -->
      <div v-else class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'is-self': isSelf(msg) }"
        >
          <el-avatar
            :size="36"
            class="message-avatar"
            :style="{ background: isSelf(msg) ? '#409EFF' : '#67C23A' }"
          >
            {{ getAvatarText(msg) }}
          </el-avatar>
          <div class="message-body">
            <div class="message-meta">
              <span class="sender-name" :class="{ 'clickable-name': !isSelf(msg) && otherProfileRoute }" @click="!isSelf(msg) && otherProfileRoute && $router.push(otherProfileRoute)">{{ getSenderName(msg) }}</span>
              <span class="sender-role" :class="isSelf(msg) ? 'self' : 'other'">
                {{ getSenderRole(msg) }}
              </span>
              <span class="message-time">{{ formatTime(msg.create_time || msg.createTime) }}</span>
            </div>
            <div class="message-bubble">
              <!-- 文本消息 -->
              <p v-if="!msg.fileType || msg.fileType === 1" class="message-text">{{ msg.content }}</p>
              
              <!-- 图片消息 -->
              <div v-else-if="msg.fileType === 3" class="message-image">
                <el-image 
                  :src="msg.fileUrl || msg.file_url" 
                  :preview-src-list="[msg.fileUrl || msg.file_url]"
                  fit="cover"
                  class="chat-image"
                />
              </div>
              
              <!-- 文件消息 -->
              <div v-else class="message-file">
                <el-icon class="file-icon" :size="24">
                  <Document v-if="msg.fileType === 2" />
                  <Folder v-else-if="msg.fileType === 4" />
                </el-icon>
                <div class="file-info">
                  <p class="file-name">{{ msg.fileName || msg.file_name || msg.content }}</p>
                  <p class="file-hint">{{ getFileTypeText(msg.fileType || msg.file_type) }}</p>
                </div>
                <el-button 
                  link 
                  type="primary" 
                  size="small" 
                  @click="downloadFile(msg)"
                  class="download-btn"
                >
                  <el-icon><Download /></el-icon>
                </el-button>
              </div>
              
              <div v-if="msg.status === 'failed'" class="message-status failed">
                <el-icon><WarningFilled /></el-icon>
                <span>发送失败</span>
                <el-button link size="small" @click="retrySend(msg)">重试</el-button>
              </div>
              <div v-else-if="msg.status === 'sending'" class="message-status sending">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>发送中</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input">
      <div class="input-wrapper">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button link @click="showEmojiPicker = !showEmojiPicker" class="toolbar-btn" title="表情">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </el-button>
            <el-button link @click="triggerFileInput('image')" class="toolbar-btn" title="图片">
              <el-icon :size="20"><Picture /></el-icon>
            </el-button>
            <el-button link @click="triggerFileInput('file')" class="toolbar-btn" title="文件">
              <el-icon :size="20"><Document /></el-icon>
            </el-button>
            <el-button link @click="triggerFileInput('resume')" class="toolbar-btn" title="个人简历">
              <el-icon :size="20"><Folder /></el-icon>
            </el-button>
          </div>
          <span class="input-hint">Ctrl+Enter 发送</span>
        </div>
        
        <!-- 表情选择器 -->
        <div v-if="showEmojiPicker" class="emoji-picker">
          <div class="emoji-grid">
            <span 
              v-for="emoji in emojis" 
              :key="emoji" 
              class="emoji-item"
              @click="insertEmoji(emoji)"
            >{{ emoji }}</span>
          </div>
        </div>
        
        <!-- 隐藏的文件输入 -->
        <input 
          ref="fileInputRef" 
          type="file" 
          class="hidden-file-input" 
          :accept="currentAcceptType"
          @change="handleFileSelect"
        />
        
        <!-- 文本输入框 -->
        <el-input
          ref="inputRef"
          v-model="inputMessage"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入消息..."
          @keydown="onKeyDown"
          :disabled="sending"
        />
        
        <!-- 发送按钮 -->
        <div class="input-actions">
          <el-button
            type="primary"
            :loading="sending"
            :disabled="!canSend"
            @click="sendMessage"
          >
            <el-icon v-if="!sending"><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 面试邀请对话框 -->
    <el-dialog v-model="showInterviewDialog" title="发起面试邀请" width="480px" destroy-on-close>
      <el-form :model="interviewForm" :rules="interviewRules" ref="interviewFormRef" label-width="90px">
        <el-form-item label="面试职位" prop="jobId">
          <el-select
            v-model="interviewForm.jobId"
            placeholder="选择面试职位"
            style="width: 100%;"
            filterable
          >
            <el-option
              v-for="job in jobList"
              :key="job.id"
              :label="job.title + (job.location ? ' - ' + job.location : '')"
              :value="job.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="面试时间" prop="interviewTime">
          <el-date-picker
            v-model="interviewForm.interviewTime"
            type="datetime"
            placeholder="选择面试时间"
            :disabled-date="disabledPastDate"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="面试类型" prop="interviewType">
          <el-select v-model="interviewForm.interviewType" style="width: 100%;">
            <el-option label="现场面试" :value="1" />
            <el-option label="视频面试" :value="2" />
            <el-option label="电话面试" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="面试地点" prop="interviewLocation">
          <el-input v-model="interviewForm.interviewLocation" placeholder="会议室/线上链接/地址" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="interviewForm.contactPerson" placeholder="HR 姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="interviewForm.contactPhone" placeholder="联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input v-model="interviewForm.notes" type="textarea" :rows="2" placeholder="其他说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInterviewDialog = false">取消</el-button>
        <el-button type="primary" @click="submitInterview" :loading="submitting">发送邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, shallowRef, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Promotion, Calendar, Loading, WarningFilled, ChatDotRound, Picture, Document, Folder, Download, OfficeBuilding } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

// ==================== 路由和状态 ====================
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const interviewId = ref(route.params.id)
const interview = ref({ jobTitle: '在线沟通', status: 0, interviewTime: '', userId: null })
const isDirectChat = ref(false)

// ==================== 消息状态 ====================
const messages = shallowRef([])
const inputMessage = ref('')
const sending = ref(false)
const isLoadingMessages = ref(false)
const messagesContainer = ref(null)
const inputRef = ref(null)

// 表情和文件相关
const showEmojiPicker = ref(false)
const fileInputRef = ref(null)
const currentAcceptType = ref('*')
const currentUploadType = ref('file') // 记录当前上传意图：image/file/resume
const emojis = ref([
  '😀', '', '😄', '😁', '', '😅', '😂', '', '😊', '😇',
  '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
  '😋', '😛', '😜', '😝', '', '🤗', '🤭', '🤫', '🤔', '🤐',
  '👍', '👎', '👊', '✊', '🤛', '🤜', '👏', '', '👐', '🤲',
  '🤝', '🙏', '✌️', '', '🤟', '', '👌', '', '🤏', '',
  '👉', '👆', '👇', '☝️', '✋', '', '🖐', '', '👋', '🤟',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '', '🤎', '💔',
  '❣️', '', '💞', '💓', '', '💖', '💘', '', '💟', '☮️',
])

// 使用普通变量避免触发响应式更新
let isDestroyed = false
const messageIdSet = new Set()

// ==================== 轮询控制 ====================
const POLLING_INTERVAL = 5000 // 5秒
let pollingTimer = null
let isPageVisible = true

// ==================== 面试邀请表单 ====================
const showInterviewDialog = ref(false)
const submitting = ref(false)
const interviewFormRef = ref(null)
const jobList = ref([])
const interviewForm = ref({
  jobId: null,
  interviewTime: '',
  interviewLocation: '',
  interviewType: 1,
  contactPerson: '',
  contactPhone: '',
  notes: ''
})

const interviewRules = {
  jobId: [{ required: true, message: '请选择面试职位', trigger: 'change' }],
  interviewTime: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  interviewType: [{ required: true, message: '请选择面试类型', trigger: 'change' }],
  interviewLocation: [{ required: true, message: '请输入面试地点', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// ==================== 计算属性 ====================
const headerTitle = computed(() => {
  if (isDirectChat.value) return '在线沟通'
  return interview.value.jobTitle || '面试聊天'
})

// 对方信息
const otherUserId = computed(() => {
  if (!interview.value) return null
  return userStore.isEmployer ? (interview.value.userId || interview.value.user_id) : (interview.value.employerId || interview.value.employer_id)
})
const otherProfileRoute = computed(() => {
  if (!otherUserId.value) return null
  return userStore.isEmployer ? `/r/resume/${interview.value.resumeId || interview.value.resume_id || otherUserId.value}` : `/company/${otherUserId.value}`
})
const otherName = computed(() => {
  if (!interview.value) return ''
  return interview.value.chat_partner_name || interview.value.employer_name || interview.value.applicant_name || ''
})

const headerSubTitle = computed(() => {
  if (isDirectChat.value) return '人才市场直接沟通'
  return formatInterviewTime(interview.value.interviewTime)
})

const canSend = computed(() => inputMessage.value.trim().length > 0 && !sending.value)

// ==================== 消息相关方法 ====================
const isSelf = (msg) => {
  const myId = Number(userStore.user?.id)
  const senderId = Number(msg.sender_id ?? msg.senderId)
  return senderId === myId
}

const getAvatarText = (msg) => {
  const name = msg.sender_name || msg.senderName || 'U'
  return name.charAt(0).toUpperCase()
}

const getMessageAvatar = () => ''

const getSenderName = (msg) => {
  return msg.sender_name || msg.senderName || '未知用户'
}

const getSenderRole = (msg) => {
  if (isSelf(msg)) {
    return userStore.isEmployer ? '企业' : '求职者'
  }
  return msg.sender_user_type === 'EMPLOYER' ? '企业' : '求职者'
}

const normalizeMessage = (msg, status = 'sent') => {
  const myId = Number(userStore.user?.id)
  const sid = Number(msg.sender_id ?? msg.senderId)
  // 统一消息类型字段（后端返回 message_type 或 messageType，前端用 fileType）
  const fileType = msg.fileType ?? msg.file_type ?? msg.messageType ?? msg.message_type ?? 1
  return {
    ...msg,
    sender_id: sid,
    fileType,
    fileUrl: msg.fileUrl ?? msg.file_url,
    fileName: msg.fileName ?? msg.file_name,
    sender_user_type: sid === myId
      ? (userStore.isEmployer ? 'EMPLOYER' : 'EMPLOYEE')
      : (msg.sender_user_type || 'EMPLOYEE'),
    status
  }
}

// ==================== 发送消息 ====================
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || sending.value) return

  // 创建临时消息（乐观更新）
  const tempId = `temp_${Date.now()}`
  const tempMsg = {
    id: tempId,
    content,
    sender_id: Number(userStore.user?.id),
    sender_name: userStore.user?.username,
    sender_avatar: userStore.user?.avatar,
    sender_user_type: userStore.isEmployer ? 'EMPLOYER' : 'EMPLOYEE',
    create_time: new Date().toISOString(),
    status: 'sending'
  }

  messages.value = [...messages.value, tempMsg]
  inputMessage.value = ''
  sending.value = true

  scrollToBottom()

  try {
    const res = await request.post('/interview-chat', {
      interviewId: interviewId.value,
      content,
      messageType: 1
    }, {
      skipGlobalLoading: true,
      skipErrorNotification: true
    })

    // 替换临时消息为真实消息
    const realMsg = normalizeMessage(res, 'sent')
    const index = messages.value.findIndex(m => m.id === tempId)
    if (index !== -1) {
      const newMessages = [...messages.value]
      newMessages[index] = realMsg
      messages.value = newMessages
      messageIdSet.add(realMsg.id)
    }
  } catch (e) {
    // 标记发送失败
    const index = messages.value.findIndex(m => m.id === tempId)
    if (index !== -1) {
      const newMessages = [...messages.value]
      newMessages[index] = { ...newMessages[index], status: 'failed' }
      messages.value = newMessages
    }
    ElMessage.error('发送失败，请点击重试')
  } finally {
    sending.value = false
  }
}

const retrySend = (msg) => {
  inputMessage.value = msg.content
  // 移除失败消息
  messages.value = messages.value.filter(m => m.id !== msg.id)
  sendMessage()
}

// ==================== 加载消息 ====================
const loadMessages = async (isFirstLoad = false) => {
  if (isLoadingMessages.value || isDestroyed) return
  isLoadingMessages.value = true

  try {
    const res = await request.get(`/interview-chat/${interviewId.value}`, {
      skipGlobalLoading: true,
      skipErrorNotification: true
    })
    const rawMessages = res || []

    if (isFirstLoad || messages.value.length === 0) {
      // 首次加载：全量替换
      messages.value = rawMessages.map(msg => normalizeMessage(msg))
      messageIdSet.clear()
      messages.value.forEach(m => messageIdSet.add(m.id))
      await nextTick()
      scrollToBottom()
      return
    }

    // 增量更新：只追加新消息
    const newMessages = []
    for (const msg of rawMessages) {
      if (!messageIdSet.has(msg.id)) {
        newMessages.push(normalizeMessage(msg))
        messageIdSet.add(msg.id)
      }
    }

    if (newMessages.length > 0) {
      messages.value = [...messages.value, ...newMessages]
      if (isAtBottom()) {
        nextTick(scrollToBottom)
      }
    }
  } catch (e) {
    console.error('加载消息失败', e)
  } finally {
    isLoadingMessages.value = false
  }
}

// ==================== 滚动控制 ====================
const isAtBottom = () => {
  if (!messagesContainer.value) return true
  const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
  return scrollHeight - scrollTop - clientHeight < 50
}

const scrollToBottom = () => {
  if (!messagesContainer.value) return
  messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
}

// ==================== 轮询控制 ====================
const startPolling = () => {
  stopPolling()
  pollingTimer = setInterval(() => {
    if (isPageVisible && !isDestroyed) {
      loadMessages()
    }
  }, POLLING_INTERVAL)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// ==================== 页面可见性控制 ====================
const handleVisibilityChange = () => {
  isPageVisible = !document.hidden
  if (isPageVisible) {
    // 页面重新可见时立即刷新消息
    loadMessages()
  }
}

// ==================== 面试邀请 ====================
const disabledPastDate = (date) => {
  return date.getTime() < Date.now() - 86400000 // 不能选昨天及之前
}

const loadJobList = async () => {
  try {
    const res = await request.get('/job', { params: { page: 1, size: 100 } })
    jobList.value = res.records || res || []
  } catch (error) {
    console.error('加载职位列表失败', error)
  }
}

const openInterviewDialog = () => {
  loadJobList()
  showInterviewDialog.value = true
}

const submitInterview = async () => {
  if (!interviewFormRef.value) return

  try {
    await interviewFormRef.value.validate()
  } catch {
    return
  }

  const targetUserId = interview.value.userId || interview.value.user_id
  if (!targetUserId) {
    ElMessage.error('无法获取对方信息，请刷新页面重试')
    return
  }

  submitting.value = true
  try {
    await request.post('/interview', {
      ...interviewForm.value,
      applicationId: null,
      jobId: interviewForm.value.jobId,
      userId: targetUserId,
      interviewTime: interviewForm.value.interviewTime,
      interviewType: interviewForm.value.interviewType || 1,
      directChat: false
    })
    ElMessage.success('面试邀请已发送')
    showInterviewDialog.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '发送失败，请重试')
  } finally {
    submitting.value = false
  }
}

// ==================== 加载面试信息 ====================
const loadInterview = async () => {
  try {
    // 优先从 my-chats 获取（包含正式面试和直接沟通）
    const chats = await request.get('/interview/my-chats', { skipErrorNotification: true })
    const found = chats.find(i => i.id === parseInt(interviewId.value))
    if (found) {
      interview.value = found
      isDirectChat.value = !found.jobId && !found.job_id && !found.applicationId && !found.application_id
      return
    }
    // 回退：从正式面试列表获取
    const endpoint = userStore.isEmployer ? '/interview/employer' : '/interview/my'
    const interviews = await request.get(endpoint, { skipErrorNotification: true })
    const found2 = interviews.find(i => i.id === parseInt(interviewId.value))
    if (found2) {
      interview.value = found2
      isDirectChat.value = false
    }
  } catch (e) {
    console.error('加载面试信息失败', e)
  }
}

// ==================== 输入处理 ====================
const onKeyDown = (e) => {
  if (e.key === 'Enter' && e.ctrlKey) {
    e.preventDefault()
    sendMessage()
  }
}

// ==================== 表情和文件处理 ====================
const insertEmoji = (emoji) => {
  inputMessage.value += emoji
  showEmojiPicker.value = false
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const triggerFileInput = (type) => {
  currentUploadType.value = type
  const acceptMap = {
    image: 'image/*',
    file: '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar',
    resume: '.pdf,.doc,.docx'
  }
  currentAcceptType.value = acceptMap[type] || '*'
  fileInputRef.value?.click()
}

const handleFileSelect = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 文件大小限制：10MB
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    e.target.value = ''
    return
  }

  // 根据点击的按钮验证文件类型
  const uploadType = currentUploadType.value
  if (uploadType === 'image' && !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    e.target.value = ''
    return
  }
  if (uploadType === 'resume' && !/\.(pdf|doc|docx)$/i.test(file.name)) {
    ElMessage.warning('请选择 PDF 或 Word 文档')
    e.target.value = ''
    return
  }
  if (uploadType === 'file' && /\.(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(file.name)) {
    ElMessage.warning('图片请使用图片按钮发送')
    e.target.value = ''
    return
  }

  // 根据按钮类型设置消息类型（而不是文件实际类型）
  const typeMap = { image: 3, file: 2, resume: 4 }
  const messageType = typeMap[uploadType] || 2

  const tempId = `temp_${Date.now()}`

  try {
    // 创建 FormData 上传文件
    const formData = new FormData()
    formData.append('file', file)
    formData.append('interviewId', interviewId.value)
    formData.append('messageType', messageType)

    // 创建临时消息
    const tempMsg = {
      id: tempId,
      content: `[${file.name}]`,
      fileUrl: URL.createObjectURL(file),
      fileName: file.name,
      fileType: messageType,
      sender_id: Number(userStore.user?.id),
      sender_name: userStore.user?.username,
      sender_avatar: userStore.user?.avatar,
      sender_user_type: userStore.isEmployer ? 'EMPLOYER' : 'EMPLOYEE',
      create_time: new Date().toISOString(),
      status: 'sending'
    }
    
    messages.value = [...messages.value, tempMsg]
    sending.value = true
    scrollToBottom()
    
    // 上传文件
    const res = await request.post('/interview-chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    // 替换为真实消息
    const realMsg = normalizeMessage(res, 'sent')
    const index = messages.value.findIndex(m => m.id === tempId)
    if (index !== -1) {
      const newMessages = [...messages.value]
      newMessages[index] = realMsg
      messages.value = newMessages
      messageIdSet.add(realMsg.id)
    }
    
    ElMessage.success('文件发送成功')
  } catch (err) {
    ElMessage.error('文件发送失败：' + (err.message || '未知错误'))
    // 移除失败消息
    messages.value = messages.value.filter(m => m.id !== tempId)
  } finally {
    sending.value = false
    e.target.value = ''
  }
}

// ==================== 工具方法 ====================
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatInterviewTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { 0: '待确认', 1: '已确认', 2: '已取消', 3: '已完成' }
  return map[status] || '未知'
}

const goBack = () => {
  router.back()
}

// ==================== 文件相关方法 ====================
const getFileTypeText = (type) => {
  const map = {
    1: '文本消息',
    2: '文件',
    3: '图片',
    4: '简历'
  }
  return map[type] || '文件'
}

const downloadFile = (msg) => {
  const fileUrl = msg.fileUrl || msg.file_url
  if (!fileUrl) {
    ElMessage.warning('文件链接不存在')
    return
  }
  
  const a = document.createElement('a')
  a.href = fileUrl
  a.download = msg.fileName || msg.file_name || 'file'
  a.click()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  isDestroyed = false
  await loadInterview()
  await loadMessages(true)
  
  // 进入聊天页面时标记消息为已读
  try {
    await request.put(`/interview-chat/${interviewId.value}/read`, {
      skipErrorNotification: true
    })
  } catch (e) {
    console.error('标记已读失败', e)
  }
  
  startPolling()

  // 监听页面可见性
  document.addEventListener('visibilitychange', handleVisibilityChange)

  // 输入框自动聚焦
  nextTick(() => {
    inputRef.value?.focus()
  })
})

onUnmounted(() => {
  isDestroyed = true
  stopPolling()
  messageIdSet.clear()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 52px);
  background: var(--gray-50);
  position: relative;
  overflow: hidden;
}

/* 装饰性背景 */
.chat-page::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -30%;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--primary-50) 0%, transparent 70%);
  opacity: 0.5;
  pointer-events: none;
}

.chat-page::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -20%;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--success-50) 0%, transparent 70%);
  opacity: 0.4;
  pointer-events: none;
}

/* ── 头部 ──────────────────────────────────── */
.chat-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  z-index: 10;
}

.back-btn {
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
  color: var(--gray-600);
}

.back-btn:hover {
  background: var(--gray-100);
  color: var(--gray-900);
  transform: translateX(-2px);
}

.header-avatar {
  flex-shrink: 0;
  border: 2px solid var(--gray-200);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all var(--duration-fast) var(--ease-out);
}

.clickable-avatar {
  cursor: pointer;
}

.clickable-avatar:hover {
  border-color: var(--primary-400);
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

.header-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  min-width: 0;
}

.header-info h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--gray-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-subtitle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: 4px 0 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

/* ── 消息区域 ──────────────────────────────── */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6) var(--space-8);
  scroll-behavior: smooth;
  position: relative;
  z-index: 1;
}

.chat-messages::-webkit-scrollbar { width: 5px; }
.chat-messages::-webkit-scrollbar-track { background: transparent; }
.chat-messages::-webkit-scrollbar-thumb { background: var(--gray-200); border-radius: var(--radius-full); }
.chat-messages::-webkit-scrollbar-thumb:hover { background: var(--gray-300); }

.empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-muted);
}

.empty-icon { font-size: 56px; margin-bottom: var(--space-4); opacity: 0.8; }
.empty-text { margin: 0; font-size: var(--text-base); color: var(--color-text-sub); font-weight: var(--weight-medium); }
.empty-hint { margin: var(--space-1) 0 0; font-size: var(--text-sm); color: var(--gray-400); }

/* ── 消息列表 ──────────────────────────────── */
.message-list { display: flex; flex-direction: column; gap: var(--space-5); }

.message-item {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  animation: messageSlideIn 0.35s var(--ease-out);
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.message-item.is-self { flex-direction: row-reverse; }

.message-avatar {
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 2.5px solid var(--color-surface);
  transition: transform var(--duration-fast) var(--ease-out);
}

.message-item:hover .message-avatar {
  transform: scale(1.05);
}

.message-body { max-width: 60%; min-width: 0; }

.message-item.is-self .message-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-bottom: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.message-item.is-self .message-meta { flex-direction: row-reverse; }

.sender-name { font-weight: var(--weight-semibold); color: var(--gray-700); }

.clickable-name {
  cursor: pointer;
  transition: color var(--duration-fast);
}
.clickable-name:hover {
  color: var(--primary-600);
}

.header-sep {
  color: var(--gray-300);
  margin: 0 2px;
}

.sender-role {
  padding: 0 var(--space-1);
  border-radius: var(--radius-xs);
  font-size: 10px;
  font-weight: var(--weight-medium);
}

.sender-role.self { background: var(--primary-50); color: var(--primary-600); }
.sender-role.other { background: var(--success-50); color: var(--success-600); }

.message-time { color: var(--gray-400); font-size: 11px; }

/* ── 消息气泡 ──────────────────────────────── */
.message-bubble {
  position: relative;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-xl);
  background: var(--color-surface);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all var(--duration-normal) var(--ease-out);
  max-width: 100%;
}

.message-bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.message-item.is-self .message-bubble {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  border-color: transparent;
  color: var(--color-surface);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.message-item.is-self .message-bubble:hover {
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3);
}

.message-text {
  margin: 0;
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  color: var(--gray-800);
  word-break: break-word;
  white-space: pre-wrap;
}

.message-item.is-self .message-text { color: var(--color-surface); }

/* 气泡小尾巴 */
.message-item:not(.is-self) .message-bubble::before {
  content: '';
  position: absolute;
  left: -7px;
  top: 14px;
  width: 14px;
  height: 14px;
  background: var(--color-surface);
  border-left: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  transform: rotate(45deg);
  border-radius: 0 0 0 3px;
}

.message-item.is-self .message-bubble::before {
  content: '';
  position: absolute;
  right: -7px;
  top: 14px;
  width: 14px;
  height: 14px;
  background: var(--primary-600);
  transform: rotate(45deg);
  border-radius: 0 0 3px 0;
}

/* ── 消息状态 ──────────────────────────────── */
.message-status {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-1);
  font-size: 11px;
}

.message-item.is-self .message-status { justify-content: flex-end; }
.message-status.sending { color: var(--color-text-muted); }
.message-status.failed { color: var(--danger-500); }

/* ── 输入区域 ──────────────────────────────── */
.chat-input {
  padding: var(--space-4) var(--space-6);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--color-surface);
  border: 1.5px solid var(--gray-200);
  border-radius: var(--radius-xl);
  padding: var(--space-3);
  transition: all var(--duration-normal) var(--ease-out);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.input-wrapper:focus-within {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px var(--primary-100), 0 4px 12px rgba(0, 0, 0, 0.06);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--space-1);
}

.toolbar-left { display: flex; gap: var(--space-1); }

.toolbar-btn {
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
  color: var(--gray-500);
}

.toolbar-btn:hover {
  background: var(--primary-50);
  color: var(--primary-600);
  transform: scale(1.1);
}

.emoji-picker {
  background: var(--color-surface);
  border: 1px solid var(--gray-100);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  max-height: 220px;
  overflow-y: auto;
  animation: pickerSlideUp 0.2s var(--ease-out);
}

@keyframes pickerSlideUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.emoji-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: var(--space-1); }

.emoji-item {
  font-size: 24px;
  cursor: pointer;
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
  text-align: center;
  user-select: none;
}

.emoji-item:hover {
  background: var(--primary-50);
  transform: scale(1.25);
}

.hidden-file-input { display: none; }

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: var(--radius-lg);
  border: none;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  transition: all var(--duration-fast) var(--ease-out);
  resize: none;
  background: transparent;
  box-shadow: none !important;
}

.input-wrapper :deep(.el-textarea__inner:focus) {
  box-shadow: none !important;
}

.input-actions { display: flex; justify-content: flex-end; align-items: center; }

.input-actions .el-button {
  border-radius: var(--radius-lg);
  padding: var(--space-2) var(--space-5);
  font-weight: var(--weight-medium);
  transition: all var(--duration-fast) var(--ease-out);
}

.input-actions .el-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.input-hint { font-size: var(--text-xs); color: var(--gray-400); }

/* ── 文件消息 ──────────────────────────────── */
.message-image {
  max-width: 280px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chat-image {
  width: 100%;
  max-height: 280px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out);
}

.chat-image:hover {
  transform: scale(1.02);
}

.message-file {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-lg);
  border: 1px solid var(--gray-100);
  min-width: 200px;
  max-width: 280px;
  transition: all var(--duration-fast) var(--ease-out);
}

.message-file:hover {
  background: var(--gray-100);
}

.message-item.is-self .message-file {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
}

.file-icon { flex-shrink: 0; color: var(--primary-500); }
.message-item.is-self .file-icon { color: var(--color-surface); }

.file-info { flex: 1; min-width: 0; }

.file-name {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--gray-800);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-item.is-self .file-name { color: var(--color-surface); }

.file-hint { margin: 2px 0 0; font-size: var(--text-xs); color: var(--color-text-muted); }
.message-item.is-self .file-hint { color: rgba(255, 255, 255, 0.75); }

.download-btn {
  flex-shrink: 0;
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
}

.download-btn:hover {
  background: var(--primary-50);
  transform: scale(1.1);
}

/* ── 响应式 ────────────────────────────────── */
@media (max-width: 768px) {
  .chat-header {
    padding: var(--space-3) var(--space-4);
  }

  .chat-messages {
    padding: var(--space-4) var(--space-5);
  }

  .chat-input {
    padding: var(--space-3) var(--space-4);
  }

  .message-body {
    max-width: 75%;
  }

  .input-wrapper {
    padding: var(--space-2);
  }
}

@media (max-width: 480px) {
  .chat-messages {
    padding: var(--space-3);
  }

  .message-body {
    max-width: 80%;
  }

  .emoji-grid {
    grid-template-columns: repeat(8, 1fr);
  }
}
</style>
