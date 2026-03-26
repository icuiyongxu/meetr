import { http, unwrap } from '@/api'
import type { Booking, BookingResult, ConflictCheckResponse } from '@/types/booking'

export function checkConflict(data: {
  roomId: number
  startTime: number
  endTime: number
  excludeBookingId?: number
}): Promise<ConflictCheckResponse> {
  return unwrap(http.post('/bookings/check-conflict', data))
}

export function createBooking(data: {
  roomId: number
  subject: string
  bookerId: string
  bookerName?: string
  startTime: number
  endTime: number
  attendeeCount: number
  remark?: string
}) {
  return unwrap<BookingResult>(http.post('/bookings', data))
}

export function updateBooking(
  id: number,
  data: {
    subject: string
    operatorId: string
    startTime: number
    endTime: number
    attendeeCount: number
    remark?: string
  },
) {
  return unwrap<BookingResult>(http.put(`/bookings/${id}`, data))
}

export function cancelBooking(id: number, operatorId: string) {
  return unwrap(http.post(`/bookings/${id}/cancel`, { operatorId }))
}

export function getBooking(id: number): Promise<Booking> {
  return unwrap(http.get(`/bookings/${id}`))
}

export function getMyBookings(params: { bookerId: string; page: number; size: number }) {
  return unwrap(http.get('/bookings/mine', { params }))
}

export function getTodayBookings(bookerId: string) {
  return unwrap(http.get('/bookings/today', { params: { bookerId } }))
}

/** 拉取指定会议室指定日期的全部预约（供日历视图） */
export function getBookingsByRoomAndDate(roomId: number, date: string) {
  // date: "2026-03-25" (北京时间当天 00:00:00 的 UTC 毫秒)
  // 使用北京时间偏移量 +08:00
  const dayMillis = new Date(date + 'T00:00:00+08:00').getTime()
  return unwrap<Booking[]>(http.get('/rooms/schedule', { params: { roomId, dayMillis } }))
}

