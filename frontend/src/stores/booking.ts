import { defineStore } from 'pinia'
import { ref } from 'vue'

const USER_ID_KEY = 'meetr_user_id'
const USER_NAME_KEY = 'meetr_user_name'

export const useBookingStore = defineStore('booking', () => {
  const userId = ref<string>('')
  const userName = ref<string>('')
  const isAdmin = ref(false)
  const isLoggedIn = ref(false)

  function ensureUser() {
    const existing = localStorage.getItem(USER_ID_KEY)
    if (existing) {
      userId.value = existing
      userName.value = localStorage.getItem(USER_NAME_KEY) || ''
    }
    // 不再自动生成 ID，登录页负责写入
  }

  /** 登录（自动注册）并拉取角色 */
  async function login() {
    if (!userId.value) return
    try {
      const res = await fetch(`/api/auth/login?userId=${encodeURIComponent(userId.value)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
      })
      if (res.ok) {
        const json = await res.json()
        const data = json?.data
        isAdmin.value = data?.roles?.includes('ADMIN') ?? false
        isLoggedIn.value = true
        if (data?.name) {
          userName.value = data.name
          localStorage.setItem(USER_NAME_KEY, data.name)
        }
      }
    } catch {}
  }

  /** 拉取当前用户角色 */
  async function loadRole() {
    if (!userId.value) return
    try {
      const res = await fetch(`/api/auth/me?userId=${encodeURIComponent(userId.value)}`, {
        credentials: 'include',
      })
      if (res.ok) {
        const json = await res.json()
        const data = json?.data
        isAdmin.value = data?.roles?.includes('ADMIN') ?? false
        isLoggedIn.value = true
      }
    } catch {
      isAdmin.value = false
    }
  }

  function setUserName(name: string) {
    userName.value = name
    localStorage.setItem(USER_NAME_KEY, name)
  }

  return {
    userId,
    userName,
    isAdmin,
    isLoggedIn,
    ensureUser,
    login,
    loadRole,
    setUserName,
  }
})

