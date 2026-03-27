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
          <el-option label="全部楼栋" :value="undefined" />
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-button type="primary" @click="showBookingDialog(undefined)">+ 新建预约</el-button>
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
            @mousedown.left.prevent="onCellMousedown(room, slot)"
            @mouseenter.left="onCellMouseenter(room, slot)"
          >
            <template v-if="getBookingAt(room.id, slot.time)">
              <div
                class="booking-block"
                :class="getBookingBlockClass(getBookingAt(room.id, slot.time))"
                :style="getBookingBlockStyle(getBookingAt(room.id, slot.time), room.id, slot.time)"
                @click.stop="onBookingClick(getBookingAt(room.id, slot.time)!)"
              >
                <div class="booking-subject">
                  <span v-if="getBookingAt(room.id, slot.time)!.recurrenceType && getBookingAt(room.id, slot.time)!.recurrenceType !== 'NONE'" class="recurring-badge">↻</span>
                  {{ getBookingAt(room.id, slot.time)!.subject }}
                </div>
                <div class="booking-time">{{ formatTime(getBookingAt(room.id, slot.time)!.startTime) }}-{{ formatTime(getBookingAt(room.id, slot.time)!.endTime) }}</div>
                <div class="booking-owner">{{ getBookingAt(room.id, slot.time)!.bookerName || getBookingAt(room.id, slot.time)!.bookerId }}</div>
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
          <el-descriptions-item v-if="selectedBooking.recurrenceType && selectedBooking.recurrenceType !== 'NONE'" label="重复">
            {{ recurrenceLabel(selectedBooking.recurrenceType) }}，至 {{ selectedBooking.recurrenceEndDate }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ selectedBooking.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 系列子预约列表 -->
        <div v-if="seriesBookings.length > 1" class="series-list">
          <div class="series-list-title">系列全部场次（共 {{ seriesBookings.length }} 场）</div>
          <el-table :data="seriesBookings" size="small" border>
            <el-table-column label="时间" prop="startTime" :formatter="(row: Booking) => formatRange(row.startTime, row.endTime)" />
            <el-table-column label="状态" prop="status" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button
                  v-if="canCancel(row)"
                  type="danger"
                  size="small"
                  link
                  @click="onCancelBooking(row)"
                >取消</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-actions">
          <el-button
            v-if="canCancel(selectedBooking)"
            type="danger"
            plain
            @click="onCancelBooking(selectedBooking)"
          >
            {{ selectedBooking.recurrenceType && selectedBooking.recurrenceType !== 'NONE' ? '取消本次' : '取消预约' }}
          </el-button>
          <el-button
            v-if="selectedBooking.recurrenceType && selectedBooking.recurrenceType !== 'NONE'"
            type="warning"
            plain
            @click="onCancelSeries(selectedBooking)"
          >
            取消全部系列
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

        <el-form-item label="重复">
          <el-select v-model="form.recurrenceType" style="width: 100%">
            <el-option label="不重复" value="NONE" />
            <el-option label="每天" value="DAILY" />
            <el-option label="每周" value="WEEKLY" />
            <el-option label="工作日（周一至周五）" value="WORKDAY" />
            <el-option label="每月" value="MONTHLY" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.recurrenceType !== 'NONE'" label="重复结束">
          <el-date-picker
            v-model="form.recurrenceEndDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="(d: Date) => d.getTime() < dayjs(form.startTime).startOf('day').valueOf()"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
dayjs.extend(utc)
dayjs.extend(timezone)

import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import { getBookingsByRoomAndDate, getMyBookings } from '@/api/booking'
import { createBooking, cancelBooking, checkConflict } from '@/api/booking'
import { getBookingRules } from '@/api/config'
import { useBookingStore } from '@/stores/booking'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'
import type { RoomConfig } from '@/types/room'
import type { Booking, BookingConflictDTO } from '@/types/booking'
import ConflictAlert from '@/components/ConflictAlert.vue'
import { formatBusinessTime, formatRange, msToBusinessValueFormat } from '@/utils/datetime'

// ── 日期状态 ────────────────────────────────────────────
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedBuildingId = ref<number | undefined>(undefined)
const loading = ref(false)

// ── 数据 ────────────────────────────────────────────────
const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const allDayBookings = ref<Booking[]>([]) // 当日全量预约
const roomConfig = ref<RoomConfig | null>(null)

// ── 工具 ───────────────────────────────────────────────
function parseHHmm(s: string) {
  const [h, m] = s.split(':').map((x) => Number(x))
  return (h || 0) * 60 + (m || 0)
}

// ── 时间格（按会议室 config 动态生成） ───────────────────
const OPEN_MINUTES = computed(() => parseHHmm(roomConfig.value?.morningStarts ?? '00:00'))
const CLOSE_MINUTES = computed(() => parseHHmm(roomConfig.value?.eveningEnds ?? '24:00'))
const SLOT_MINUTES = computed(() => Math.max(1, Math.round((roomConfig.value?.resolution ?? 1800) / 60)))

const timeSlots = computed(() => {
  const slots = []
  const startM = OPEN_MINUTES.value
  const endM = CLOSE_MINUTES.value
  for (let m = startM; m < endM; m += SLOT_MINUTES.value) {
    const hh = String(Math.floor(m / 60)).padStart(2, '0')
    const mm = String(m % 60).padStart(2, '0')
    slots.push({
      time: `${selectedDate.value} ${hh}:${mm}:00`,
      label: `${hh}:${mm}`,
      totalMinutes: m,
    })
  }
  return slots
})

// grid 列宽：第一个时间标签列 + N 个房间列
const gridStyle = computed(() => ({
  gridTemplateColumns: `80px repeat(${rooms.value.length}, 1fr)`,
}))

// ── 工具 ───────────────────────────────────────────────
function formatTime(dt: string | number) {
  return formatBusinessTime(dt)
}

function statusTagType(s: Booking['status']) {
  return { BOOKED: 'primary', CANCELED: 'info', FINISHED: 'success' }[s] || 'info'
}

function approvalTagType(s: Booking['approvalStatus']) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', NONE: 'info' }[s] || 'info'
}

