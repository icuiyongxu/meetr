import { http, unwrap } from '@/api'
import type { Building } from '@/types/building'

export function getBuildings(): Promise<Building[]> {
  return unwrap(http.get('/buildings'))
}

export function createBuilding(data: Partial<Building>) {
  return unwrap(http.post('/admin/buildings', data))
}

export function updateBuilding(id: number, data: Partial<Building>) {
  return unwrap(http.put(`/admin/buildings/${id}`, data))
}

