<template>
  <el-table :data="bookings" v-loading="loading" style="width: 100%">
    <el-table-column label="会议室" min-width="160">
      <template #default="{ row }">
        <div class="room">
          <div class="name">{{ row.roomName || `room#${row.roomId}` }}</div>
          <div class="building" v-if="row.buildingName">{{ row.buildingName }}</div>
        </div>
      </template>
    </el-table-column>

    <el-table-column label="时间段" min-width="220">
      <template #default="{ row }">
        <span class="time">{{ formatRange(row.startTime, row.endTime) }}</span>
      </template>
    </el-table-column>

    <el-table-column label="主题" min-width="200">
      <template #default="{ row }">
        <div class="subject">{{ row.subject }}</div>
        <div class="remark" v-if="row.remark">{{ row.remark }}</div>
      </template>
    </el-table-column>

    <el-table-column label="状态" width="170">
      <template #default="{ row }">
        <div class="tags">
          <el-tag :type="bookingTagType(row.status)" effect="plain">{{ row.status }}</el-tag>
          <el-tag :type="approvalTagType(row.approvalStatus)" effect="plain">
            {{ row.approvalStatus }}
          </el-tag>
        </div>
      </template>
    </el-table-column>

    <el-table-column label="操作" width="180" fixed="right">
      <template #default="{ row }">
        <el-button size="small" @click="emit('view', row)">查看</el-button>
        <el-button
          size="small"
          type="danger"
          plain
          :disabled="!canCancel(row)"
          @click="emit('cancel', row)"
        >
          取消
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import type { Booking } from '@/types/booking'
import { formatRange } from '@/utils/datetime'

const props = defineProps<{
  bookings: Booking[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'view', booking: Booking): void
  (e: 'cancel', booking: Booking): void
}>()

function bookingTagType(status: Booking['status']) {
  switch (status) {
    case 'BOOKED':
      return 'primary'
    case 'CANCELED':
      return 'info'
    case 'FINISHED':
      return 'success'
    default:
      return 'info'
  }
}

function approvalTagType(status: Booking['approvalStatus']) {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'NONE':
      return 'info'
    default:
      return 'info'
  }
}

function canCancel(row: Booking) {
  if (row.status !== 'BOOKED') return false
  return dayjs(row.startTime).isAfter(dayjs())
}

void props
</script>

<style scoped>
.room .name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.room .building {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.time {
  font-variant-numeric: tabular-nums;
}

.subject {
  font-weight: 500;
}

.remark {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

