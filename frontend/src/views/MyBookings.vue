<template>
  <div>
    <div class="meetr-page-title">我的预约</div>

    <el-card shadow="never">
      <!-- 搜索工具栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会议主题"
          clearable
          style="width: 200px"
          @keyup.enter="doSearch"
        />
        <el-date-picker
          v-model="searchDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          :clearable="true"
        />
        <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="已预约" value="BOOKED" />
          <el-option label="已取消" value="CANCELED" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
        <el-button type="primary" @click="doSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

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
          @current-change="(p: number) => changePage(p - 1)"
          @size-change="(s: number) => changeSize(s)"
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
          <el-descriptions-item v-if="detail.recurrenceType && detail.recurrenceType !== 'NONE'" label="重复">
            {{ recurrenceLabel(detail.recurrenceType) }}，至 {{ detail.recurrenceEndDate }}
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
import type { Booking } from '@/types/booking'
import BookingList from '@/components/BookingList.vue'
import { cancelBooking, getBooking, getMyBookings, getTodayBookings, searchBookings } from '@/api/booking'
import { useBookingStore } from '@/stores/booking'
import { businessDayEndMs, businessDayStartMs, formatRange } from '@/utils/datetime'

type TabKey = 'all' | 'today' | 'pending'

const store = useBookingStore()

function recurrenceLabel(type?: string) {
  const map: Record<string, string> = {
    DAILY: '每天', WEEKLY: '每周', WORKDAY: '工作日', MONTHLY: '每月',
  }
  return type ? (map[type] ?? type) : ''
}

const active = ref<TabKey>('all')
const loading = ref(false)

const allBookings = ref<Booking[]>([])
const todayBookings = ref<Booking[]>([])
const pendingAll = ref<Booking[]>([])

const page = ref(0)
const size = ref(10)
const total = ref(0)

// 搜索状态
const searchKeyword = ref('')
const searchDateRange = ref<[string, string] | null>(null)
const searchStatus = ref<string>('')
const searchMode = ref(false)

const viewBookings = computed<Booking[]>(() => {
  if (active.value === 'today') return todayBookings.value
  if (active.value === 'pending') return pendingAll.value
  return allBookings.value
})

const showPagination = computed(() => searchMode.value || active.value !== 'today')

async function loadAll() {
  loading.value = true
  try {
    const p = await getMyBookings({ bookerId: store.userId, page: page.value, size: size.value }) as { content: Booking[]; totalElements: number }
    allBookings.value = p.content || []
    total.value = p.totalElements ?? allBookings.value.length
  } finally {
    loading.value = false
  }
}

async function loadSearch() {
  loading.value = true
  try {
    const [from, to] = searchDateRange.value ?? [null, null]
    const p = await searchBookings({
      bookerId: store.userId,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value || undefined,
      startTimeFrom: from ? businessDayStartMs(from) : undefined,
      startTimeTo: to ? businessDayEndMs(to) : undefined,
      page: page.value,
      size: size.value,
    }) as { content: Booking[]; totalElements: number }
    allBookings.value = p.content || []
    total.value = p.totalElements ?? allBookings.value.length
  } finally {
    loading.value = false
  }
}

async function doSearch() {
  page.value = 0
  searchMode.value = !!(searchKeyword.value || searchDateRange.value || searchStatus.value)
  await reload()
}

async function resetSearch() {
  searchKeyword.value = ''
  searchDateRange.value = null
  searchStatus.value = ''
  page.value = 0
  searchMode.value = false
  await loadAll()
}

async function loadToday() {
  loading.value = true
  try {
    todayBookings.value = await getTodayBookings(store.userId) as Booking[]
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  loading.value = true
  try {
    // backend has no "pending only" endpoint, so fetch a larger page and filter
    const p = await getMyBookings({ bookerId: store.userId, page: 0, size: 200 }) as { content: Booking[]; totalElements: number }
    pendingAll.value = (p.content || []).filter((b: Booking) => b.approvalStatus === 'PENDING')
    total.value = pendingAll.value.length
  } finally {
    loading.value = false
  }
}

async function reload() {
  if (searchMode.value) return loadSearch()
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
  reload()
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
  const isRecurring = b.recurrenceType && b.recurrenceType !== 'NONE'
  try {
    if (isRecurring) {
      await ElMessageBox.confirm(
        '该预约为重复预约，选择「取消本次」只取消当前场次，选择「取消全部」取消整个系列所有场次。',
        '取消方式',
        {
          confirmButtonText: '取消本次',
          cancelButtonText: '取消全部',
          distinguishCancelAndClose: true,
          type: 'warning',
        }
      )
      await cancelBooking(b.id, store.userId, false)
      ElMessage.success('已取消本次')
    } else {
      await ElMessageBox.confirm('确认取消该预约？', '提示', {
        confirmButtonText: '取消预约',
        cancelButtonText: '返回',
        type: 'warning',
      })
      await cancelBooking(b.id, store.userId)
      ElMessage.success('已取消')
    }
  } catch (action: any) {
    if (action === 'cancel') {
      // 用户点击了"取消全部"（cancelButton）
      await cancelBooking(b.id, store.userId, true)
      ElMessage.success('已取消全部系列预约')
    }
    return
  }
  await reload()
}

onMounted(() => reload())
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
