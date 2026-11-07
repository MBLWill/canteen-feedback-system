import request from './request'

export function getCart() {
  return request.get('/cart')
}

export function addToCart(dishId, quantity = 1) {
  return request.post('/cart/items', { dishId, quantity })
}

export function updateCartQuantity(id, quantity) {
  return request.put(`/cart/items/${id}`, { quantity })
}

export function removeCartItem(id) {
  return request.delete(`/cart/items/${id}`)
}

export function clearCart() {
  return request.delete('/cart')
}

export function checkoutCart(data) {
  return request.post('/cart/checkout', data)
}
