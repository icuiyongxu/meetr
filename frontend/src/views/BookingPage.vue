<template>
  <div class="page">
    <div class="meetr-page-title">预约会议室</div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="9" :md="8" :lg="7">
        <el-card shadow="never" v-loading="roomLoading">
          <template #header>
            <div class="panel-title">会议室信息</div>
          </template>

          <template v-if="room">
            <div class="room-name">{{ room.name }}</div>
            <div class="room-meta">
              <div class="line">
                <span class="k">楼栋</span>
                <span class="v">{{ room.buildingName || room.buildingId }}</span>
              </div>
              <div class="line" v-if="room.floor">
                <span class="k">楼层</span>
                <span class="v">{{ room.floor }}</span>
              </div>
              <div class="line">
                <span class="k">容量</span>
                <span class="v">{{ room.capacity }}</span>
              </div>
              <div class="line">
                <span class="k">状态</span>
                <el-tag :type="room.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
                  {{ room.status }}
                </el-tag>
              </div>
            </div>

            <div class="equip">
              <div class="k">设备</div>
              <div class="v">
                <el-tag v-for="k in room.equipment" :key="k" size="small" effect="plain" class="equip-tag">
                  {{ equipmentLabel(k) }}
                </el-tag>
                <el-text v-if="!room.equipment.length" type="info">无</el-text>
              </div>
            </div>

            <div class="cfg">
              <el-divider content-position="left">规则</el-divider>
              <div class="line">
                <span class="k">开放时间</span>
                <span class="v">{{ config.morningStarts }} - {{ config.eveningEnds }}</span>
              </div>
              <div class="line">
                <span class="k">粒度</span>
                <span class="v">{{ Math.round(config.resolution / 60) }} 分钟</span>
              </div>
              <div class="line">
                <span class="k">默认时长</span>
                <span class="v">{{ config.defaultDuration }} 分钟</span>
              </div>
              <div class="line">
                <span class="k">需要审批</span>
                <el-tag :type="config.approvalRequired ? 'warning' : 'success'" effect="plain">
                  {{ config.approvalRequired ? '是' : '否' }}
                </el-tag>
              </div>
            </div>
          </template>
          <el-empty v-else description="会议室不存在" />
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="15" :md="16" :lg="17">
        <el-card shadow="never">
          <template #header>
            <div class="right-header">
              <div class="panel-title">选择时间段</div>
              <div class="actions">
                <el-date-picker
                  v-model="selectedDate"
                  type="date"
                  format="YYYY-MM-DD"
                  placeholder="选择日期"
                  :disabled-date="disablePast"
                  @change="onDateChange"
                />
                <el-button :loading="slotsLoading" @click="loadDayBookings">刷新</el-button>
                <el-button :disabled="!hasSelection" @click="clearSelection">清除选择</el-button>
              </div>
            </div>
          </template>

          <div class="legend">
            <span class="dot available"></span><span class="txt">可用</span>
            <span class="dot occupied"></span><span class="txt">已占用</span>
            <span class="dot selected"></span><span class="txt">已选中</span>
          </div>

          <el-skeleton v-if="slotsLoading" :rows="8" animated />
          <div v-else class="slots">
            <div class="slot-grid">
              <button
                v-for="s in slots"
                :key="s.key"
                class="slot"
                :class="{
                  occupied: s.occupied,
                  selected: s.selected,
                }"
                :disabled="s.occupied"
                @click="pickSlot(s.index)"
              >
                <span class="t">{{ s.label }}</span>
              </button>
            </div>
            <div class="hint">
              <div v-if="hasSelection" class="sel">
                已选：{{ selectionText }}
              </div>
              <div v-else class="sel muted">点击一个时间格，默认选择 {{ config.defaultDuration }} 分钟，可再点击调整结束时间</div>
              <div class="muted">已占用基于当日开放时间段内的冲突检测结果</div>
            </div>
          </div>
        </el-card>

        <div style="height: 12px"></div>

        <BookingForm
          v-if="room"
          :room-id="room.id"
          :initial-slot="initialSlot"
          @conflict="(c) => (submitConflicts = c)"
          @success="onSuccess"
        />
        <ConflictAlert :conflicts="submitConflicts" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import type { Room, RoomConfig } from '@/types/room'
