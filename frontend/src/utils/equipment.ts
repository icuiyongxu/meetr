export type EquipmentKey = 'projector' | 'whiteboard' | 'video' | 'phone'

export const equipmentOptions: Array<{ label: string; value: EquipmentKey }> = [
  { label: '投影仪', value: 'projector' },
  { label: '白板', value: 'whiteboard' },
  { label: '视频会议', value: 'video' },
  { label: '电话', value: 'phone' },
]

export function equipmentLabel(key: string) {
  const found = equipmentOptions.find((o) => o.value === key)
  return found?.label || key
}

