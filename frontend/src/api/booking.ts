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
  attendeeIds?: string[]
  remark?: string
  recurrenceType?: string
  recurrenceEndDate?: string
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
    attendeeIds?: string[]
    remark?: string
  },
) {
  return unwrap<BookingResult>(http.put(`/bookings/${id}`, data))
}

export function cancelBooking(id: number, operatorId: string, cancelSeries = false) {
  return unwrap(http.post(`/bookings/${id}/cancel`, { operatorId, cancelSeries }))
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

export function searchBookings(params: {
  bookerId: string
  keyword?: string
  status?: string
  startTimeFrom?: number
  startTimeTo?: number
  page: number
  size: number
}) {
  return unwrap<{ content: Booking[]; totalElements: number }>(http.get('/bookings/search', { params }))
}

export function countPendingBookings() {
  return unwrap<{ totalElements: number; content: Booking[] }>(http.get('/admin/bookings/pending', { params: { page: 0, size: 1 } }))
}

export function getPendingBookings(params: {
  buildingId?: number
  roomId?: number
  bookerId?: string
  keyword?: string
  startDateMs?: number
  endDateMs?: number
  page: number
  size: number
}) {
  return unwrap<{ content: Booking[]; totalElements: number }>(http.get('/admin/bookings/pending', { params }))
}

export function approveBooking(id: number, operatorId: string) {
  return unwrap<Booking>(http.put(`/admin/bookings/${id}/approve`, { operatorId }))
}

export function rejectBooking(id: number, operatorId: string) {
  return unwrap<Booking>(http.put(`/admin/bookings/${id}/reject`, { operatorId }))
}

// ── 系列预约 ──────────────────────────────────────────────

export interface SeriesBookingResponse {
  master: Booking
  instances: Booking[]
  totalCount: number
}

export interface UpdateFutureSeriesRequest {
  operatorId: string
  newStartTimeMs: number
  newEndTimeMs: number
  fromSeriesIndex: number
}

/** 获取某个预约所属系列的所有预约（主预约 + 子预约列表） */
export function getSeriesBookings(bookingId: number, bookerId: string) {
  return unwrap<SeriesBookingResponse>(http.get(`/bookings/${bookingId}/series`, { params: { bookerId } }))
}

/** 批量修改系列中从 fromSeriesIndex 开始的后续预约时间 */
export function updateFutureSeries(bookingId: number, data: UpdateFutureSeriesRequest) {
  return unwrap<SeriesBookingResponse>(http.put(`/bookings/${bookingId}/future`, data))
}