function canCancel(booking: Booking) {
  if (booking.status !== 'BOOKED') return false
  return dayjs.tz(booking.startTime, 'Asia/Shanghai').valueOf() > dayjs.tz(dayjs(), 'Asia/Shanghai').valueOf()
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
  // UTC 毫秒比较，消除时区歧义
  const nowMs = dayjs.tz(dayjs(), 'Asia/Shanghai').valueOf()
  const slotMs = dayjs.tz(slotTime, 'Asia/Shanghai').valueOf()
  if (b) return { booked: true, 'booking-start': dayjs.tz(b.startTime, 'Asia/Shanghai').valueOf() === slotMs }
  if (slotMs < nowMs) return { past: true }
  if (isInDragRange(roomId, slotTime)) return { free: true, selecting: true }
  return { free: true }
}

function getBookingBlockClass(booking: Booking | null | undefined) {
  if (!booking) return {}
  return {
    'is-own': booking.bookerId === store.userId,
    'is-others': booking.bookerId !== store.userId,
    'is-canceled': booking.status === 'CANCELED',
  }
}

function getBookingBlockStyle(booking: Booking | null | undefined, _roomId: number, slotTime: string): Record<string, string> {
  if (!booking) return { display: 'none' }
  // 纵向位置由 slot 在 timeSlots 中的索引决定
  const slotIdx = timeSlots.value.findIndex((s) => s.time === slotTime)
  if (slotIdx < 0 || dayjs.tz(booking.startTime, 'Asia/Shanghai').valueOf() !== dayjs.tz(slotTime, 'Asia/Shanghai').valueOf()) {
    return { display: 'none' }
  }

  // 计算跨越的 slot 数
  const startMin = dayjs.tz(booking.startTime, 'Asia/Shanghai').valueOf()
  const endMin = dayjs.tz(booking.endTime, 'Asia/Shanghai').valueOf()
  const slotMs = SLOT_MINUTES.value * 60 * 1000
  const slotsCount = Math.round((endMin - startMin) / slotMs)

  // 每个 slot 高度 36px
  const CELL_H = 36
  return {
    top: '2px',
    height: `${slotsCount * CELL_H - 4}px`,
  }
}

// 预留：后续如恢复单击创建，可复用该函数
// function onCellClick(_room: Room, _slot: { time: string; label: string }) {
//   return
// }

async function onBookingClick(booking: Booking) {
  selectedBooking.value = booking
  seriesBookings.value = []
  dialogMode.value = 'view'
  dialogVisible.value = true
  // 如果是重复预约，加载系列全部场次
  if (booking.recurrenceType && booking.recurrenceType !== 'NONE') {
    await loadSeriesBookings(booking)
  }
}

async function loadSeriesBookings(b: Booking) {
  // 系列 ID = parentId（非空时）或自身 ID
  const seriesId = b.parentId ?? b.id
  try {
    const all = await getMyBookings({ bookerId: store.userId, page: 0, size: 500 }) as { content: Booking[] }
    seriesBookings.value = (all.content ?? []).filter(
      (item: Booking) => (item.parentId === seriesId) || (item.id === seriesId && item.seriesIndex === 1),
    ).sort((a: Booking, b: Booking) => a.startTime - b.startTime)
  } catch {}
}

