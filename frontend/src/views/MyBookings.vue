<template>
  <div>
    <div class="meetr-page-title">我的预约</div>

    <el-card shadow="never">
      <el-tabs v-model="active" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="今日" name="today" />
        <el-tab-pane label="待审批" name="pending" />
      </el-tabs>

      <BookingList :bookings="viewBookings" :loading="loading" @view="openDetail" @cancel="askCancel" />

      <div class="pager" v-if="showPagination">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          :current-page="page + 1"
          :total="total"
          @current-change="(p) => changePage(p - 1)"
          @size-change="(s) => changeSize(s)"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="预约详情" width="520px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会议室">
            {{ detail.roomName || detail.roomId }}
          </el-descriptions-item>
          <el-descriptions-item label="时间段">
            {{ formatRange(detail.startTime, detail.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="主题">{{ detail.subject }}</el-descriptions-item>
          <el-descriptions-item label="参会人数">{{ detail.attendeeCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag effect="plain">{{ detail.status }}</el-tag>
            <el-tag effect="plain" style="margin-left: 8px">{{ detail.approvalStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ detail.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-skeleton v-else :rows="6" animated />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookingList from '@/components/BookingList.vue'
import type { Booking } from '@/types/booking'
import { cancelBooking, getBooking, getMyBookings, getTodayBookings } from '@/api/booking'
import { useBookingStore } from '@/stores/booking'
import { formatRange } from '@/utils/datetime'

type TabKey = 'all' | 'today' | 'pending'

const store = useBookingStore()

const active = ref<TabKey>('all')
const loading = ref(false)

const allBookings = ref<Booking[]>([])
const todayBookings = ref<Booking[]>([])
const pendingAll = ref<Booking[]>([])

const page = ref(0)
const size = ref(10)
const total = ref(0)

const viewBookings = computed(() => {
  if (active.value === 'today') return todayBookings.value
  if (active.value === 'pending') {
    const start = page.value * size.value
    return pendingAll.value.slice(start, start + size.value)
  }
  return allBookings.value
})

const showPagination = computed(() => active.value !== 'today')

async function loadAll() {
  loading.value = true
  try {
    const p = await getMyBookings({ bookerId: store.userId, page: page.value, size: size.value })
    allBookings.value = p.content || []
    total.value = p.totalElements ?? allBookings.value.length
  } finally {
    loading.value = false
  }
}

async function loadToday() {
  loading.value = true
  try {
    todayBookings.value = await getTodayBookings(store.userId)
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  loading.value = true
  try {
    // backend has no "pending only" endpoint, so fetch a larger page and filter
    const p = await getMyBookings({ bookerId: store.userId, page: 0, size: 200 })
    pendingAll.value = (p.content || []).filter((b: Booking) => b.approvalStatus === 'PENDING')
    total.value = pendingAll.value.length
  } finally {
    loading.value = false
  }
}

async function reload() {
  if (active.value === 'today') return loadToday()
  if (active.value === 'pending') return loadPending()
  return loadAll()
}

function onTabChange() {
  page.value = 0
  total.value = 0
  reload()
}

function changePage(p: number) {
  page.value = p
  if (active.value === 'all') reload()
}

function changeSize(s: number) {
  size.value = s
  page.value = 0
  reload()
}

const detailVisible = ref(false)
const detail = ref<Booking | null>(null)

async function openDetail(b: Booking) {
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await getBooking(b.id)
  } catch {
    detailVisible.value = false
  }
}

async function askCancel(b: Booking) {
  try {
    await ElMessageBox.confirm('确认取消该预约？', '提示', {
      confirmButtonText: '取消预约',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch {
    return
  }

  await cancelBooking(b.id, store.userId)
  ElMessage.success('已取消')
  await reload()
}
</script>

<style scoped>
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
