import { http, unwrap } from '@/api'
import type { Building } from '@/types/building'

export interface RoomUsage {
  roomId: number
  roomName: string
  buildingId: number
  buildingName: string
  totalBookings: number
  totalMinutes: number
  usagePercent: number
  canceledCount: number
  pendingCount: number
}

export interface BookingRecord {
  id: number
  subject: string
  roomId: number
  roomName: string
  buildingId: number
  buildingName: string
  bookerId: string
  bookerName: string
  startTime: number
  endTime: number
  durationMinutes: number
  status: string
  approvalStatus: string
  createdAtMs: number
}

export interface UserUsage {
  bookerId: string
  bookerName: string
  totalBookings: number
  validBookings: number
  canceledCount: number
  rejectedCount: number
  totalMinutes: number
}

export function getRoomUsage(params: {
  startDate: string
  endDate: string
  buildingId?: number
}) {
  return unwrap<RoomUsage[]>(http.get('/admin/reports/room-usage', { params }))
}

export function getBookingRecords(params: {
  buildingIds?: string
  roomIds?: string
  bookerId?: string
  keyword?: string
  status?: string
  approvalStatus?: string
  startFromMs?: number
  startToMs?: number
  page: number
  size: number
}) {
  return unwrap<{ content: BookingRecord[]; totalElements: number }>(
    http.get('/admin/reports/booking-records', { params }),
  )
}

export function getUserUsage(params: { startDate: string; endDate: string }) {
  return unwrap<UserUsage[]>(http.get('/admin/reports/user-usage', { params }))
}
