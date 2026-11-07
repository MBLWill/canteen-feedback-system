const GRADIENTS = {
  荤菜: 'linear-gradient(135deg, #ffd0a6 0%, #ff9d5c 55%, #f97316 100%)',
  素菜: 'linear-gradient(135deg, #cdeecb 0%, #8fd694 55%, #4caf50 100%)',
  主食: 'linear-gradient(135deg, #ffe1a8 0%, #ffb066 55%, #ff9248 100%)',
  小吃: 'linear-gradient(135deg, #e5d4ff 0%, #b79bf0 55%, #8b5cf6 100%)',
  饮品: 'linear-gradient(135deg, #cfe9ff 0%, #7cc4f5 55%, #3b82f6 100%)',
  甜品: 'linear-gradient(135deg, #ffd9e8 0%, #ff9ec4 55%, #ec4899 100%)'
}

export function coverGradient(category) {
  return GRADIENTS[category] || GRADIENTS['主食']
}
