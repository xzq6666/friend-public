import request from '../utils/request'

/**
 * 简历相关 API
 */

/**
 * 获取我的简历列表
 * @returns {Promise<ResumeSummary[]>}
 */
export const getMyResumes = () => {
  return request.get('/resume/my/list', { skipErrorNotification: true })
}

/**
 * 获取单份简历详情
 * @param {number} id - 简历ID
 * @returns {Promise<ResumeDetail>}
 */
export const getResumeById = (id) => {
  return request.get(`/resume/${id}`)
}

/**
 * 获取当前用户的默认简历
 * @returns {Promise<ResumeDetail>}
 */
export const getMyResume = () => {
  return request.get('/resume/my')
}

/**
 * 创建简历
 * @param {Object} data - 简历数据
 * @returns {Promise<ResumeDetail>}
 */
export const createResume = (data) => {
  return request.post('/resume', data)
}

/**
 * 更新简历
 * @param {number} id - 简历ID
 * @param {Object} data - 简历数据
 * @returns {Promise<ResumeDetail>}
 */
export const updateResume = (id, data) => {
  return request.put(`/resume/${id}`, data)
}

/**
 * 删除简历
 * @param {number} id - 简历ID
 * @returns {Promise<void>}
 */
export const deleteResume = (id) => {
  return request.delete(`/resume/${id}`)
}

/**
 * 设置默认简历
 * @param {number} id - 简历ID
 * @returns {Promise<void>}
 */
export const setDefaultResume = (id) => {
  return request.put(`/resume/${id}/set-default`)
}

/**
 * 复制简历
 * @param {number} id - 简历ID
 * @returns {Promise<ResumeDetail>}
 */
export const copyResume = (id) => {
  return request.post(`/resume/${id}/copy`)
}

/**
 * 上传简历附件
 * @param {File} file - 文件对象
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise<{ fileUrl: string, fileName: string }>}
 */
export const uploadResumeFile = (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload', formData, {
    headers: { 'Content-Type': undefined },
    onUploadProgress: onProgress
      ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
      : undefined
  })
}

/**
 * 智能导入简历（解析文件内容并填充表单）
 * @param {File} file - 文件对象
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise<ResumeDetail>}
 */
export const importResume = (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
      ? (e) => onProgress(Math.round((e.loaded * 100) / e.total))
      : undefined
  })
}

/**
 * AI 分析简历
 * @param {number} resumeId - 简历ID
 * @returns {Promise<string>} JSON string of AI analysis result
 */
export const analyzeResumeAi = (resumeId) => {
  return request.post(`/resume/${resumeId}/ai-analyze`)
}

/**
 * 获取 AI 分析结果（从缓存或重新分析）
 * @param {number} resumeId - 简历ID
 * @returns {Promise<string>}
 */
export const getResumeAiAnalysis = (resumeId) => {
  return request.get(`/resume/${resumeId}/ai-analysis`, {
    skipErrorNotification: true
  })
}
