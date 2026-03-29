import { unwrap, http } from './index'

export interface UserItem {
  id: number
  userId: string
  name: string
  status: string
  roles: string[]
  email?: string
  emailEnabled?: boolean
}

export interface UserProfile {
  id: number
  userId: string
  name: string
  status: string
  roles: string[]
  email?: string
  emailEnabled?: boolean
}

export async function getUserProfile(userId: string): Promise<UserProfile> {
  return unwrap(http.get('/auth/me', { params: { userId } }))
}

export async function updateProfile(data: { userId: string; name?: string; password?: string; email?: string; emailEnabled?: boolean }) {
  return unwrap(http.put('/auth/profile', data))
}

export async function getUsers(): Promise<UserItem[]> {
  return unwrap(http.get<ApiResponseLike<UserItem[]>>('/users') as any)
}

export async function getUser(userId: string): Promise<UserItem> {
  return unwrap(http.get<ApiResponseLike<UserItem>>(`/users/${userId}`) as any)
}

export async function createUser(data: { userId: string; name: string; password: string }) {
  return unwrap(http.post<ApiResponseLike<UserItem>>('/admin/users', data) as any)
}

export async function updateUser(id: number, data: { name?: string; password?: string; status?: string; email?: string; emailEnabled?: boolean }) {
  return unwrap(http.put<ApiResponseLike<UserItem>>(`/admin/users/${id}`, data) as any)
}

export async function setUserStatus(id: number, status: string) {
  return unwrap(http.put<ApiResponseLike<UserItem>>(`/admin/users/${id}/status`, { status }) as any)
}

export async function deleteUser(id: number) {
  return unwrap(http.delete<ApiResponseLike<unknown>>(`/admin/users/${id}`) as any)
}

export async function assignRoles(id: number, roleCodes: string[]) {
  return unwrap(http.put<ApiResponseLike<unknown>>(`/admin/users/${id}/roles`, { roleCodes }) as any)
}

type ApiResponseLike<T> = {
  code: number
  message: string
  data: T
}
