import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PermissionCode } from '@/types/auth'

const USER_ID_KEY = 'meetr_user_id'
const USER_NAME_KEY = 'meetr_user_name'

export const useBookingStore = defineStore('booking', () => {
  const userId = ref<string>('')
  const userName = ref<string>('')
  const isAdmin = ref(false)
  const isLoggedIn = ref(false)
  const permissions = ref<PermissionCode[]>([])

  function ensureUser() {
    const existing = localStorage.getItem(USER_ID_KEY)
    if (existing) {
      userId.value = existing
      userName.value = localStorage.getItem(USER_NAME_KEY) || ''
    }
    // 不再自动生成 ID，登录页负责写入
  }

  /** 登录并拉取角色 */
  async function login(password?: string) {
    if (!userId.value) return
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: userId.value, password: password || null }),
      })
      if (res.ok) {
        const json = await res.json()
        const data = json?.data
        isAdmin.value = data?.roles?.includes('ADMIN') ?? false
        permissions.value = data?.permissions || []
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
        permissions.value = data?.permissions || []
        isLoggedIn.value = true
      }
    } catch {
      isAdmin.value = false
      permissions.value = []
    }
  }

  function setUserName(name: string) {
    userName.value = name
    localStorage.setItem(USER_NAME_KEY, name)
  }

  function hasPermission(code: PermissionCode) {
    if (isAdmin.value) return true
    return permissions.value.includes(code)
  }

  return {
    userId,
    userName,
    isAdmin,
    isLoggedIn,
    permissions,
    ensureUser,
    login,
    loadRole,
    setUserName,
    hasPermission,
  }
})

