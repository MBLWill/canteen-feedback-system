import request from './request'

export function pageMyOrders(params) {
  return request.get('/orders/my', { params })
}

export function pageOrders(params) {
  return request.get('/orders', { params })
}

export function pageStallOrders(params) {
  return request.get('/orders/stall', { params })
}

export function getOrder(id) {
  return request.get(`/orders/${id}`)
}

export function updateOrderStatus(id, status) {
  return request.put(`/orders/${id}/status`, null, { params: { status } })
}
