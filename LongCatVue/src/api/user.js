import request from '../utils/request'

/**
 * 获取通知偏好设置
 * @returns {Promise<Object>} 偏好设置
 */
export const getNotificationPrefs = () => {
  return request.get('/user/notification-prefs', {
    skipErrorNotification: true
  })
}

/**
 * 更新通知偏好设置
 * @param {Object} prefs - 偏好设置
 * @returns {Promise<Object>} 更新后的设置
 */
export const updateNotificationPrefs = (prefs) => {
  return request.put('/user/notification-prefs', prefs)
}
