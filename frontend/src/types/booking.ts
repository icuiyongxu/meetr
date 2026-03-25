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
  startTime: string
  endTime: string
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
  startTime: string
  endTime: string
  bookerName?: string
}

export interface ConflictCheckResponse {
  conflict: boolean
  alignedStartTime: string
  alignedEndTime: string
  conflictingBookings: BookingConflictDTO[]
}

export interface BookingResult {
  success: boolean
  booking?: Booking
  violations?: { code: string; message: string }[]
  conflicts?: BookingConflictDTO[]
}

