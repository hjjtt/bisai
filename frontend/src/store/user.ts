import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/types'
import { setToken, setUserInfo, clearAuth, getToken, getUserInfo } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(getUserInfo())
  const isLoggedIn = ref(!!getToken())

  function setLogin(tokenVal: string, user: UserInfo) {
    token.value = tokenVal
    userInfo.value = user
    isLoggedIn.value = true
    setToken(tokenVal)
    setUserInfo(user)
  }

  function logout() {
    token.value = null
    userInfo.value = null
    isLoggedIn.value = false
    clearAuth()
  }

  const role = () => userInfo.value?.role ?? null
  const isStudent = () => role() === 'STUDENT'
  const isTeacher = () => role() === 'TEACHER'
  const isAdmin = () => role() === 'ADMIN'

  return {
    token,
    userInfo,
    isLoggedIn,
    setLogin,
    logout,
    role,
    isStudent,
    isTeacher,
    isAdmin,
  }
})
