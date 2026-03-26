import { unwrap, http } from './index'

export interface Equipment {
  id: number
  code: string
  name: string
  status: string
}

export async function getEquipments(): Promise<Equipment[]> {
  return unwrap(http.get<Equipment[]>('/equipments'))
}

export async function createEquipment(data: { code: string; name: string }) {
  return unwrap(http.post<Equipment>('/admin/equipments', data))
}

export async function updateEquipment(id: number, data: { name?: string; status?: string }) {
  return unwrap(http.put<Equipment>(`/admin/equipments/${id}`, data))
}

export async function deleteEquipment(id: number) {
  return unwrap(http.delete(`/admin/equipments/${id}`))
}
