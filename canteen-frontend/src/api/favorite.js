import request from './request'

export function listFavorites() {
  return request.get('/favorites')
}

export function addFavorite(dishId) {
  return request.post(`/favorites/${dishId}`)
}

export function removeFavorite(dishId) {
  return request.delete(`/favorites/${dishId}`)
}
