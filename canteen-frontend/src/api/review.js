import request from './request'

export function pageReviews(params) {
  return request.get('/reviews', { params })
}

export function pageMyReviews(params) {
  return request.get('/reviews/my', { params })
}

export function submitReview(data) {
  return request.post('/reviews', data)
}

export function deleteReview(id) {
  return request.delete(`/reviews/${id}`)
}

export function updateReviewStatus(id, status) {
  return request.put(`/reviews/${id}/status`, null, { params: { status } })
}
