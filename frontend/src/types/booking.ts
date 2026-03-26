export type BookingStatus = 'BOOKED' | 'CANCELED' | 'FINISHED'
export type ApprovalStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'

export interface BookingAttendee {
  id?: string
  name?: string
}

export interface Booking {
  id: number
  roomId: number
  roomName?: string
  buildingId?: number
  buildingName?: string
  subject: string
  bookerId: string
  bookerName?: string
  /** UTC 毫秒时间戳 */
  startTime: number
  /** UTC 毫秒时间戳 */
  endTime: number
  attendeeCount: number
  status: BookingStatus
  approvalStatus: ApprovalStatus
  remark?: string
  version: number
  attendees: BookingAttendee[]
}

export interface BookingConflictDTO {
  id: number
  subject: string
  /** UTC 毫秒时间戳 */
  startTime: number
  /** UTC 毫秒时间戳 */
  endTime: number
  bookerName?: string
}

export interface ConflictCheckResponse {
  conflict: boolean
  /** UTC 毫秒时间戳 */
  alignedStartTime: number
  /** UTC 毫秒时间戳 */
  alignedEndTime: number
  conflictingBookings: BookingConflictDTO[]
}

export interface BookingResult {
  success: boolean
  booking?: Booking
  violations?: { code: string; message: string }[]
  conflicts?: BookingConflictDTO[]
}

