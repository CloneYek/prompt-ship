import { defineStore } from 'pinia'
import { getLoginUser, userLogout } from '@/api/userController'

type LoginUser = API.LoginUserVO | null

export const useUserStore = defineStore('user', {
  state: () => ({
    loginUser: null as LoginUser,
    loaded: false,
  }),
  actions: {
    setLoginUser(user: LoginUser) {
      this.loginUser = user
      this.loaded = true
    },
    async fetchLoginUser() {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        this.setLoginUser(res.data.data)
      } else {
        this.setLoginUser(null)
      }
      return this.loginUser
    },
    async logout() {
      await userLogout()
      this.setLoginUser(null)
    },
  },
})
