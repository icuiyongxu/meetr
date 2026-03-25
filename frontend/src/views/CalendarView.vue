<template>
  <div class="calendar-view">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          size="default"
          @change="onDateChange"
        />
        <el-button @click="goToday">今天</el-button>
        <el-button @click="prevDay">&lt;</el-button>
        <el-button @click="nextDay">&gt;</el-button>
        <span class="date-label">{{ formattedDate }}</span>
      </div>
      <div class="toolbar-right">
        <el-select v-model="selectedBuildingId" filterable clearable placeholder="全部楼栋" size="default" @change="loadData">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="showBookingDialog(null)">+ 新建预约</el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-loading="loading" class="calendar-wrap">
      <!-- 无会议室提示 -->
      <el-empty v-if="!loading && !rooms.length" description="该日期/楼栋下无会议室" />

      <!-- 日历网格 -->
      <div v-else class="grid" :style="gridStyle">
        <!-- 左上角空白 -->
        <div class="corner"></div>

        <!-- 横向会议室头 -->
        <div
          v-for="room in rooms"
          :key="room.id"
          class="room-header"
        >
          <div class="room-name">{{ room.name }}</div>
          <div class="room-meta">{{ room.floor || '' }} {{ room.capacity ? '· ' + room.capacity + '人' : '' }}</div>
        </div>

        <!-- 时间行 -->
        <template v-for="slot in timeSlots" :key="slot.time">
          <!-- 时间标签 -->
          <div class="time-label">{{ slot.label }}</div>

          <!-- 每个房间的格子 -->
          <div
            v-for="room in rooms"
            :key="room.id + '-' + slot.time"
            class="cell"
            :class="getCellClass(room.id, slot.time)"
            @click="onCellClick(room, slot)"
          >
            <template v-if="getBookingAt(room.id, slot.time) as booking">
              <div
                class="booking-block"
                :class="getBookingBlockClass(booking)"
                :style="getBookingBlockStyle(booking, room.id, slot.time)"
                @click.stop="onBookingClick(booking)"
              >
                <div class="booking-subject">{{ booking.subject }}</div>
                <div class="booking-time">{{ formatTime(booking.startTime) }}-{{ formatTime(booking.endTime) }}</div>
                <div class="booking-owner">{{ booking.bookerName || booking.bookerId }}</div>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>

    <!-- 预约对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建预约' : '预约详情'"
      width="520px"
      destroy-on-close
    >
      <!-- 详情模式 -->
      <div v-if="dialogMode === 'view' && selectedBooking" class="booking-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会议室">{{ selectedBooking.roomName }}</el-descriptions-item>
          <el-descriptions-item label="主题">{{ selectedBooking.subject }}</el-descriptions-item>
          <el-descriptions-item label="预约人">{{ selectedBooking.bookerName || selectedBooking.bookerId }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatRange(selectedBooking.startTime, selectedBooking.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="参会人数">{{ selectedBooking.attendeeCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(selectedBooking.status)">{{ selectedBooking.status }}</el-tag>
            <el-tag v-if="selectedBooking.approvalStatus !== 'NONE'" :type="approvalTagType(selectedBooking.approvalStatus)">
              {{ selectedBooking.approvalStatus }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedBooking.remark" label="备注">{{ selectedBooking.remark }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-actions">
          <el-button
            v-if="canCancel(selectedBooking)"
            type="danger"
            plain
            @click="onCancelBooking(selectedBooking)"
          >
            取消预约
          </el-button>
          <el-button @click="dialogMode = 'create'; dialogVisible = false">新建其他</el-button>
        </div>
      </div>

      <!-- 创建/编辑模式 -->
      <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="会议室">
          <el-select v-model="form.roomId" placeholder="选择会议室" style="width: 100%">
            <el-option
              v-for="r in rooms"
              :key="r.id"
              :label="`${r.name} (${r.floor || ''} · ${r.capacity}人)`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="会议主题" prop="subject">
          <el-input v-model="form.subject" placeholder="必填" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="参会人数">
          <el-input-number v-model="form.attendeeCount" :min="1" :max="999" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>

        <!-- 冲突提示 -->
        <ConflictAlert v-if="conflicts.length" :conflicts="conflicts" />
      </el-form>

      <template #footer v-if="dialogMode === 'create'">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
dayjs.extend(utc)
dayjs.extend(timezone)

import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import { getBookingsByRoomAndDate } from '@/api/booking'
import { createBooking, cancelBooking, checkConflict } from '@/api/booking'
import { useBookingStore } from '@/stores/booking'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'
import type { Booking, BookingConflictDTO } from '@/types/booking'
import ConflictAlert from '@/components/ConflictAlert.vue'

// ── 日期状态 ────────────────────────────────────────────
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedBuildingId = ref<number | undefined>(undefined)
const loading = ref(false)

// ── 数据 ────────────────────────────────────────────────
const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const allDayBookings = ref<Booking[]>([]) // 当日全量预约

// ── 时间格（按 resolution=30min 生成） ───────────────────
const SLOT_MINUTES = 30 // TODO: 后续按会议室 config 动态
const OPEN_HOUR = 0   // 24小时制，从 00:00 开始
const CLOSE_HOUR = 24 // 到 24:00 结束（全天）

const timeSlots = computed(() => {
  const slots = []
  for (let h = OPEN_HOUR; h < CLOSE_HOUR; h++) {
    for (let m = 0; m < 60; m += SLOT_MINUTES) {
      const totalMin = h * 60 + m
      const hh = String(Math.floor(totalMin / 60)).padStart(2, '0')
      const mm = String(totalMin % 60).padStart(2, '0')
      slots.push({
        time: `${selectedDate.value} ${hh}:${mm}:00`,
        label: `${hh}:${mm}`,
        totalMinutes: totalMin,
      })
    }
  }
  return slots
})

// grid 列宽：第一个时间标签列 + N 个房间列
const gridStyle = computed(() => ({
  gridTemplateColumns: `80px repeat(${rooms.value.length}, 1fr)`,
}))

// ── 工具 ───────────────────────────────────────────────
function formatTime(dt: string) {
  return dayjs.tz(dt, 'Asia/Shanghai').format('HH:mm')
}

function formatRange(start: string, end: string) {
  return `${dayjs.tz(start, 'Asia/Shanghai').format('YYYY-MM-DD HH:mm')} - ${dayjs.tz(end, 'Asia/Shanghai').format('HH:mm')}`
}

function statusTagType(s: Booking['status']) {
  return { BOOKED: 'primary', CANCELED: 'info', FINISHED: 'success' }[s] || 'info'
}

function approvalTagType(s: Booking['approvalStatus']) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', NONE: 'info' }[s] || 'info'
}

function canCancel(booking: Booking) {
  if (booking.status !== 'BOOKED') return false
  return dayjs.tz(booking.startTime, 'Asia/Shanghai').isAfter(dayjs())
}

// ── 预约格子逻辑 ────────────────────────────────────────
function getBookingAt(roomId: number, slotTime: string): Booking | null {
  const slotMin = dayjs.tz(slotTime, 'Asia/Shanghai').valueOf()
  return (
    allDayBookings.value.find((b) => {
      if (b.roomId !== roomId) return false
      if (b.status === 'CANCELED') return false
      const startMin = dayjs.tz(b.startTime, 'Asia/Shanghai').valueOf()
      const endMin = dayjs.tz(b.endTime, 'Asia/Shanghai').valueOf()
      return slotMin >= startMin && slotMin < endMin
    }) || null
  )
}

function getCellClass(roomId: number, slotTime: string) {
  const b = getBookingAt(roomId, slotTime)
  const now = dayjs()
  const slotDt = dayjs.tz(slotTime, 'Asia/Shanghai')
  if (b) return { booked: true, 'booking-start': dayjs.tz(b.startTime, 'Asia/Shanghai').valueOf() === slotDt.valueOf() }
  if (slotDt.isBefore(now)) return { past: true }
  return { free: true }
}

function getBookingBlockClass(booking: Booking) {
  return {
    'is-own': booking.bookerId === store.userId,
    'is-others': booking.bookerId !== store.userId,
    'is-canceled': booking.status === 'CANCELED',
  }
}

function getBookingBlockStyle(booking: Booking, _roomId: number, slotTime: string): Record<string, string> {
  // 纵向位置由 slot 在 timeSlots 中的索引决定
  const slotIdx = timeSlots.value.findIndex((s) => s.time === slotTime)
  if (slotIdx < 0 || dayjs.tz(booking.startTime, 'Asia/Shanghai').valueOf() !== dayjs.tz(slotTime, 'Asia/Shanghai').valueOf()) {
    return { display: 'none' }
  }

  // 计算跨越的 slot 数
  const startMin = dayjs.tz(booking.startTime, 'Asia/Shanghai').valueOf()
  const endMin = dayjs.tz(booking.endTime, 'Asia/Shanghai').valueOf()
  const slotMs = SLOT_MINUTES * 60 * 1000
  const slotsCount = Math.round((endMin - startMin) / slotMs)

  // 每个 slot 高度 36px
  const CELL_H = 36
  return {
    top: '2px',
    height: `${slotsCount * CELL_H - 4}px`,
  }
}

function onCellClick(room: Room, slot: { time: string; label: string }) {
  // 点在已有预约格子上 — 在 onBookingClick 处理
  const existing = getBookingAt(room.id, slot.time)
  if (existing) {
    onBookingClick(existing)
    return
  }

  // 免费的格子 — 弹出创建对话框，预填时间和房间
  const now = dayjs()
  const slotDt = dayjs(slot.time)
  if (slotDt.isBefore(now)) {
    ElMessage.warning('不能预约过去的时间段')
    return
  }
  showBookingDialog(null, room.id, slot.time)
}

function onBookingClick(booking: Booking) {
  selectedBooking.value = booking
  dialogMode.value = 'view'
  dialogVisible.value = true
}

// ── 对话框 ──────────────────────────────────────────────
const dialogMode = ref<'create' | 'view'>('create')
const dialogVisible = ref(false)
const selectedBooking = ref<Booking | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const conflicts = ref<BookingConflictDTO[]>([])

const store = useBookingStore()

const form = reactive({
  roomId: null as number | null,
  subject: '',
  startTime: '',
  endTime: '',
  attendeeCount: 1,
  remark: '',
})

const rules: FormRules = {
  roomId: [{ required: true, message: '请选择会议室', trigger: 'change' }],
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

function disablePastDate(date: Date) {
  return dayjs(date).isBefore(dayjs(), 'day')
}

function showBookingDialog(_event?: MouseEvent, prefilledRoomId?: number, prefilledTime?: string) {
  dialogMode.value = 'create'
  conflicts.value = []
  Object.assign(form, {
    roomId: prefilledRoomId ?? (rooms.value[0]?.id ?? null),
    subject: '',
    startTime: prefilledTime ? dayjs(prefilledTime).format('YYYY-MM-DDTHH:mm:ss') : '',
    endTime: prefilledTime
      ? dayjs(prefilledTime).add(1, 'hour').format('YYYY-MM-DDTHH:mm:ss')
      : '',
    attendeeCount: 1,
    remark: '',
  })
  dialogVisible.value = true
}

async function onSubmit() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  const start = dayjs(form.startTime)
  const end = dayjs(form.endTime)
  if (!start.isBefore(end)) {
    ElMessage.error('开始时间必须早于结束时间')
    return
  }
  if (start.isBefore(dayjs())) {
    ElMessage.error('开始时间不能在过去')
    return
  }

  submitting.value = true
  try {
    // 先做冲突预检
    const cr = await checkConflict({
      roomId: form.roomId!,
      startTime: start.format('YYYY-MM-DDTHH:mm:ss'),
      endTime: end.format('YYYY-MM-DDTHH:mm:ss'),
    })
    if (cr.conflict) {
      conflicts.value = cr.conflictingBookings
      ElMessage.error('该时间段存在冲突')
      return
    }
    conflicts.value = []

    const result = await createBooking({
      roomId: form.roomId!,
      subject: form.subject.trim(),
      bookerId: store.userId,
      bookerName: store.userName || undefined,
      startTime: start.format('YYYY-MM-DDTHH:mm:ss'),
      endTime: end.format('YYYY-MM-DDTHH:mm:ss'),
      attendeeCount: form.attendeeCount,
      remark: form.remark?.trim() || undefined,
    })

    if (!result.success) {
      if (result.conflicts?.length) conflicts.value = result.conflicts
      if (result.violations?.length) ElMessage.error(result.violations.map((v) => v.message).join('；'))
      return
    }

    ElMessage.success('预约成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function onCancelBooking(booking: Booking) {
  await ElMessageBox.confirm('确定要取消这个预约吗？', '取消确认', { type: 'warning' })
  await cancelBooking(booking.id, store.userId)
  ElMessage.success('已取消')
  dialogVisible.value = false
  await loadData()
}

// ── 日期导航 ────────────────────────────────────────────
function onDateChange() {
  loadData()
}

function goToday() {
  selectedDate.value = dayjs().format('YYYY-MM-DD')
  loadData()
}

function prevDay() {
  selectedDate.value = dayjs(selectedDate.value).subtract(1, 'day').format('YYYY-MM-DD')
  loadData()
}

function nextDay() {
  selectedDate.value = dayjs(selectedDate.value).add(1, 'day').format('YYYY-MM-DD')
  loadData()
}

const formattedDate = computed(() => dayjs(selectedDate.value).format('YYYY年MM月DD日 dddd'))

// ── 数据加载 ────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const [bData, rData] = await Promise.all([
      getBuildings(),
      getRooms({
        buildingId: selectedBuildingId.value,
        status: 'ENABLED',
      }),
    ])
    buildings.value = bData
    rooms.value = rData

    // 拉每个房间当天的预约
    const dateStr = selectedDate.value
    const roomBookings = await Promise.all(
      rData.map((r) => getBookingsByRoomAndDate(r.id, dateStr).catch(() => []))
    )
    allDayBookings.value = roomBookings.flat()
  } finally {
    loading.value = false
  }
}

// ── 初始化 ─────────────────────────────────────────────
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.calendar-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-label {
  font-weight: 600;
  color: var(--el-text-color-primary);
  min-width: 140px;
}

.calendar-wrap {
  flex: 1;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

/* ── 日历网格 ─────────────────────────────────────────── */
.grid {
  display: grid;
  /* gridTemplateColumns 由 :style 动态设置: 80px + repeat(N, 1fr) */
  min-width: 600px;
}

.corner {
  position: sticky;
  top: 0;
  left: 0;
  z-index: 3;
  background: var(--el-bg-color-page);
  border-right: 1px solid var(--el-border-color-light);
  border-bottom: 1px solid var(--el-border-color-light);
}

.room-header {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--el-bg-color-page);
  border-right: 1px solid var(--el-border-color-light);
  border-bottom: 2px solid var(--el-border-color-light);
  padding: 8px 6px;
  text-align: center;
  overflow: hidden;
}

.room-header:last-child {
  border-right: none;
}

.room-header .room-name {
  font-weight: 700;
  font-size: 13px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-header .room-meta {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.time-label {
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--el-bg-color-page);
  border-right: 1px solid var(--el-border-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 6px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  height: 36px;
  display: flex;
  align-items: center;
  text-align: right;
  justify-content: flex-end;
}

.cell {
  height: 36px;
  border-right: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  position: relative;
  cursor: pointer;
  transition: background 0.1s;
}

.cell:last-child {
  border-right: none;
}

.cell:hover:not(.booked) {
  background: var(--el-color-primary-light-9);
}

.cell.past {
  background: var(--el-fill-color-light);
  cursor: not-allowed;
}

.cell.free:hover::after {
  content: '+';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--el-color-primary);
  font-weight: 700;
  pointer-events: none;
}

/* ── 预约色块 ─────────────────────────────────────────── */
.booking-block {
  position: absolute;
  left: 2px;
  right: 2px;
  z-index: 1;
  border-radius: 4px;
  padding: 2px 5px;
  overflow: hidden;
  cursor: pointer;
  font-size: 11px;
  transition: opacity 0.15s;
}

.booking-block:hover {
  opacity: 0.85;
}

.booking-block.is-own {
  background: var(--el-color-primary-light-8);
  border-left: 3px solid var(--el-color-primary);
  color: var(--el-color-primary);
}

.booking-block.is-others {
  background: var(--el-color-success-light-8);
  border-left: 3px solid var(--el-color-success);
  color: var(--el-color-success);
}

.booking-block.is-canceled {
  background: var(--el-fill-color-dark);
  border-left: 3px solid var(--el-border-color);
  color: var(--el-text-color-secondary);
  text-decoration: line-through;
}

.booking-subject {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.booking-time {
  opacity: 0.8;
  white-space: nowrap;
}

.booking-owner {
  opacity: 0.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 详情 ─────────────────────────────────────────────── */
.booking-detail {
  padding: 4px 0;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
