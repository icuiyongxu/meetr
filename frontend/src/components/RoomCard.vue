<template>
  <el-card class="card" shadow="hover" @click="emit('select', room)">
    <div class="top">
      <div class="name">{{ room.name }}</div>
      <el-tag :type="room.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
        {{ room.status === 'ENABLED' ? '可用' : '停用' }}
      </el-tag>
    </div>

    <div class="meta">
      <div class="line">
        <el-icon><OfficeBuilding /></el-icon>
        <span class="text">{{ room.buildingName || `楼栋#${room.buildingId}` }}</span>
        <span v-if="room.floor" class="sep">|</span>
        <span v-if="room.floor" class="text">楼层 {{ room.floor }}</span>
      </div>
      <div class="line">
        <el-icon><UserFilled /></el-icon>
        <span class="text">容量 {{ room.capacity }}</span>
      </div>
    </div>

    <div class="equip">
      <template v-if="room.equipment?.length">
        <el-tag v-for="k in room.equipment" :key="k" size="small" class="equip-tag" effect="plain">
          <span class="equip-inner">
            <el-icon class="equip-icon">
              <component :is="equipmentIcon(k)" />
            </el-icon>
            <span>{{ equipmentLabel(k) }}</span>
          </span>
        </el-tag>
      </template>
      <el-text v-else type="info">无设备标记</el-text>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { Room } from '@/types/room'
import { equipmentLabel } from '@/utils/equipment'
import { OfficeBuilding, UserFilled, VideoCamera, Phone, Monitor, EditPen } from '@element-plus/icons-vue'

const props = defineProps<{ room: Room }>()
const emit = defineEmits<{ (e: 'select', room: Room): void }>()

function equipmentIcon(k: string) {
  switch (k) {
    case 'projector':
      return Monitor
    case 'whiteboard':
      return EditPen
    case 'video':
      return VideoCamera
    case 'phone':
      return Phone
    default:
      return Monitor
  }
}

// avoid unused warning from defineProps
void props
</script>

<style scoped>
.card {
  cursor: pointer;
}

.top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.name {
  font-weight: 700;
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.meta {
  margin-top: 10px;
  color: var(--el-text-color-regular);
}

.line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

.text {
  font-size: 13px;
}

.sep {
  color: var(--el-text-color-secondary);
}

.equip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.equip-tag {
  border-style: dashed;
}

.equip-inner {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.equip-icon {
  font-size: 14px;
}
</style>

