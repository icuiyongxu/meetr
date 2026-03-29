<template>
  <div>
    <el-alert
      v-if="conflicts.length"
      type="error"
      :title="`检测到时间冲突（${conflicts.length}）`"
      show-icon
      class="alert"
    >
      <template #default>
        <div class="list">
          <div v-for="c in conflicts" :key="c.id" class="item">
            <div class="subject">{{ c.subject }}</div>
            <div class="meta">
              <span class="time">{{ formatRange(c.startTime, c.endTime) }}</span>
              <span v-if="c.bookerName" class="who">预约人：{{ c.bookerName }}</span>
            </div>
          </div>
        </div>

        <!-- 备选会议室推荐 -->
        <div v-if="alternatives.length > 0" class="alternatives">
          <div class="alt-title">以下会议室在同一时段可用：</div>
          <div class="alt-list">
            <div v-for="r in alternatives" :key="r.id" class="alt-item">
              <div class="alt-info">
                <span class="alt-name">{{ r.name }}</span>
                <span class="alt-meta">{{ r.buildingName || r.buildingId }}</span>
              </div>
              <el-button type="primary" size="small" @click="emit('select-room', r.id)">
                换到该会议室
              </el-button>
            </div>
          </div>
        </div>
        <div v-else-if="alternativesLoading" class="alt-loading">正在查询备选会议室...</div>
        <div v-else-if="!alternativesLoading && alternativesFetched && alternatives.length === 0" class="alt-empty">
          未找到同时段可用的其他会议室
        </div>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { BookingConflictDTO } from '@/types/booking'
import type { Room } from '@/types/room'
import { formatRange } from '@/utils/datetime'
import { getAvailableRooms } from '@/api/room'

const props = defineProps<{
  conflicts: BookingConflictDTO[]
  startTime?: number
  endTime?: number
}>()

const emit = defineEmits<{
  'select-room': [roomId: number]
}>()

const alternatives = ref<Room[]>([])
const alternativesLoading = ref(false)
const alternativesFetched = ref(false)

watch(
  () => [props.conflicts.length, props.startTime, props.endTime],
  async ([conflictCount, startMs, endMs]) => {
    if (conflictCount > 0 && startMs && endMs) {
      alternativesLoading.value = true
      alternativesFetched.value = false
      try {
        const rooms = await getAvailableRooms({
          startTime: new Date(startMs).toISOString(),
          endTime: new Date(endMs).toISOString(),
        })
        alternatives.value = rooms || []
      } catch {
        alternatives.value = []
      } finally {
        alternativesLoading.value = false
        alternativesFetched.value = true
      }
    } else {
      alternatives.value = []
      alternativesFetched.value = false
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.alert {
  margin-top: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.item {
  padding: 6px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
}

.subject {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.meta {
  margin-top: 2px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.time {
  font-variant-numeric: tabular-nums;
}

.alternatives {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.alt-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.alt-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alt-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
}

.alt-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.alt-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.alt-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.alt-loading,
.alt-empty {
  margin-top: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
