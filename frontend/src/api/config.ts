import { http, unwrap } from '@/api'
import type { RoomConfig } from '@/types/room'

export function getBookingRules(roomId?: number | null): Promise<RoomConfig> {
  return unwrap(http.get('/admin/booking-rules', { params: { roomId: roomId ?? undefined } }))
}

export function saveBookingRules(data: Partial<RoomConfig> & { roomId: number | null }) {
  return unwrap(http.post('/admin/booking-rules', data))
}

