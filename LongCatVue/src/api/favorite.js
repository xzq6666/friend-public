import request from '../utils/request'

/**
 * 收藏夹文件夹 API
 */

/** 获取文件夹列表 */
export const getFolders = () => {
  return request.get('/favorite/folders', { skipErrorNotification: true })
}

/** 创建文件夹 */
export const createFolder = (name) => {
  return request.post('/favorite/folders', { name })
}

/** 重命名文件夹 */
export const renameFolder = (id, name) => {
  return request.put(`/favorite/folders/${id}`, { name })
}

/** 删除文件夹 */
export const deleteFolder = (id) => {
  return request.delete(`/favorite/folders/${id}`)
}

/** 移动收藏到文件夹 */
export const moveToFolder = (favoriteId, folderId) => {
  return request.put(`/favorite/${favoriteId}/folder`, { folderId })
}

/** 按文件夹获取收藏职位 */
export const getFavoriteJobs = (folderId, config = {}) => {
  const params = folderId ? { folderId } : {}
  return request.get('/favorite/jobs', { ...config, params })
}

/** 按文件夹获取收藏简历 */
export const getFavoriteResumes = (folderId, config = {}) => {
  const params = folderId ? { folderId } : {}
  return request.get('/favorite/resumes', { ...config, params })
}
