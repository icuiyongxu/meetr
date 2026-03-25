import { defineStore } from 'pinia'
import { ref } from 'vue'

const USER_ID_KEY = 'meetr_user_id'
const USER_NAME_KEY = 'meetr_user_name'

export const useBookingStore = defineStore('booking', () => {
  const userId = ref<string>('')
  const userName = ref<string>('')

  function ensureUser() {
    const existing = localStorage.getItem(USER_ID_KEY)
    if (existing) {
      userId.value = existing
    } else {
      const id = `user_${Math.random().toString(36).slice(2, 10)}`
      localStorage.setItem(USER_ID_KEY, id)
      userId.value = id
    }
    userName.value = localStorage.getItem(USER_NAME_KEY) || ''
  }

  function setUserName(name: string) {
    userName.value = name
    localStorage.setItem(USER_NAME_KEY, name)
  }

  return {
    userId,
    userName,
    ensureUser,
    setUserName,
  }
})

