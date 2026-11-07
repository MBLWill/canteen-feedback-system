import { defineStore } from 'pinia'
import { getCart } from '@/api/cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0,
    totalAmount: '0.00',
    stallCount: 0,
    drawerOpen: false
  }),

  getters: {
    empty: state => state.count === 0
  },

  actions: {
    async refresh() {
      try {
        const res = await getCart()
        const data = res.data || {}
        this.count = data.totalQuantity || 0
        this.totalAmount = data.totalAmount ?? '0.00'
        this.stallCount = (data.groups || []).length
      } catch {
        this.count = 0
        this.totalAmount = '0.00'
        this.stallCount = 0
      }
    },
    openDrawer() {
      this.drawerOpen = true
      this.refresh()
    },
    closeDrawer() {
      this.drawerOpen = false
    },
    toggleDrawer() {
      if (this.drawerOpen) this.closeDrawer()
      else this.openDrawer()
    },
    reset() {
      this.count = 0
      this.totalAmount = '0.00'
      this.stallCount = 0
      this.drawerOpen = false
    }
  }
})
