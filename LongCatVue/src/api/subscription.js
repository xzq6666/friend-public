import request from '../utils/request'

/**
 * 职位订阅 API
 */

// 创建订阅
export function createSubscription(data) {
  return request({
    url: '/api/subscription',
    method: 'post',
    data
  })
}

// 更新订阅
export function updateSubscription(id, data) {
  return request({
    url: `/api/subscription/${id}`,
    method: 'put',
    data
  })
}

// 删除订阅
export function deleteSubscription(id) {
  return request({
    url: `/api/subscription/${id}`,
    method: 'delete'
  })
}

// 获取我的订阅列表
export function getMySubscriptions() {
  return request({
    url: '/api/subscription/my-subscriptions',
    method: 'get'
  })
}

// 激活/停用订阅
export function toggleSubscription(id, isActive) {
  return request({
    url: `/api/subscription/${id}/toggle`,
    method: 'put',
    params: { isActive }
  })
}

// 手动触发匹配
export function triggerMatch(id) {
  return request({
    url: `/api/subscription/${id}/trigger-match`,
    method: 'post'
  })
}

// 获取推送历史
export function getPushHistory(id) {
  return request({
    url: `/api/subscription/${id}/push-history`,
    method: 'get'
  })
}

/**
 * 简历版本 API
 */

// 创建新版本
export function createResumeVersion(resumeId, tag, note) {
  return request({
    url: `/api/resume-version/${resumeId}`,
    method: 'post',
    params: { tag, note }
  })
}

// 获取简历的所有版本
export function getResumeVersions(resumeId) {
  return request({
    url: `/api/resume-version/resume/${resumeId}`,
    method: 'get'
  })
}

// 获取版本详情
export function getVersionDetail(versionId) {
  return request({
    url: `/api/resume-version/${versionId}`,
    method: 'get'
  })
}

// 恢复到指定版本
export function restoreVersion(versionId) {
  return request({
    url: `/api/resume-version/${versionId}/restore`,
    method: 'post'
  })
}

// 对比两个版本
export function compareVersions(v1, v2) {
  return request({
    url: '/api/resume-version/compare',
    method: 'get',
    params: { v1, v2 }
  })
}

// 删除版本
export function deleteVersion(versionId) {
  return request({
    url: `/api/resume-version/${versionId}`,
    method: 'delete'
  })
}

/**
 * 求职进度看板 API
 */

// 获取看板数据
export function getBoardData() {
  return request({
    url: '/api/board/data',
    method: 'get'
  })
}

// 更新申请状态
export function updateApplicationStatus(applicationId, status) {
  return request({
    url: `/api/board/application/${applicationId}/status`,
    method: 'put',
    params: { status }
  })
}

// 获取统计数据
export function getBoardStatistics() {
  return request({
    url: '/api/board/statistics',
    method: 'get'
  })
}
