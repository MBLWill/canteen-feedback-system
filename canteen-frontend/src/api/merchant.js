import request from './request'

// 商家待办
export function listTodos() {
  return request.get('/merchant/todos')
}

export function doneTodo(id) {
  return request.put(`/merchant/todos/${id}/done`)
}

export function createInspectTodo(data) {
  return request.post('/merchant/todos', data)
}

// 商家留言
export function sendMessage(data) {
  return request.post('/merchant/messages', data)
}

export function listMyMessages() {
  return request.get('/merchant/messages')
}

export function listAllMessages(params) {
  return request.get('/merchant/messages/all', { params })
}

export function replyMessage(id, data) {
  return request.post(`/merchant/messages/${id}/reply`, data)
}
