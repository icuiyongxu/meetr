import { http, unwrap } from '@/api'
import type { Booking, BookingResult, ConflictCheckResponse } from '@/types/booking'

export function checkConflict(data: {
  roomId: number
  startTime: string
  endTime: string
  excludeBookingId?: number
}): Promise<ConflictCheckResponse> {
  return unwrap(http.post('/bookings/check-conflict', data))
}

export function createBooking(data: {
  roomId: number
  subject: string
  bookerId: string
  bookerName?: string
  startTime: string
  endTime: string
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
    startTime: string
    endTime: string
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
  // 调用 GET /api/rooms/schedule?roomId=1&day=2026-03-26T00:00:00
  return unwrap<Booking[]>(http.get('/rooms/schedule', { params: { roomId, day: date + 'T00:00:00' } }))
}

