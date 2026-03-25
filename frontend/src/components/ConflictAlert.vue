<template>
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
    </template>
  </el-alert>
</template>

<script setup lang="ts">
import type { BookingConflictDTO } from '@/types/booking'
import { formatRange } from '@/utils/datetime'

defineProps<{ conflicts: BookingConflictDTO[] }>()
</script>

<style scoped>
.alert {
  margin-top: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.item {
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
}

.subject {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.time {
  font-variant-numeric: tabular-nums;
}
</style>

