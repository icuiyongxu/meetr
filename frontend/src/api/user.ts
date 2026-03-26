import { unwrap, http } from './index'

export interface UserItem {
  id: number
  userId: string
  name: string
  status: string
  roles: string[]
}

export async function getUsers(): Promise<UserItem[]> {
  return unwrap(http.get<UserItem[]>('/users'))
}

export async function getUser(userId: string): Promise<UserItem> {
  return unwrap(http.get<UserItem>(`/users/${userId}`))
}

export async function createUser(data: { userId: string; name: string; password: string }) {
  return unwrap(http.post<UserItem>('/admin/users', data))
}

export async function updateUser(id: number, data: { name?: string; password?: string; status?: string }) {
  return unwrap(http.put<UserItem>(`/admin/users/${id}`, data))
}

export async function setUserStatus(id: number, status: string) {
  return unwrap(http.put<UserItem>(`/admin/users/${id}/status`, { status }))
}

export async function deleteUser(id: number) {
  return unwrap(http.delete(`/admin/users/${id}`))
}

export async function assignRoles(id: number, roleCodes: string[]) {
  return unwrap(http.put(`/admin/users/${id}/roles`, { roleCodes }))
}
