import { http, unwrap } from '@/api'
import type { Room } from '@/types/room'

type RoomDTO = {
  id: number
  buildingId: number
  buildingName?: string
  name: string
  floor?: string
  capacity: number
  equipmentItems: string[]
  status: 'ENABLED' | 'DISABLED'
  remark?: string
}

function mapRoom(dto: RoomDTO): Room {
  return {
    id: dto.id,
    buildingId: dto.buildingId,
    buildingName: dto.buildingName,
    name: dto.name,
    floor: dto.floor,
    capacity: dto.capacity,
    equipment: dto.equipmentItems || [],
    status: dto.status,
    remark: dto.remark,
  }
}

export async function getRooms(params: {
  buildingId?: number
  floor?: string
  capacity?: number
  status?: 'ENABLED' | 'DISABLED'
  keyword?: string
}): Promise<Room[]> {
  const dtos = await unwrap<RoomDTO[]>(http.get('/rooms', { params }))
  return dtos.map(mapRoom)
}

export async function getAvailableRooms(params: {
  startTime: string
  endTime: string
  buildingId?: number
  capacity?: number
}): Promise<Room[]> {
  const dtos = await unwrap<RoomDTO[]>(http.get('/rooms/available', { params }))
  return dtos.map(mapRoom)
}

export async function getRoom(id: number): Promise<Room> {
  const dto = await unwrap<RoomDTO>(http.get(`/rooms/${id}`))
  return mapRoom(dto)
}

export function createRoom(data: {
  buildingId: number
  name: string
  floor?: string
  capacity: number
  equipmentItems: string[]
  remark?: string
  status?: 'ENABLED' | 'DISABLED'
}) {
  return unwrap(http.post('/admin/rooms', data))
}

export function updateRoom(
  id: number,
  data: {
    buildingId: number
    name: string
    floor?: string
    capacity: number
    equipmentItems: string[]
    remark?: string
    status?: 'ENABLED' | 'DISABLED'
  },
) {
  return unwrap(http.put(`/admin/rooms/${id}`, data))
}

export function updateRoomStatus(id: number, status: 'ENABLED' | 'DISABLED') {
  return unwrap(http.put(`/admin/rooms/${id}/status`, { status }))
}