import type { BookingConflictDTO } from '@/types/booking'
import { getRoom } from '@/api/room'
import { getBookingRules } from '@/api/config'
import { checkConflict } from '@/api/booking'
import { equipmentLabel } from '@/utils/equipment'
import { formatRange } from '@/utils/datetime'
import BookingForm from '@/components/BookingForm.vue'
import ConflictAlert from '@/components/ConflictAlert.vue'

const route = useRoute()
const router = useRouter()

const roomId = computed(() => Number(route.params.roomId))

const room = ref<Room | null>(null)
const roomLoading = ref(false)

const config = reactive<RoomConfig>({
  id: 0,
  roomId: null,
  resolution: 1800,
  defaultDuration: 60,
  morningStarts: '08:00',
  eveningEnds: '22:00',
  minBookAheadMinutes: 0,
  maxBookAheadDays: 30,
  minDurationMinutes: 15,
  maxDurationMinutes: 480,
  maxPerDay: 3,
  maxPerWeek: 10,
  approvalRequired: false,
})

const selectedDate = ref<Date>(dayjs().startOf('day').toDate())
const slotsLoading = ref(false)
const occupiedBookings = ref<BookingConflictDTO[]>([])

const startIndex = ref<number | null>(null)
const endIndex = ref<number | null>(null) // exclusive

const submitConflicts = ref<BookingConflictDTO[]>([])

function disablePast(date: Date) {
  return dayjs(date).endOf('day').isBefore(dayjs())
}

function clearSelection() {
  startIndex.value = null
  endIndex.value = null
  submitConflicts.value = []
}

const hasSelection = computed(() => startIndex.value !== null && endIndex.value !== null)

function parseHHmm(s: string) {
  const [h, m] = s.split(':').map((x) => Number(x))
  return (h || 0) * 60 + (m || 0)
}

function slotRangeForDay() {
  const base = dayjs(selectedDate.value).startOf('day')
  const startM = parseHHmm(config.morningStarts)
  const endM = parseHHmm(config.eveningEnds)
  const start = base.add(startM, 'minute')
  const end = base.add(endM, 'minute')
  return { start, end }
}

type SlotVM = {
  key: string
  index: number
  start: dayjs.Dayjs
  end: dayjs.Dayjs
  label: string
  occupied: boolean
  selected: boolean
}

const slots = computed<SlotVM[]>(() => {
  const { start, end } = slotRangeForDay()
  const stepSec = Math.max(60, config.resolution || 1800)
  const stepMin = Math.max(1, Math.round(stepSec / 60))

  const list: SlotVM[] = []
  let cur = start
  let idx = 0
  while (cur.isBefore(end)) {
    const nxt = cur.add(stepMin, 'minute')
    const occupied = occupiedBookings.value.some((b) => {
      // b.startTime/endTime 是 UTC 毫秒，转为北京时间后比较
      const bs = dayjs.tz(b.startTime, 'Asia/Shanghai')
      const be = dayjs.tz(b.endTime, 'Asia/Shanghai')
      return bs.isBefore(nxt) && be.isAfter(cur)
    })
    const selected =
      startIndex.value !== null &&
      endIndex.value !== null &&
      idx >= startIndex.value &&
      idx < endIndex.value
    list.push({
      key: `${cur.format('HH:mm')}-${idx}`,
      index: idx,
      start: cur,
      end: nxt,
      label: cur.format('HH:mm'),
      occupied,
      selected,
    })
    cur = nxt
    idx++
  }
  return list
})

const selectionText = computed(() => {
  if (!hasSelection.value) return ''
  const s = slots.value[startIndex.value!]
  const e = slots.value[endIndex.value! - 1]
  // start/end 是 dayjs(UTC ms)，转为北京时间后格式化
  return formatRange(
    dayjs.tz(s.start.valueOf(), 'Asia/Shanghai').format('YYYY-MM-DD HH:mm'),
    dayjs.tz(e.end.valueOf(), 'Asia/Shanghai').format('YYYY-MM-DD HH:mm'),
  )
})

const initialSlot = computed(() => {
  if (!hasSelection.value) return undefined
  const s = slots.value[startIndex.value!]
  const e = slots.value[endIndex.value! - 1]
  return { start: s.start.toDate(), end: e.end.toDate() }
})

