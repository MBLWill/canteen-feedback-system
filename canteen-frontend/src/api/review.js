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

// 商家端：本档口学生评价
export function getMerchantReviews(params) {
  return request.get('/reviews/merchant', { params })
}

export function replyReview(id, data) {
  return request.put(`/reviews/${id}/reply`, data)
}

// 点赞 / 取消点赞
export function likeReview(id) {
  return request.post(`/reviews/${id}/like`)
}

// 学生对评价的追问/回答
export function studentReply(id, data) {
  return request.post(`/reviews/${id}/student-reply`, data)
}

// 查询某评价的学生回复列表
export function getReplies(id) {
  return request.get(`/reviews/${id}/replies`)
}

// 是否可评价该菜品（需下过单）
export function canReview(dishId) {
  return request.get('/reviews/can-review', { params: { dishId } })
}
