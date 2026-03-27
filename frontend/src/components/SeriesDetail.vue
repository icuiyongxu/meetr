<template>
  <el-dialog
    v-model="visible"
    :title="seriesTitle"
    width="700px"
    :close-on-click-modal="false"
  >
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="series">
      <!-- 主预约信息 -->
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="会议室">{{ series.master.roomName }}</el-descriptions-item>
        <el-descriptions-item label="主题">{{ series.master.subject }}</el-descriptions-item>
        <el-descriptions-item label="重复类型">{{ recurrenceLabel(series.master.recurrenceType) }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ series.master.recurrenceEndDate || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 系列操作：修改后续时间 -->
      <div class="update-future-bar">
        <span class="bar-label">从第</span>
        <el-input-number v-model="fromIndex" :min="2" :max="series.totalCount" size="small" />
        <span class="bar-label">场开始，将时间调整为</span>
        <el-time-select
          v-model="newStartTime"
          placeholder="开始时间"
          size="small"
          start="00:00"
          step="00:15"
          end="23:45"
          style="width: 100px"
        />
        <span class="bar-label">-</span>
        <el-time-select
          v-model="newEndTime"
          placeholder="结束时间"
          size="small"
          start="00:00"
          step="00:15"
          end="23:45"
          style="width: 100px"
        />
        <el-button type="primary" size="small" :loading="updatingFuture" @click="onUpdateFuture">
          批量修改
        </el-button>
      </div>

      <!-- 系列实例列表 -->
      <div class="series-list-title">
        系列预约（共 {{ series.totalCount }} 场）
      </div>

      <el-table :data="allSeriesBookings" stripe size="small">
        <el-table-column label="#" width="50" prop="seriesIndex" />
        <el-table-column label="日期" prop="startTime" :formatter="(r: any) => formatDate(r.startTime)" />
        <el-table-column label="时间" :formatter="(r: any) => formatTimeRange(r.startTime, r.endTime)" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'CANCELED'"
              link
              type="danger"
              size="small"
              :loading="skippingId === row.id"
              @click="onSkipOne(row)"
            >
              跳过本次
            </el-button>
            <span v-else class="skipped-tag">已跳过</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        type="danger"
        plain
        :loading="cancelingSeries"
        @click="onCancelSeries"
      >
        取消整个系列
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Booking } from '@/types/booking'
import { getSeriesBookings, updateFutureSeries, cancelBooking } from '@/api/booking'
import type { SeriesBookingResponse } from '@/api/booking'
import { formatBusinessDateTime } from '@/utils/datetime'

const props = defineProps<{
  modelValue: boolean
  bookingId: number | undefined
  bookerId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  'series-updated': []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const series = ref<SeriesBookingResponse | null>(null)
const loading = ref(false)
const updatingFuture = ref(false)
const skippingId = ref<number | null>(null)
const cancelingSeries = ref(false)

// 修改后续时间的控件
const fromIndex = ref(2)
const newStartTime = ref('')
const newEndTime = ref('')

watch(visible, async (v) => {
  if (v && props.bookingId) {
    await loadSeries()
    // 默认从第2场开始（即第一个子预约）
    fromIndex.value = 2
    newStartTime.value = ''
    newEndTime.value = ''
  }
})

const allSeriesBookings = computed<Booking[]>(() => {
  if (!series.value) return []
  return [series.value.master, ...series.value.instances]
})

const seriesTitle = computed(() => {
  if (!series.value) return '系列预约详情'
  const rec = recurrenceLabel(series.value.master.recurrenceType)
  return `系列详情：${series.value.master.subject}（${rec}，共${series.value.totalCount}场）`
})

async function loadSeries() {
  if (!props.bookingId) return
  loading.value = true
  try {
    series.value = await getSeriesBookings(props.bookingId, props.bookerId)
  } catch {
    ElMessage.error('加载系列预约失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

function formatDate(ms: number) {
  return formatBusinessDateTime(ms, 'YYYY-MM-DD')
}

function formatTimeRange(startMs: number, endMs: number) {
  const s = formatBusinessDateTime(startMs, 'HH:mm')
  const e = formatBusinessDateTime(endMs, 'HH:mm')
  return `${s} - ${e}`
}

function recurrenceLabel(type?: string) {
  const map: Record<string, string> = {
    DAILY: '每天', WEEKLY: '每周', WORKDAY: '工作日', MONTHLY: '每月',
  }
  return type ? (map[type] ?? type) : '-'
}

function statusType(status?: string) {
  return status === 'CANCELED' ? 'info' : status === 'PENDING' ? 'warning' : 'success'
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    BOOKED: '已预约', CANCELED: '已取消', FINISHED: '已完成', PENDING: '待审批',
  }
  return status ? (map[status] ?? status) : '-'
}

// 跳过本次（取消单个子预约）
async function onSkipOne(booking: Booking) {
  try {
    await ElMessageBox.confirm(
      `确定跳过 ${formatDate(booking.startTime)} 的这场预约吗？其他场次不受影响。`,
      '跳过本次',
      { type: 'warning', confirmButtonText: '跳过', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  skippingId.value = booking.id
  try {
    await cancelBooking(booking.id, props.bookerId, false)
    ElMessage.success('已跳过')
    await loadSeries()
    emit('series-updated')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    skippingId.value = null
  }
}

// 批量修改后续时间
async function onUpdateFuture() {
  if (!newStartTime.value || !newEndTime.value) {
    ElMessage.warning('请填写新的开始和结束时间')
    return
  }
  if (!series.value) return
  const master = series.value.master

  // 用 master 的日期 + 新的时间拼出新的 epoch ms
  const baseDate = formatBusinessDateTime(master.startTime, 'YYYY-MM-DD')
  const newStartMs = new Date(`${baseDate}T${newStartTime.value}:00+08:00`).getTime()
  const newEndMs = new Date(`${baseDate}T${newEndTime.value}:00+08:00`).getTime()

  if (newEndMs <= newStartMs) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定将第 ${fromIndex.value} 场及后续所有场次的时间统一调整为 ${newStartTime.value} - ${newEndTime.value} 吗？`,
      '批量修改时间',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  updatingFuture.value = true
  try {
    await updateFutureSeries(master.id, {
      operatorId: props.bookerId,
      newStartTimeMs: newStartMs,
      newEndTimeMs: newEndMs,
      fromSeriesIndex: fromIndex.value,
    })
    ElMessage.success('已批量修改')
    await loadSeries()
    emit('series-updated')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    updatingFuture.value = false
  }
}

// 取消整个系列
async function onCancelSeries() {
  if (!series.value) return
  try {
    await ElMessageBox.confirm(
      `确定取消整个系列预约（${series.value.totalCount}场）吗？此操作不可恢复。`,
      '取消整个系列',
      { type: 'error', confirmButtonText: '确定取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  cancelingSeries.value = true
  try {
    await cancelBooking(series.value.master.id, props.bookerId, true)
    ElMessage.success('已取消整个系列')
    visible.value = false
    emit('series-updated')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    cancelingSeries.value = false
  }
}
</script>

<style scoped>
.update-future-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding: 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  flex-wrap: wrap;
}

.bar-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.series-list-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.skipped-tag {
  font-size: 12px;
  color: var(--el-text-color-disabled);
}
</style>
