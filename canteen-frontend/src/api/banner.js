import request from './request'

export function listBanners() {
  return request.get('/banners/list')
}

export function pageBanners(params) {
  return request.get('/banners', { params })
}

export function createBanner(data) {
  return request.post('/banners', data)
}

export function updateBanner(id, data) {
  return request.put(`/banners/${id}`, data)
}

export function deleteBanner(id) {
  return request.delete(`/banners/${id}`)
}
