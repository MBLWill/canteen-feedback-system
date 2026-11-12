import request from './request'

/** 菜品评分排行（美食探索页使用） */
export function getDishRanking(limit = 10, period = 'all') {
  return request.get('/stats/dish-ranking', { params: { limit, period } })
}

/** 数据大屏聚合数据（管理员端使用） */
export function getDashboard(days = 30) {
  return request.get('/stats/dashboard', { params: { days } })
}
