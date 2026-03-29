import { http, unwrap } from '@/api'
import type { Booking, BookingResult, ConflictCheckResponse } from '@/types/booking'
import type { BookingDetail } from '@/types/booking-detail'

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

export function getBookingDetail(id: number): Promise<BookingDetail> {
  return unwrap(http.get(`/bookings/${id}/detail`))
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

/** 管理员全局预约搜索（bookerId 可选） */
export function searchAdminBookings(params: {
  bookerId?: string
  roomId?: number
  keyword?: string
  status?: string
  approvalStatus?: string
  startTimeFrom?: number
  startTimeTo?: number
  page: number
  size: number
}) {
  return unwrap<{ content: Booking[]; totalElements: number }>(http.get('/admin/bookings/search', { params }))
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

export function approveBooking(id: number, operatorId: string, remark?: string) {
  return unwrap<Booking>(http.put(`/admin/bookings/${id}/approve`, { operatorId, remark }))
}

export function rejectBooking(id: number, operatorId: string, remark?: string) {
  return unwrap<Booking>(http.put(`/admin/bookings/${id}/reject`, { operatorId, remark }))
}

export function batchApproveBooking(bookingIds: number[], operatorId: string, remark?: string) {
  return unwrap<{ successCount: number; skippedCount: number }>(
    http.put('/admin/bookings/batch-approve', { bookingIds, operatorId, remark }),
  )
}

export function batchRejectBooking(bookingIds: number[], operatorId: string, remark: string) {
  return unwrap<{ successCount: number; skippedCount: number }>(
    http.put('/admin/bookings/batch-reject', { bookingIds, operatorId, remark }),
  )
}

// ── 系列预约 ──────────────────────────────────────────────

export interface SeriesBookingResponse {
  master: Booking
  instances: Booking[]
  totalCount: number
}



/** 系列修改/取消范围 */
export type SeriesScope = 'ONCE' | 'FUTURE' | 'ALL'

export interface UpdateSeriesRequest {
  operatorId: string
  scope: SeriesScope
  subject?: string
  startTime?: number
  endTime?: number
  attendeeCount?: number
  remark?: string
}

export interface CancelSeriesRequest {
  operatorId: string
  scope: SeriesScope
}

/** 获取某个预约所属系列的所有预约（主预约 + 子预约列表） */
export function getSeriesBookings(bookingId: number, bookerId: string) {
  return unwrap<SeriesBookingResponse>(http.get(`/bookings/${bookingId}/series`, { params: { bookerId } }))
}

/** 统一修改系列（本次 / 后续 / 全部） */
export function updateSeries(bookingId: number, data: UpdateSeriesRequest) {
  return unwrap<SeriesBookingResponse>(http.put(`/bookings/${bookingId}/series`, data))
}

/** 统一取消系列（本次 / 后续 / 全部） */
export function cancelSeries(bookingId: number, data: CancelSeriesRequest) {
  return unwrap<SeriesBookingResponse>(http.post(`/bookings/${bookingId}/series-cancel`, data))
}

/** 批量修改系列中从 fromSeriesIndex 开始的后续预约时间（兼容旧接口） */
export function updateFutureSeries(bookingId: number, data: UpdateFutureSeriesRequest) {
  return unwrap<SeriesBookingResponse>(http.put(`/bookings/${bookingId}/future`, data))
}
