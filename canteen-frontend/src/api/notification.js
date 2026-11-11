import request from './request'

// 消息列表（时间倒序）
export function listNotifications(limit = 20) {
  return request.get('/notifications', { params: { limit } })
}

// 未读消息数
export function unreadCount() {
  return request.get('/notifications/unread-count')
}

// 全部标记已读
export function readAllNotifications() {
  return request.put('/notifications/read-all')
}
