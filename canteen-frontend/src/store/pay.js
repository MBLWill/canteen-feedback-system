import { defineStore } from 'pinia'
import { getPayStatus, mockPaySuccess } from '@/api/pay'

const POLL_INTERVAL = 3000
const POLL_MAX = 30

// 定时器放在模块作用域：任何组件卸载都不会中断正在进行的支付轮询
let timer = null
let ticks = 0

export const usePayStore = defineStore('pay', {
  state: () => ({
    visible: false,
    session: null,
    querying: false,
    mocking: false
  }),

  getters: {
    status: state => state.session?.status || 'CREATED',
    paid: state => state.session?.status === 'PAID',
    closed: state => state.session?.status === 'CLOSED'
  },

  actions: {
    open(session) {
      this.session = { ...session }
      this.visible = true
      if (!this.paid && this.session?.payNo) this.poll()
    },

    hide() {
      this.visible = false
    },

    dismiss() {
      this.stopPoll()
      this.visible = false
      this.session = null
    },

    poll() {
      this.stopPoll()
      ticks = 0
      timer = setInterval(async () => {
        ticks += 1
        if (ticks > POLL_MAX) {
          this.stopPoll()
          return
        }
        try {
          const status = await this.query()
          if (status && status !== 'CREATED') this.stopPoll()
        } catch {
          this.stopPoll()
        }
      }, POLL_INTERVAL)
    },

    stopPoll() {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
      ticks = 0
    },

    async query() {
      if (!this.session?.payNo) return null
      this.querying = true
      try {
        const res = await getPayStatus(this.session.payNo)
        this.session = { ...this.session, ...res.data }
        return res.data?.status
      } finally {
        this.querying = false
      }
    },

    async mockPay() {
      if (!this.session?.payNo) return
      this.mocking = true
      try {
        await mockPaySuccess(this.session.payNo)
        await this.query()
      } finally {
        this.mocking = false
      }
    }
  }
})
