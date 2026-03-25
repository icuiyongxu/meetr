export type BuildingStatus = 'ACTIVE' | 'INACTIVE'

export interface Building {
  id: number
  name: string
  campus?: string
  address?: string
  sortNo: number
  status: BuildingStatus
}

