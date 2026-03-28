<template>
  <div>
    <div class="meetr-page-title">预约记录</div>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select v-model="filters.buildingId" clearable filterable placeholder="楼栋" style="width:160px" @change="onBuildingChange">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>

        <el-select v-model="filters.roomId" clearable filterable placeholder="会议室" style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>

        <el-input v-model="filters.bookerId" clearable placeholder="预约人" style="width:140px" />
        <el-input v-model="filters.keyword" clearable placeholder="主题关键字" style="width:180px" />

        <el-select v-model="filters.status" clearable placeholder="预约状态" style="width:130px">
          <el-option label="已确认" value="BOOKED" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>

        <el-select v-model="filters.approvalStatus" clearable placeholder="审批状态" style="width:140px">
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="待审批" value="PENDING" />
          <el-option label="无需审批" value="NONE" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width:260px"
          clearable
        />

        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="exportExcel">导出 Excel</el-button>
      </div>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column prop="subject" label="主题" min-width="180" />
        <el-table-column prop="buildingName" label="楼栋" width="130" />
        <el-table-column prop="roomName" label="会议室" width="150" />
        <el-table-column prop="bookerName" label="预约人" width="120" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ fmtDate(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ fmtDate(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" align="center" />
        <el-table-column label="预约状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'BOOKED' ? 'success' : 'info'">
              {{ row.status === 'BOOKED' ? '已确认' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 'APPROVED'" size="small" type="success">已通过</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'REJECTED'" size="small" type="danger">已驳回</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'PENDING'" size="small" type="warning">待审批</el-tag>
            <el-tag v-else size="small">无需审批</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :page-size="pageSize"
          :current-page="page + 1"
          :total="total"
          @current-change="(p: number) => changePage(p - 1)"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import { getBookingRecords } from '@/api/report'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'
import type { BookingRecord } from '@/api/report'
import { formatBusinessDateTime } from '@/utils/datetime'

const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const records = ref<BookingRecord[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = ref(20)
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)

const filters = ref<{
  buildingId?: number
  roomId?: number
  bookerId?: string
  keyword?: string
  status?: string
  approvalStatus?: string
}>({})

const roomOptions = computed(() =>
  filters.value.buildingId
    ? rooms.value.filter((r) => r.buildingId === filters.value.buildingId)
    : rooms.value,
)

function fmtDate(ms: number) {
  return formatBusinessDateTime(ms)
}

async function load() {
  loading.value = true
  try {
    const resp = await getBookingRecords({
      buildingIds: filters.value.buildingId ? String(filters.value.buildingId) : undefined,
      roomIds: filters.value.roomId ? String(filters.value.roomId) : undefined,
      bookerId: filters.value.bookerId || undefined,
      keyword: filters.value.keyword || undefined,
      status: filters.value.status || undefined,
      approvalStatus: filters.value.approvalStatus || undefined,
      page: page.value,
      size: pageSize.value,
    })
    records.value = (resp as any).content || []
    total.value = (resp as any).totalElements ?? 0
  } finally {
    loading.value = false
  }
}

function onBuildingChange() {
  filters.value.roomId = undefined
}

function search() {
  page.value = 0
  load()
}

function reset() {
  filters.value = {}
  dateRange.value = null
  page.value = 0
  load()
}

function changePage(p: number) {
  page.value = p
  load()
}

function exportExcel() {
  ElMessage.info('Excel 导出开发中（待实现）')
}

onMounted(async () => {
  ;[buildings.value, rooms.value] = await Promise.all([getBuildings(), getRooms()])
  await load()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
