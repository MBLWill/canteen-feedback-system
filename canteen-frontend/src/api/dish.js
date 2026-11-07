import request from './request'

export function pageDishes(params) {
  return request.get('/dishes', { params })
}

export function getDishDetail(id) {
  return request.get(`/dishes/${id}`)
}

export function listCategories() {
  return request.get('/dishes/categories')
}

export function createDish(data) {
  return request.post('/dishes', data)
}

export function updateDish(id, data) {
  return request.put(`/dishes/${id}`, data)
}

export function deleteDish(id) {
  return request.delete(`/dishes/${id}`)
}
