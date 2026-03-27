import { unwrap, http } from './index'

export interface Equipment {
  id: number
  code: string
  name: string
  status: string
}

export async function getEquipments(): Promise<Equipment[]> {
  return unwrap(http.get<ApiResponseLike<Equipment[]>>('/equipments') as any)
}

export async function createEquipment(data: { code: string; name: string }) {
  return unwrap(http.post<ApiResponseLike<Equipment>>('/admin/equipments', data) as any)
}

export async function updateEquipment(id: number, data: { name?: string; status?: string }) {
  return unwrap(http.put<ApiResponseLike<Equipment>>(`/admin/equipments/${id}`, data) as any)
}

export async function deleteEquipment(id: number) {
  return unwrap(http.delete<ApiResponseLike<unknown>>(`/admin/equipments/${id}`) as any)
}

type ApiResponseLike<T> = {
  code: number
  message: string
  data: T
}
