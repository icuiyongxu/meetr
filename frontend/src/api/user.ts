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
