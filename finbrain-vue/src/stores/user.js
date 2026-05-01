import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import router from '@/router'

const getStorage = (rememberMe) => rememberMe ? localStorage : sessionStorage

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || sessionStorage.getItem('token') || '')
  const userInfo = ref(null)

  const login = async (loginForm, rememberMe = false) => {
    const res = await loginApi(loginForm)
    const storage = getStorage(rememberMe)
    
    token.value = res.data.token
    storage.setItem('token', res.data.token)
    storage.setItem('userId', res.data.userId)
    await fetchUserInfo()
    
    if (res.data.role === 'admin') {
      router.push('/admin/dashboard')
    } else {
      router.push('/dashboard')
    }
    return res
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (e) {
      console.error('logout error', e)
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userId')
    router.push('/login')
  }

  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
    } catch (e) {
      console.error('fetch user info error', e)
    }
  }

  return {
    token,
    userInfo,
    login,
    logout,
    fetchUserInfo
  }
})
