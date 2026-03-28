<template>
  <div>
    <div class="meetr-page-title">审批队列</div>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select v-model="filters.buildingId" clearable placeholder="楼栋" style="width: 180px" @change="onBuildingChange">
          <el-option v-for="item in buildings" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>

        <el-select v-model="filters.roomId" clearable placeholder="会议室" style="width: 200px">
          <el-option v-for="item in roomOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>

        <el-input v-model="filters.bookerId" clearable placeholder="预约人工号" style="width: 160px" @keyup.enter="search" />
        <el-input v-model="filters.keyword" clearable placeholder="主题关键字" style="width: 200px" @keyup.enter="search" />

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          clearable
        />

        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="bookings" v-loading="loading" border>
        <el-table-column prop="subject" label="主题" min-width="180" />
        <el-table-column prop="buildingName" label="楼栋" width="140" />
        <el-table-column prop="roomName" label="会议室" width="160" />
        <el-table-column label="预约人" width="140">
          <template #default="{ row }">
            {{ row.bookerName || row.bookerId }}
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">
            {{ formatBusinessDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">
            {{ formatBusinessDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="attendeeCount" label="参会人数" width="100" align="center" />
        <el-table-column label="创建状态" width="110" align="center">
          <template #default>
            <el-tag type="warning">待审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近审批" width="200">
          <template #default="{ row }">
            <template v-if="row.lastApprovalAtMs">
              <div>{{ row.lastApprovalOperatorName || row.lastApprovalOperatorId || '-' }}</div>
              <div style="color: #909399; font-size: 12px">{{ formatBusinessDateTime(row.lastApprovalAtMs) }}</div>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canApprove" label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" link @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" link @click="handleReject(row)">驳回</el-button>
            <el-button type="primary" size="small" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :current-page="page + 1"
          :total="total"
          @current-change="(p: number) => changePage(p - 1)"
          @size-change="changeSize"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="预约详情" width="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="主题">{{ detail.subject }}</el-descriptions-item>
          <el-descriptions-item label="楼栋">{{ detail.buildingName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="会议室">{{ detail.roomName || detail.roomId }}</el-descriptions-item>
          <el-descriptions-item label="预约人">{{ detail.bookerName || detail.bookerId }}</el-descriptions-item>
          <el-descriptions-item label="时间范围">{{ formatRange(detail.startTime, detail.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="参会人数">{{ detail.attendeeCount }}</el-descriptions-item>
          <el-descriptions-item label="参会人">
            <template v-if="detail.attendees?.length">
              <el-tag
                v-for="item in detail.attendees"
                :key="item.userId || item.userName || item.id || item.name"
                size="small"
                style="margin-right: 6px; margin-bottom: 6px"
              >
                {{ item.userName || item.userId || item.name || item.id }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批记录">
            <div v-if="detailLogs.length">
              <div v-for="log in detailLogs" :key="log.id" style="margin-bottom: 6px">
                <strong>{{ log.operationType }}</strong>
                <span style="margin-left: 6px">{{ log.operatorName || log.operatorId || '-' }}</span>
                <span style="margin-left: 8px; color: #909399">{{ formatBusinessDateTime(log.createdAtMs) }}</span>
                <div style="margin-top: 2px; color: #606266">{{ log.content }}</div>
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkVisible" :title="remarkMode === 'approve' ? '审批通过' : '审批驳回'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="approvalRemark" type="textarea" :rows="4" placeholder="可填写审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Booking } from '@/types/booking'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'
import { approveBooking, getBookingDetail, getPendingBookings, rejectBooking } from '@/api/booking'
import type { BookingDetail, BookingOperationLog } from '@/types/booking-detail'
import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import { useBookingStore } from '@/stores/booking'
import { businessDayEndMs, businessDayStartMs, formatBusinessDateTime, formatRange } from '@/utils/datetime'

const store = useBookingStore()
const canApprove = computed(() => store.isAdmin)
const loading = ref(false)
const bookings = ref<Booking[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = ref(10)

const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const dateRange = ref<[string, string] | null>(null)

const filters = ref<{
  buildingId?: number
  roomId?: number
  bookerId?: string
  keyword?: string
}>({})

const roomOptions = computed(() => {
  if (!filters.value.buildingId) return rooms.value
  return rooms.value.filter((item) => item.buildingId === filters.value.buildingId)
})

async function loadBaseOptions() {
  const [buildingData, roomData] = await Promise.all([
    getBuildings(),
    getRooms({ status: 'ENABLED' }),
  ])
  buildings.value = buildingData
  rooms.value = roomData
}

async function load() {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value ?? [null, null]
    const resp = await getPendingBookings({
      buildingId: filters.value.buildingId,
      roomId: filters.value.roomId,
      bookerId: filters.value.bookerId || undefined,
      keyword: filters.value.keyword || undefined,
      startDateMs: startDate ? businessDayStartMs(startDate) : undefined,
      endDateMs: endDate ? businessDayEndMs(endDate) : undefined,
      page: page.value,
      size: pageSize.value,
    })
    bookings.value = resp.content || []
    total.value = resp.totalElements ?? bookings.value.length
  } finally {
    loading.value = false
  }
}

function onBuildingChange() {
  if (filters.value.roomId) {
    const exists = roomOptions.value.some((item) => item.id === filters.value.roomId)
    if (!exists) filters.value.roomId = undefined
  }
}

async function search() {
  page.value = 0
  await load()
}

async function reset() {
  filters.value = {}
  dateRange.value = null
  page.value = 0
  pageSize.value = 10
  await load()
}

function changePage(nextPage: number) {
  page.value = nextPage
  load()
}

function changeSize(nextSize: number) {
  pageSize.value = nextSize
  page.value = 0
  load()
}

async function handleApprove(row: Booking) {
  if (!canApprove.value) {
    ElMessage.warning('当前无权限审批预约')
    return
  }
  remarkMode.value = 'approve'
  remarkTarget.value = row
  approvalRemark.value = ''
  remarkVisible.value = true
}

async function handleReject(row: Booking) {
  if (!canApprove.value) {
    ElMessage.warning('当前无权限审批预约')
    return
  }
  remarkMode.value = 'reject'
  remarkTarget.value = row
  approvalRemark.value = ''
  remarkVisible.value = true
}

async function submitApproval() {
  if (!remarkTarget.value) return
  if (remarkMode.value === 'reject' && !approvalRemark.value.trim()) {
    ElMessage.warning('驳回时请填写原因')
    return
  }
  if (remarkMode.value === 'approve') {
    await approveBooking(remarkTarget.value.id, store.userId, approvalRemark.value.trim() || undefined)
    ElMessage.success('审批通过')
  } else {
    await rejectBooking(remarkTarget.value.id, store.userId, approvalRemark.value.trim())
    ElMessage.success('已驳回')
  }
  remarkVisible.value = false
  remarkTarget.value = null
  approvalRemark.value = ''
  await load()
}

const approvalRemark = ref('')
const remarkVisible = ref(false)
const remarkMode = ref<'approve' | 'reject'>('approve')
const remarkTarget = ref<Booking | null>(null)

async function openDetail(row: Booking) {
  detailVisible.value = true
  const resp: BookingDetail = await getBookingDetail(row.id)
  detail.value = resp.booking
  detailLogs.value = resp.operationLogs || []
}

onMounted(async () => {
  await loadBaseOptions()
  await load()
})
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
