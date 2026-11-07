import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getProfile as getProfileApi } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'

const USER_KEY = 'canteen_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),

  getters: {
    isLogin: state => !!state.token,
    isAdmin: state => !!state.userInfo && state.userInfo.role === 'ADMIN',
    isMerchant: state => !!state.userInfo && state.userInfo.role === 'MERCHANT',
    userId: state => (state.userInfo ? state.userInfo.id : null),
    nickname: state => (state.userInfo ? state.userInfo.realName : '')
  },

  actions: {
    async login(payload) {
      const body = await loginApi(payload)
      this.token = body.data.token
      this.userInfo = body.data.userInfo
      setToken(this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      return this.userInfo
    },

    async register(payload) {
      await registerApi(payload)
    },

    async fetchProfile() {
      const body = await getProfileApi()
      this.userInfo = body.data
      localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      return this.userInfo
    },

    logout() {
      this.token = ''
      this.userInfo = null
      clearToken()
      localStorage.removeItem(USER_KEY)
    }
  }
})
