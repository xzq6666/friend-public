import request from '../utils/request'

/**
 * 数据导出 API
 */

/**
 * 获取导出数据预览（记录数）
 * @param {string} type - 导出类型
 * @param {Object} params - 筛选参数
 * @returns {Promise<{ count: number }>}
 */
export const getExportPreview = (type, params = {}) => {
  return request.get(`/export/${type}/preview`, {
    params,
    skipErrorNotification: true,
  })
}

/**
 * 执行导出（返回 Blob）
 * @param {string} type - 导出类型
 * @param {Object} params - 导出参数（format、fields、dateRange 等）
 * @returns {Promise<Blob>}
 */
export const doExport = async (type, params = {}) => {
  const token = localStorage.getItem('token')
  const queryParams = new URLSearchParams()
  if (params.format) queryParams.set('format', params.format)
  if (params.fields) queryParams.set('fields', params.fields.join(','))
  if (params.startDate) queryParams.set('startDate', params.startDate)
  if (params.endDate) queryParams.set('endDate', params.endDate)

  const url = `/api/export/${type}?${queryParams.toString()}`
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    const err = await response.json().catch(() => ({}))
    throw new Error(err.error || '导出失败')
  }
  return response.blob()
}
