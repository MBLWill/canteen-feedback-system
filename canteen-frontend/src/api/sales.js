import request from './request'

export function getSalesOverview() {
  return request.get('/merchant/sales/overview')
}

export function getDailyIncome(days) {
  return request.get('/merchant/sales/daily', { params: { days } })
}

export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/files/upload', form)
}
