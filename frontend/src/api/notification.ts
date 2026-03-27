import { unwrap, http } from './index'
import type { Notification } from '@/types/notification'

export function getNotifications(userId: string, page: number, size: number) {
  return unwrap<{ list: Notification[]; total: number }>(http.get('/notifications', {
    params: { userId, page, size },
  }))
}

export function getUnreadCount(userId: string) {
  return unwrap<number>(http.get('/notifications/unread-count', {
    params: { userId },
  }))
}

export function markAsRead(id: number) {
  return unwrap<void>(http.put(`/notifications/${id}/read`))
}

export function markAllRead(userId: string) {
  return unwrap<void>(http.put('/notifications/read-all', null, {
    params: { userId },
  }))
}
