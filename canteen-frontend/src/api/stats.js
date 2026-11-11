import request from './request'

export function getOverview() {
  return request.get('/stats/overview')
}

export function getDishRanking(limit = 10, period = 'all') {
  return request.get('/stats/dish-ranking', { params: { limit, period } })
}

export function getStallGoodRate() {
  return request.get('/stats/stall-good-rate')
}

export function getStallTop(limit = 3) {
  return request.get('/stats/stall-top', { params: { limit } })
}

export function getRatingTrend(days = 7) {
  return request.get('/stats/rating-trend', { params: { days } })
}

export function getDishScatter(limit = 0) {
  return request.get('/stats/dish-scatter', { params: { limit } })
}

export function getStallScatter() {
  return request.get('/stats/stall-scatter')
}
