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
    /** 当前展示的那一笔支付单 */
    session: null,
    /** 等待支付队列（跨档口拆单会产生多笔） */
    queue: [],
    /** 已成功支付笔数（用于多笔场景提示） */
    paidCount: 0,
    /** 本批支付单总数 */
    totalCount: 0,
    querying: false,
    mocking: false
  }),

  getters: {
    status: state => state.session?.status || 'CREATED',
    paid: state => state.session?.status === 'PAID',
    closed: state => state.session?.status === 'CLOSED',
    /** 当前是否还有待处理的支付单 */
    hasNext: state => state.queue.length > 0,
    /** 本批是否全部支付完成 */
    allPaid: state => state.totalCount > 0 && state.paidCount >= state.totalCount && state.queue.length === 0
  },

  actions: {
    /** 单笔支付（兼容旧调用） */
    open(session) {
      this.openBatch([session])
    },

    /** 批量支付：跨档口拆单产生的多笔支付单，逐笔支付 */
    openBatch(list) {
      const sessions = (Array.isArray(list) ? list : []).map(s => ({
        payNo: s.payNo,
        orderId: s.orderId,
        amount: s.amount,
        channel: s.channel,
        payUrl: s.payUrl,
        status: s.paid ? 'PAID' : 'CREATED'
      }))
      if (!sessions.length) return
      this.queue = sessions
      this.totalCount = sessions.length
      this.paidCount = 0
      this.visible = true
      this.next()
    },

    /** 从队列取下一笔未支付的，作为当前 session */
    next() {
      if (!this.queue.length) {
        return
      }
      // 找到第一笔未支付的
      const idx = this.queue.findIndex(s => s.status !== 'PAID')
      if (idx === -1) {
        this.session = null
        return
      }
      this.session = { ...this.queue[idx] }
      if (this.session.status !== 'PAID' && this.session.payNo) this.poll()
    },

    /** 当前这笔支付成功后的回调：累计并切下一笔 */
    advance() {
      if (this.session && this.session.payNo) {
        this.paidCount += 1
        const idx = this.queue.findIndex(s => s.payNo === this.session.payNo)
        if (idx !== -1) this.queue[idx].status = 'PAID'
      }
      this.stopPoll()
      // 还有未支付的继续下一笔，否则全部完成
      if (this.queue.some(s => s.status !== 'PAID')) {
        this.next()
      } else {
        this.session = null
      }
    },

    hide() {
      this.visible = false
    },

    dismiss() {
      this.stopPoll()
      this.visible = false
      this.session = null
      this.queue = []
      this.totalCount = 0
      this.paidCount = 0
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
        const data = res.data || {}
        // 更新队列中对应这笔的状态，保持多笔同步
        const idx = this.queue.findIndex(s => s.payNo === this.session.payNo)
        if (idx !== -1) this.queue[idx].status = data.status
        this.session = { ...this.session, ...data }
        return data.status
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
