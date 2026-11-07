import request from './request'

export function listStalls(params) {
  return request.get('/stalls', { params })
}

export function getStall(id) {
  return request.get(`/stalls/${id}`)
}

export function createStall(data) {
  return request.post('/stalls', data)
}

export function updateStall(id, data) {
  return request.put(`/stalls/${id}`, data)
}

export function deleteStall(id) {
  return request.delete(`/stalls/${id}`)
}

export function resetMerchantPassword(id, data) {
  return request.put(`/stalls/${id}/merchant-password`, data)
}

export function listWindowNos(params) {
  return request.get('/stalls/window-nos', { params })
}
