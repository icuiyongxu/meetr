export interface BookingOperationLog {
  id: number
  bookingId: number
  operationType: string
  operatorId?: string
  operatorName?: string
  content: string
  createdAtMs: number
}

export interface BookingDetail {
  booking: import('./booking').Booking
  operationLogs: BookingOperationLog[]
}