function pickSlot(idx: number) {
  submitConflicts.value = []
  const s = slots.value[idx]
  if (!s || s.occupied) return

  const curStart = startIndex.value
  const curEnd = endIndex.value
  if (curStart === null || curEnd === null) {
    // default range: start + defaultDuration
    const stepMin = Math.max(1, Math.round(Math.max(60, config.resolution) / 60))
    const wantSlots = Math.max(1, Math.round((config.defaultDuration || 60) / stepMin))
    const proposedEnd = Math.min(slots.value.length, idx + wantSlots)
    const hasOccupied = slots.value.slice(idx, proposedEnd).some((x) => x.occupied)
    startIndex.value = idx
    endIndex.value = hasOccupied ? idx + 1 : proposedEnd
    if (hasOccupied) ElMessage.warning('默认时长包含已占用时间段，已改为选择 1 个时间格')
    return
  }

  if (idx < curStart) {
    startIndex.value = idx
    endIndex.value = curStart + 1
    return
  }

  if (idx === curStart) {
    startIndex.value = idx
    endIndex.value = idx + 1
    return
  }

  // set end to idx+1, ensure no occupied inside
  const nextEnd = Math.min(slots.value.length, idx + 1)
  const seg = slots.value.slice(curStart, nextEnd)
  if (seg.some((x) => x.occupied)) {
    ElMessage.error('选择范围包含已占用时间段，请重新选择')
    return
  }
  endIndex.value = nextEnd
}

async function loadRoomAndConfig() {
  roomLoading.value = true
  try {
    room.value = await getRoom(roomId.value)
  } finally {
    roomLoading.value = false
  }

  try {
    const cfg = await getBookingRules(roomId.value)
    Object.assign(config, cfg)
  } catch {
    // if config endpoint is unavailable/forbidden, keep defaults
  }
}

async function loadDayBookings() {
  if (!room.value) return
  slotsLoading.value = true
  try {
    const { start, end } = slotRangeForDay()
    const res = await checkConflict({
      roomId: room.value.id,
      startTime: start.valueOf(),
      endTime: end.valueOf(),
    })
    occupiedBookings.value = res.conflictingBookings || []
    // if current selection becomes invalid, clear it
    if (hasSelection.value) {
      const seg = slots.value.slice(startIndex.value!, endIndex.value!)
      if (seg.some((x) => x.occupied)) clearSelection()
    }
  } finally {
    slotsLoading.value = false
  }
}

function onDateChange() {
  clearSelection()
  loadDayBookings()
}

function onSuccess() {
  router.push('/dashboard')
}

onMounted(async () => {
  await loadRoomAndConfig()
  await loadDayBookings()
})
</script>

<style scoped>
.panel-title {
  font-weight: 600;
}

.room-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.room-meta {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.k {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.v {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.equip {
  margin-top: 12px;
}

.equip-tag {
  margin-right: 6px;
  margin-bottom: 6px;
}

.cfg {
  margin-top: 12px;
}

.right-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.legend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

.dot.available {
  background: #fff;
  border: 1px solid var(--el-border-color);
}

.dot.occupied {
  background: #dcdfe6;
}

.dot.selected {
  background: var(--el-color-primary);
}

.txt {
  margin-right: 8px;
}

.slots {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(84px, 1fr));
  gap: 8px;
  flex: 1;
}

.slot {
  appearance: none;
  border: 1px solid var(--el-border-color);
  background: #fff;
  border-radius: 8px;
  padding: 10px 8px;
  cursor: pointer;
  transition: transform 0.05s ease, border-color 0.15s ease, background 0.15s ease;
}

.slot:hover {
  border-color: var(--el-color-primary-light-5);
}

.slot:active {
  transform: translateY(1px);
}

.slot.occupied {
  background: #f2f3f5;
  color: var(--el-text-color-secondary);
  cursor: not-allowed;
}

.slot.selected {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}

.slot .t {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.hint {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: #fafcff;
}

.sel {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 1200px) {
  .slot-grid {
    grid-template-columns: repeat(4, minmax(84px, 1fr));
  }
  .hint {
    display: none;
  }
}

@media (max-width: 768px) {
  .slot-grid {
    grid-template-columns: repeat(3, minmax(84px, 1fr));
  }
}
</style>

