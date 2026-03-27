export interface Notification {
  id: number
  userId: string
  eventType: string
  title: string
  content: string
  bookingId: number | null
  roomId: number | null
  bookingStartTimeMs: number | null
  bookingEndTimeMs: number | null
  isRead: boolean
  readAt: number | null
  createdAtMs: number
}
