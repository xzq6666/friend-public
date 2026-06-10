import request from '../utils/request'

/**
 * 通知 API
 */
export const getUnreadCount = () => {
  return request.get('/notification/unread-count', {
    skipGlobalLoading: true,
    skipErrorNotification: true
  })
}

export const getNotifications = (params = {}) => {
  return request.get('/notification', {
    params,
    skipGlobalLoading: true
  })
}

export const markAsRead = (id) => {
  return request.put(`/notification/${id}/read`, null, {
    skipGlobalLoading: true
  })
}

export const markAllRead = () => {
  return request.put('/notification/read-all', null, {
    skipGlobalLoading: true
  })
}

export const deleteNotification = (id) => {
  return request.delete(`/notification/${id}`, {
    skipGlobalLoading: true
  })
}
