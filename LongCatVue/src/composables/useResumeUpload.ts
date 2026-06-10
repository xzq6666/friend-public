import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadResumeFile, importResume } from '../api/resume'
import type {
  UploadPhase,
  UploadProgress,
  ResumeUploadResult,
  ResumeParseResult
} from '../types'

/**
 * 简历上传组合式函数
 * 管理文件上传、智能解析的完整流程状态
 *
 * @param {Object} options - 配置选项
 * @param {Function} options.onParsed - 解析完成回调
 * @param {Function} options.onError - 错误回调
 */
export function useResumeUpload(options = {}) {
  const { onParsed, onError } = options

  // ── 状态 ──────────────────────────────────
  const file = ref<File | null>(null)
  const fileName = ref('')
  const fileSize = ref(0)
  const isDragging = ref(false)
  const uploadResult = ref<ResumeUploadResult | null>(null)
  const parsedData = ref<ResumeParseResult | null>(null)

  const progress = reactive<UploadProgress>({
    phase: 'idle',
    percent: 0,
    message: ''
  })

  // ── 计算属性 ──────────────────────────────
  const isIdle = computed(() => progress.phase === 'idle')
  const isUploading = computed(() => progress.phase === 'uploading')
  const isParsing = computed(() => progress.phase === 'parsing')
  const isAnalyzing = computed(() => progress.phase === 'analyzing')
  const isSuccess = computed(() => progress.phase === 'success')
  const isError = computed(() => progress.phase === 'error')
  const isProcessing = computed(() =>
    progress.phase === 'uploading' ||
    progress.phase === 'parsing' ||
    progress.phase === 'analyzing'
  )

  // ── 常量 ──────────────────────────────────
  const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
  const ACCEPTED_TYPES = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
  const ACCEPTED_EXTENSIONS = ['.pdf', '.doc', '.docx']

  // ── 工具函数 ──────────────────────────────
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
  }

  const setPhase = (phase: UploadPhase, percent = 0, message = '') => {
    progress.phase = phase
    progress.percent = percent
    progress.message = message
  }

  const reset = () => {
    file.value = null
    fileName.value = ''
    fileSize.value = 0
    uploadResult.value = null
    parsedData.value = null
    setPhase('idle', 0, '')
  }

  // ── 文件验证 ──────────────────────────────
  const validateFile = (fileObj: File): boolean => {
    // 大小检查
    if (fileObj.size > MAX_FILE_SIZE) {
      ElMessage.error('文件大小不能超过 10MB')
      return false
    }
    if (fileObj.size === 0) {
      ElMessage.error('文件不能为空')
      return false
    }

    // 类型检查（通过扩展名，因为某些系统 MIME 可能不准确）
    const ext = '.' + fileObj.name.split('.').pop()?.toLowerCase()
    if (!ACCEPTED_EXTENSIONS.includes(ext)) {
      ElMessage.error('仅支持 PDF、DOC、DOCX 格式')
      return false
    }

    return true
  }

  // ── 核心上传逻辑 ──────────────────────────
  const handleFile = async (fileObj: File) => {
    if (!validateFile(fileObj)) return false

    file.value = fileObj
    fileName.value = fileObj.name
    fileSize.value = fileObj.size
    uploadResult.value = null
    parsedData.value = null

    try {
      // 阶段 1: 上传 + 智能解析
      setPhase('uploading', 0, '正在上传简历文件...')

      const result = await importResume(fileObj, (percent) => {
        // 上传占整体进度的 0-70%
        const overallPercent = Math.round(percent * 0.7)
        setPhase('uploading', overallPercent, `正在上传... ${percent}%`)
      })

      // 阶段 2: 服务端解析
      setPhase('parsing', 75, '正在解析简历内容...')

      // 模拟解析完成（实际解析已在服务端完成）
      await new Promise(resolve => setTimeout(resolve, 400))

      if (result && result.id) {
        uploadResult.value = result

        // 构建解析后的数据
        const skillsStr = (() => {
          try {
            const arr = JSON.parse(result.skills)
            return Array.isArray(arr) ? arr.join(', ') : result.skills
          } catch {
            return result.skills
          }
        })()

        const workExp = (() => {
          try {
            const arr = JSON.parse(result.workExperience)
            return Array.isArray(arr) ? arr : []
          } catch {
            return []
          }
        })()

        parsedData.value = {
          name: result.name || '',
          phone: result.phone || '',
          email: result.email || '',
          age: result.age || 25,
          education: result.education || '',
          categoryId: result.categoryId || null,
          skills: skillsStr,
          experience: result.experience || '',
          expectedSalary: result.expectedSalary || null,
          workExperience: workExp,
          selfIntroduction: result.selfIntroduction || ''
        }

        // 阶段 3: AI 分析（如果有）
        if (result.aiAnalysis) {
          setPhase('analyzing', 90, 'AI 正在分析简历...')
          await new Promise(resolve => setTimeout(resolve, 300))
        }

        setPhase('success', 100, '简历导入成功！')
        onParsed?.(parsedData.value, result)
        return true
      } else {
        throw new Error('未获取到解析结果')
      }
    } catch (error) {
      console.error('[useResumeUpload] 上传失败:', error)
      const errorMessage = error?.response?.data?.error || error?.message || '上传失败，请重试'
      setPhase('error', 0, errorMessage)
      onError?.(error)
      ElMessage.error(errorMessage)
      return false
    }
  }

  // ── 拖拽事件处理 ──────────────────────────
  const handleDragOver = (e: DragEvent) => {
    e.preventDefault()
    isDragging.value = true
  }

  const handleDragLeave = (e: DragEvent) => {
    e.preventDefault()
    isDragging.value = false
  }

  const handleDrop = async (e: DragEvent) => {
    e.preventDefault()
    isDragging.value = false

    const files = e.dataTransfer?.files
    if (files && files.length > 0) {
      await handleFile(files[0])
    }
  }

  // ── 文件选择处理 ──────────────────────────
  const handleFileSelect = async (e: Event) => {
    const target = e.target as HTMLInputElement
    const files = target.files
    if (files && files.length > 0) {
      await handleFile(files[0])
    }
    // 重置 input 以便重复选择同一文件
    target.value = ''
  }

  const triggerFileSelect = () => {
    const input = document.getElementById('resume-file-input') as HTMLInputElement
    input?.click()
  }

  return {
    // 状态
    file,
    fileName,
    fileSize,
    isDragging,
    uploadResult,
    parsedData,
    progress,

    // 计算属性
    isIdle,
    isUploading,
    isParsing,
    isAnalyzing,
    isSuccess,
    isError,
    isProcessing,

    // 工具
    formatFileSize,
    MAX_FILE_SIZE,
    ACCEPTED_EXTENSIONS,

    // 方法
    handleFile,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    handleFileSelect,
    triggerFileSelect,
    reset
  }
}
