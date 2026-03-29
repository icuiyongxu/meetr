<template>
  <div>
    <div class="meetr-page-title">预约全局搜索</div>

    <el-card shadow="never">
      <!-- 搜索工具栏 -->
      <div class="search-bar">
        <el-input v-model="filters.keyword" clearable placeholder="主题关键字" style="width:180px" @keyup.enter="search" />

        <el-select v-model="filters.buildingId" clearable filterable placeholder="楼栋" style="width:160px" @change="onBuildingChange">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>

        <el-select v-model="filters.roomId" clearable filterable placeholder="会议室" style="width:180px">
          <el-option v-for="r in roomOptions" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>

        <el-input v-model="filters.bookerId" clearable placeholder="预约人ID" style="width:130px" />

        <el-select v-model="filters.status" clearable placeholder="预约状态" style="width:130px">
          <el-option label="已确认" value="BOOKED" />
          <el-option label="已取消" value="CANCELED" />
          <el-option label="已完成" value="FINISHED" />
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
      </div>

      <!-- 搜索结果 -->
      <div class="result-info" v-if="hasSearched">
        <span>共找到 <strong>{{ total }}</strong> 条预约记录</span>
        <el-button type="success" size="small" @click="exportExcel" style="margin-left:12px">导出 Excel</el-button>
      </div>

      <el-table :data="records" v-loading="loading" stripe style="width:100%;margin-top:12px" :row-class-name="rowClass">
        <el-table-column prop="subject" label="主题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="buildingName" label="楼栋" min-width="100" />
        <el-table-column prop="roomName" label="会议室" min-width="130" />
        <el-table-column prop="bookerName" label="预约人" min-width="90" />
        <el-table-column prop="bookerId" label="预约人ID" min-width="90" />
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ fmtDate(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ fmtDate(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长" align="center" width="80">
          <template #default="{ row }">{{ row.durationMinutes ?? Math.round((row.endTime - row.startTime) / 60000) }}</template>
        </el-table-column>
        <el-table-column label="预约状态" align="center" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" align="center" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="approvalTagType(row.approvalStatus)">{{ approvalLabel(row.approvalStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="prev,pager,next,total"
          :page-size="pageSize"
          :total="total"
          :current-page="currentPage + 1"
          @current-change="onPageChange"
        />
      </div>

      <el-empty v-if="hasSearched && records.length === 0 && !loading" description="未找到符合条件的预约记录" />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="520px" append-to-body destroy-on-close>
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="主题">{{ detail.subject }}</el-descriptions-item>
        <el-descriptions-item label="会议室">{{ detail.roomName }}</el-descriptions-item>
        <el-descriptions-item label="预约人">{{ detail.bookerName }}</el-descriptions-item>
        <el-descriptions-item label="预约人ID">{{ detail.bookerId }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ fmtDate(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ fmtDate(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="时长">{{ detail.durationMinutes }} 分钟</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ detail.attendeeCount }} 人</el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <el-tag size="small" :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag size="small" :type="approvalTagType(detail.approvalStatus)">{{ approvalLabel(detail.approvalStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { Booking } from '@/types/booking'
import { searchAdminBookings } from '@/api/booking'
import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import { formatBusinessDateTime } from '@/utils/datetime'

const buildings = ref<any[]>([])
const roomOptions = ref<any[]>([])

const filters = ref({
  keyword: '',
  buildingId: undefined as number | undefined,
  roomId: undefined as number | undefined,
  bookerId: '',
  status: '',
  approvalStatus: '',
})

const dateRange = ref<[string, string] | null>(null)
const records = ref<Booking[]>([])
const loading = ref(false)
const hasSearched = ref(false)
const total = ref(0)
const pageSize = 20
const currentPage = ref(0)

const detailVisible = ref(false)
const detail = ref<Booking | null>(null)

onMounted(async () => {
  buildings.value = await getBuildings()
})

function onBuildingChange() {
  filters.value.roomId = undefined
  if (filters.value.buildingId) {
    getRooms({ buildingId: filters.value.buildingId }).then((res) => {
      roomOptions.value = res
    })
  } else {
    roomOptions.value = []
  }
}

function buildParams() {
  const [from, to] = dateRange.value ?? [null, null]
  return {
    keyword: filters.value.keyword || undefined,
    roomId: filters.value.roomId || undefined,
    bookerId: filters.value.bookerId || undefined,
    status: filters.value.status || undefined,
    approvalStatus: filters.value.approvalStatus || undefined,
    startTimeFrom: from ? new Date(from + 'T00:00:00+08:00').getTime() : undefined,
    startTimeTo: to ? new Date(to + 'T23:59:59+08:00').getTime() : undefined,
    page: currentPage.value,
    size: pageSize,
  }
}

async function search() {
  loading.value = true
  hasSearched.value = true
  try {
    const res = await searchAdminBookings(buildParams())
    records.value = res.content ?? []
    total.value = res.totalElements ?? 0
  } catch (e: any) {
    ElMessage.error(e?.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.value = { keyword: '', buildingId: undefined, roomId: undefined, bookerId: '', status: '', approvalStatus: '' }
  dateRange.value = null
  currentPage.value = 0
  records.value = []
  hasSearched.value = false
  total.value = 0
}

function onPageChange(page: number) {
  currentPage.value = page - 1
  search()
}

function viewDetail(row: Booking) {
  detail.value = row
  detailVisible.value = true
}

function fmtDate(ms: number) {
  return formatBusinessDateTime(ms, 'YYYY-MM-DD HH:mm')
}

function statusTagType(status?: string) {
  return status === 'BOOKED' ? 'success' : status === 'CANCELED' ? 'info' : ''
}
function statusLabel(status?: string) {
  return status === 'BOOKED' ? '已确认' : status === 'CANCELED' ? '已取消' : status === 'FINISHED' ? '已完成' : status ?? '-'
}
function approvalTagType(status?: string) {
  return status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : status === 'PENDING' ? 'warning' : 'info'
}
function approvalLabel(status?: string) {
  return status === 'APPROVED' ? '已通过' : status === 'REJECTED' ? '已驳回' : status === 'PENDING' ? '待审批' : status === 'NONE' ? '无需审批' : status ?? '-'
}

function rowClass({ row }: { row: Booking }) {
  if (row.approvalStatus === 'REJECTED') return 'row-rejected'
  if (row.approvalStatus === 'PENDING') return 'row-pending'
  return ''
}

function exportExcel() {
  // 导出当前筛选条件的前1000条（分页导出）
  const params = { ...buildParams(), size: 1000 }
  searchAdminBookings(params).then((res) => {
    const rows = res.content ?? []
    if (rows.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }
    const header = ['主题', '楼栋', '会议室', '预约人', '预约人ID', '开始时间', '结束时间', '时长(分钟)', '预约状态', '审批状态', '备注']
    const lines = rows.map((r: any) => [
      r.subject, r.buildingName, r.roomName, r.bookerName, r.bookerId,
      fmtDate(r.startTime), fmtDate(r.endTime), r.durationMinutes ?? '-',
      statusLabel(r.status), approvalLabel(r.approvalStatus), r.remark ?? '',
    ])
    const csv = [header, ...lines]
      .map((line) => line.map((cell: any) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
      .join('\n')
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预约搜索_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${rows.length} 条记录`)
  })
}
</script>

<style scoped>
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.result-info {
  margin-top: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
:deep(.row-rejected td) {
  background-color: #fef0f0 !important;
}
:deep(.row-pending td) {
  background-color: #fdf6ec !important;
}
</style>
