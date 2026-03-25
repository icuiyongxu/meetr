import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useBookingStore } from '@/stores/booking'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  // Placeholder for future token auth.
  // const token = ''
  // if (token) config.headers.Authorization = `Bearer ${token}`

  try {
    const store = useBookingStore()
    if (store.userId) {
      config.headers = config.headers || {}
      config.headers['X-Meetr-User-Id'] = store.userId
    }
  } catch {
    // ignore (Pinia not ready in some contexts)
  }

  return config
})

http.interceptors.response.use(
  (resp) => {
    const payload = resp.data as ApiResponse<unknown>
    if (!payload || typeof payload.code !== 'number') return resp
    if (payload.code !== 0) {
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || 'request failed'))
    }
    return resp
  },
  (err) => {
    const msg =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err?.message ||
      '网络错误'
    ElMessage.error(msg)
    return Promise.reject(err)
  },
)

export async function unwrap<T>(p: Promise<{ data: ApiResponse<T> }>) {
  const resp = await p
  return resp.data.data
}

