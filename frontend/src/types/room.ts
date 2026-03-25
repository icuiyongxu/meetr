export type RoomStatus = 'ENABLED' | 'DISABLED'

export interface Room {
  id: number
  buildingId: number
  buildingName?: string
  name: string
  floor?: string
  capacity: number
  equipment: string[]
  status: RoomStatus
  remark?: string
}

export interface RoomConfig {
  id: number
  roomId: number | null
  resolution: number
  defaultDuration: number
  morningStarts: string
  eveningEnds: string
  minBookAheadMinutes: number
  maxBookAheadDays: number
  minDurationMinutes: number
  maxDurationMinutes: number
  maxPerDay: number
  maxPerWeek: number
  approvalRequired: boolean
  status?: RoomStatus
}

