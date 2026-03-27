import { unwrap, http } from './index'

export interface KioskBooking {
  id: number
  subject: string
  bookerName: string
  startTimeMs: number
  endTimeMs: number
  attendeeCount: number | null
  remark: string | null
  status: 'IN_PROGRESS' | 'UPCOMING' | 'ENDED'
}

export interface KioskRoom {
  id: number
  name: string
  buildingName: string
}

export interface KioskResponse {
  room: KioskRoom
  date: string
  bookings: KioskBooking[]
}

export function getKioskData(roomId: number, dateMs?: number) {
  const params: Record<string, any> = {}
  if (dateMs) params.dateMs = dateMs
  return unwrap<KioskResponse>(http.get(`/kiosk/rooms/${roomId}`, { params }))
}