// ── 拖拽选择 ────────────────────────────────────────────
const dragState = ref<{
  active: boolean
  roomId: number | null
  startSlot: { time: string } | null
  currentSlot: { time: string } | null
}>({ active: false, roomId: null, startSlot: null, currentSlot: null })

function isInDragRange(roomId: number, slotTime: string) {
  if (!dragState.value.active || dragState.value.roomId !== roomId || !dragState.value.startSlot) return false
  const startMs = dayjs.tz(dragState.value.startSlot.time, 'Asia/Shanghai').valueOf()
  const currentMs = dayjs.tz(dragState.value.currentSlot?.time ?? dragState.value.startSlot!.time, 'Asia/Shanghai').valueOf()
  const slotMs = dayjs.tz(slotTime, 'Asia/Shanghai').valueOf()
  const minMs = Math.min(startMs, currentMs)
  const maxMs = Math.max(startMs, currentMs)
  return slotMs >= minMs && slotMs <= maxMs
}

function onCellMousedown(room: Room, slot: { time: string }) {
  const b = getBookingAt(room.id, slot.time)
  const nowMs = dayjs.tz(dayjs(), 'Asia/Shanghai').valueOf()
  const slotMs = dayjs.tz(slot.time, 'Asia/Shanghai').valueOf()
  if (b || slotMs < nowMs) return

  dragState.value = { active: true, roomId: room.id, startSlot: slot, currentSlot: slot }
  document.addEventListener('mouseup', onDocumentMouseup)
  document.addEventListener('selectstart', preventSelect)
}

function onCellMouseenter(room: Room, slot: { time: string }) {
  if (!dragState.value.active || dragState.value.roomId !== room.id) return
  dragState.value.currentSlot = slot
}

function onDocumentMouseup() {
  document.removeEventListener('mouseup', onDocumentMouseup)
  document.removeEventListener('selectstart', preventSelect)
  if (!dragState.value.active) return

  const { roomId, startSlot, currentSlot } = dragState.value
  if (roomId && startSlot && currentSlot) {
    const s1 = dayjs.tz(startSlot.time, 'Asia/Shanghai').valueOf()
    const s2 = dayjs.tz(currentSlot.time, 'Asia/Shanghai').valueOf()
    const startMs = Math.min(s1, s2)
    const endMs = Math.max(s1, s2) + SLOT_MINUTES.value * 60 * 1000
    showBookingDialog(undefined, roomId, msToBusinessValueFormat(startMs), msToBusinessValueFormat(endMs))
  }
  dragState.value = { active: false, roomId: null, startSlot: null, currentSlot: null }
}

function preventSelect(e: Event) {
  e.preventDefault()
}
const dialogMode = ref<'create' | 'view'>('create')
const dialogVisible = ref(false)
const selectedBooking = ref<Booking | null>(null)
const seriesBookings = ref<Booking[]>([])
const formRef = ref<FormInstance>()
const submitting = ref(false)
const conflicts = ref<BookingConflictDTO[]>([])

const store = useBookingStore()

function recurrenceLabel(type?: string) {
  const map: Record<string, string> = {
    DAILY: '每天', WEEKLY: '每周', WORKDAY: '工作日', MONTHLY: '每月',
  }
  return type ? (map[type] ?? type) : ''
}

