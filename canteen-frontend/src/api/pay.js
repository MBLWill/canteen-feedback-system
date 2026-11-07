import request from './request'

export function createPay(orderId) {
  return request.post(`/pay/orders/${orderId}`)
}

export function getPayStatus(payNo) {
  return request.get(`/pay/${payNo}`)
}

export function mockPaySuccess(payNo) {
  return request.post(`/pay/${payNo}/mock-success`)
}
