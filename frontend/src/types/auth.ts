export type PermissionCode =
  | 'building:view'
  | 'building:manage'
  | 'room:view'
  | 'room:manage'
  | 'booking:view'
  | 'booking:manage'
  | 'booking:approve'
  | 'config:view'
  | 'config:manage'
  | 'user:view'
  | 'user:manage'
  | 'role:manage'
  | 'notification:view'
  | 'notification:manage'

export interface AuthUserProfile {
  id: number
  userId: string
  name: string
  status: string
  roles: string[]
  permissions?: PermissionCode[]
  email?: string
  emailEnabled?: boolean
}