const form = reactive({
  roomId: null as number | null,
  subject: '',
  startTime: '',
  endTime: '',
  recurrenceType: 'NONE',
  recurrenceEndDate: '',
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

function showBookingDialog(
  _event?: MouseEvent,
  prefilledRoomId?: number,
  prefilledTime?: string,
  prefilledEndTime?: string,
) {
  dialogMode.value = 'create'
  conflicts.value = []
  Object.assign(form, {
    roomId: prefilledRoomId ?? (rooms.value[0]?.id ?? null),
    subject: '',
    startTime: prefilledTime ?? '',
    endTime: prefilledEndTime ?? (prefilledTime ? dayjs(prefilledTime).add(1, 'hour').format('YYYY-MM-DDTHH:mm:ss') : ''),
    recurrenceType: 'NONE',
    recurrenceEndDate: '',
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
    // Date 字符串 → UTC 毫秒
    const startMs = dayjs.tz(form.startTime, 'Asia/Shanghai').valueOf()
    const endMs = dayjs.tz(form.endTime, 'Asia/Shanghai').valueOf()

    // 先做冲突预检
    const cr = await checkConflict({
      roomId: form.roomId!,
      startTime: startMs,
      endTime: endMs,
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
      startTime: startMs,
      endTime: endMs,
      attendeeCount: form.attendeeCount,
      remark: form.remark?.trim() || undefined,
      recurrenceType: form.recurrenceType as any,
      recurrenceEndDate: form.recurrenceType !== 'NONE' && form.recurrenceEndDate
        ? dayjs(form.recurrenceEndDate).format('YYYY-MM-DD') as any
        : undefined,
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

async function onCancelSeries(booking: Booking) {
  await ElMessageBox.confirm('取消全部系列预约？此操作不可恢复。', '取消确认', { type: 'warning' })
  await cancelBooking(booking.id, store.userId, true)
  ElMessage.success('已取消全部系列预约')
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
    const [bData, rData, cfg] = await Promise.all([
      getBuildings(),
      getRooms({
        buildingId: selectedBuildingId.value,
        status: 'ENABLED',
      }),
      getBookingRules(null),
    ])
    buildings.value = bData
    rooms.value = rData
    roomConfig.value = cfg

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
/* ── MRBS 风格配色变量 ─────────────────────────────────── */
.calendar-view {
  --slot-even: #ffffff;
  --slot-odd:  #f4f5f7;
  --slot-border: #e2e4e9;

  --book-own-bg:     #d1fae5;
  --book-own-border: #16a34a;
  --book-own-text:   #15803d;

  --book-other-bg:     #eff6ff;
  --book-other-border: #3b82f6;
  --book-other-text:   #1d4ed8;

  --book-pending-bg:      #fef9c3;
  --book-pending-border:  #eab308;
  --book-pending-text:    #a16207;

  --book-canceled-bg:   #f3f4f6;
  --book-canceled-text: #9ca3af;

  --drag-bg:   rgba(59, 130, 246, 0.10);
  --past-bg:   #e8edf4;
  --today-bg:  rgba(239, 68, 68, 0.05);

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
  border: 1px solid var(--slot-border);
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
  border-right: 1px solid var(--slot-border);
  border-bottom: 1px solid var(--slot-border);
}

.room-header {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--el-bg-color-page);
  border-right: 1px solid var(--slot-border);
  border-bottom: 2px solid var(--slot-border);
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
  border-right: 1px solid var(--slot-border);
  border-bottom: 1px solid var(--slot-border);
  padding: 0 6px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  height: 36px;
  display: flex;
  align-items: center;
  text-align: right;
  justify-content: flex-end;
}

/* ── 格子：MRBS 灰白交替背景 ─────────────────────────── */
/* nth-child(2n) 对应横向第2格开始（corner 占第1格），偶数格白，基数格浅灰 */
.cell {
  height: 36px;
  border-right: 1px solid var(--slot-border);
  border-bottom: 1px solid var(--slot-border);
  position: relative;
  cursor: pointer;
  transition: background 0.1s;
}

.cell:last-child {
  border-right: none;
}

/* 每个房间列各自独立交替：同列同色，形成竖向斑马条纹 */
.cell:nth-child(4n + 2) { background: var(--slot-even); } /* 偶数行偶数列 = 白 */
.cell:nth-child(4n + 3) { background: var(--slot-odd);  } /* 奇数行偶数列 = 浅灰 */
.cell:nth-child(4n + 4) { background: var(--slot-even); } /* 偶数行奇数列 = 白 */
.cell:nth-child(4n + 5) { background: var(--slot-odd);  } /* 奇数行奇数列 = 浅灰 */

.cell:hover:not(.booked) {
  background: var(--drag-bg);
}

.cell.past {
  background: var(--past-bg);
  cursor: not-allowed;
}

.cell.selecting {
  background: var(--drag-bg);
}

.cell.free:hover::after {
  content: '+';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #3b82f6;
  font-weight: 700;
  pointer-events: none;
}

/* ── 预约色块（MRBS 风格：绿色=本人，蓝色=他人）────────── */
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
  background: var(--book-own-bg);
  border-left: 3px solid var(--book-own-border);
  color: var(--book-own-text);
}

.booking-block.is-others {
  background: var(--book-other-bg);
  border-left: 3px solid var(--book-other-border);
  color: var(--book-other-text);
}

.booking-block.is-canceled {
  background: var(--book-canceled-bg);
  border-left: 3px solid #d1d5db;
  color: var(--book-canceled-text);
  text-decoration: line-through;
}

.booking-subject {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recurring-badge {
  font-size: 12px;
  margin-right: 2px;
  flex-shrink: 0;
}

.is-own .recurring-badge    { color: var(--book-own-text); }
.is-others .recurring-badge { color: var(--book-other-text); }

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

.series-list {
  margin-top: 12px;
  padding: 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.series-list-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
