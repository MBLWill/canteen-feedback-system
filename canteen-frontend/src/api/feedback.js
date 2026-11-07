import request from './request'

export function pageFeedbacks(params) {
  return request.get('/feedbacks', { params })
}

export function createFeedback(data) {
  return request.post('/feedbacks', data)
}

export function replyFeedback(id, data) {
  return request.put(`/feedbacks/${id}/reply`, data)
}

export function updateFeedbackStatus(id, status) {
  return request.put(`/feedbacks/${id}/status`, null, { params: { status } })
}

export function deleteFeedback(id) {
  return request.delete(`/feedbacks/${id}`)
}
