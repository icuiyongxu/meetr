<template>
  <el-dialog
    v-model="visible"
    :title="seriesTitle"
    width="780px"
    :close-on-click-modal="false"
  >
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="series">

      <!-- 主预约基础信息 -->
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="会议室">{{ series.master.roomName }}</el-descriptions-item>
        <el-descriptions-item label="主题">{{ series.master.subject }}</el-descriptions-item>
        <el-descriptions-item label="重复类型">{{ recurrenceLabel(series.master.recurrenceType) }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ series.master.recurrenceEndDate || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 修改操作栏 -->
      <div class="action-bar">
        <span class="action-label">修改范围：</span>
        <el-radio-group v-model="updateScope" size="small">
          <el-radio-button value="ONCE">仅本次</el-radio-button>
          <el-radio-button value="FUTURE">本次及后续</el-radio-button>
          <el-radio-button value="ALL">整个系列</el-radio-button>
        </el-radio-group>
        <el-divider direction="vertical" />
        <span class="action-label">新主题：</span>
        <el-input v-model="updateSubject" placeholder="不填则保持原值" size="small" style="width: 160px" clearable />
        <el-button type="primary" size="small" :loading="submittingUpdate" @click="onUpdateSeries">
          确认修改
        </el-button>
      </div>

      <!-- 系列实例列表 -->
      <div class="section-title">系列预约（共 {{ series.totalCount }} 场）</div>

      <el-table :data="allSeriesBookings" stripe size="small">
        <el-table-column label="#" width="50" prop="seriesIndex" />
        <el-table-column label="日期" :formatter="(r: any) => formatDate(r.startTime)" />
        <el-table-column label="时间" :formatter="(r: any) => formatTimeRange(r.startTime, r.endTime)" />
        <el-table-column label="预约状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="approvalTagType(row.approvalStatus)">{{ approvalLabel(row.approvalStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
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
      <el-button type="danger" plain :loading="submittingCancel" @click="onShowCancelDialog">
        取消系列
      </el-button>
    </template>

    <!-- 取消确认弹窗 -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="取消系列预约"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-alert
        title="选择取消范围"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-radio-group v-model="cancelScope" style="margin-bottom: 16px">
        <el-radio value="ONCE">仅取消本次</el-radio>
        <el-radio value="FUTURE">取消本次及后续所有场次</el-radio>
        <el-radio value="ALL">取消整个系列全部场次</el-radio>
      </el-radio-group>
      <div v-if="cancelScope === 'FUTURE'" style="color: #909399; font-size: 12px; margin-bottom: 8px">
        将取消当前及之后所有未开始的场次
      </div>
      <div v-if="cancelScope === 'ALL'" style="color: #f56c6c; font-size: 12px; margin-bottom: 8px">
        将取消整个系列全部 {{ series?.totalCount }} 场次，此操作不可恢复
      </div>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">返回</el-button>
        <el-button type="danger" :loading="submittingCancelAction" @click="onConfirmCancel">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Booking } from '@/types/booking'
import { getSeriesBookings, updateSeries, cancelSeries, type SeriesScope } from '@/api/booking'
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
const submittingUpdate = ref(false)
const submittingCancel = ref(false)
const submittingCancelAction = ref(false)
const skippingId = ref<number | null>(null)

// 修改范围
const updateScope = ref<SeriesScope>('ONCE')
const updateSubject = ref('')

// 取消范围
const cancelDialogVisible = ref(false)
const cancelScope = ref<SeriesScope>('ONCE')

watch(visible, async (v) => {
  if (v && props.bookingId) {
    await loadSeries()
    updateScope.value = 'ONCE'
    cancelScope.value = 'ONCE'
    updateSubject.value = ''
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

function statusTagType(status?: string) {
  const map: Record<string, string> = {
    BOOKED: 'success', CANCELED: 'info', FINISHED: '', PENDING: 'warning',
  }
  return map[status ?? ''] ?? 'info'
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    BOOKED: '已预约', CANCELED: '已取消', FINISHED: '已完成', PENDING: '待审批',
  }
  return status ? (map[status] ?? status) : '-'
}

function approvalTagType(status?: string) {
  const map: Record<string, string> = {
    APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning', NONE: 'info',
  }
  return map[status ?? ''] ?? 'info'
}

function approvalLabel(status?: string) {
  const map: Record<string, string> = {
    APPROVED: '已通过', REJECTED: '已驳回', PENDING: '待审批', NONE: '无需审批',
  }
  return status ? (map[status] ?? status) : '-'
}

// 确认修改（使用 scope 范围）
async function onUpdateSeries() {
  if (!series.value) return
  const master = series.value.master
  await ElMessageBox.confirm(
    `确定按「${scopeLabel(updateScope.value)}」修改吗？`,
    '确认修改',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
  )
  submittingUpdate.value = true
  try {
    await updateSeries(master.id, {
      operatorId: props.bookerId,
      scope: updateScope.value,
      subject: updateSubject.value || undefined,
    })
    ElMessage.success('修改成功')
    updateSubject.value = ''
    await loadSeries()
    emit('series-updated')
  } catch (e: any) {
    ElMessage.error(e?.message || '修改失败')
  } finally {
    submittingUpdate.value = false
  }
}

// 跳过本次（取消单个子预约，等同于 scope=ONCE）
async function onSkipOne(booking: Booking) {
  try {
    await ElMessageBox.confirm(
      `确定跳过 ${formatDate(booking.startTime)} 的这场预约吗？`,
      '跳过本次',
      { type: 'warning', confirmButtonText: '跳过', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  skippingId.value = booking.id
  try {
    await cancelSeries(booking.id, { operatorId: props.bookerId, scope: 'ONCE' })
    ElMessage.success('已跳过')
    await loadSeries()
    emit('series-updated')
  } catch {
    ElMessage.error('操作失败')
  } finally {
    skippingId.value = null
  }
}

function onShowCancelDialog() {
  cancelScope.value = 'ONCE'
  cancelDialogVisible.value = true
}

async function onConfirmCancel() {
  if (!series.value) return
  const scopeLabelMap: Record<string, string> = {
    ONCE: '仅本次', FUTURE: '本次及后续所有场次', ALL: `整个系列全部 ${series.value.totalCount} 场次`,
  }
  try {
    await ElMessageBox.confirm(
      `确定取消（${scopeLabelMap[cancelScope.value]}）吗？`,
      '确认取消',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  submittingCancelAction.value = true
  try {
    await cancelSeries(series.value.master.id, {
      operatorId: props.bookerId,
      scope: cancelScope.value,
    })
    ElMessage.success('已取消')
    cancelDialogVisible.value = false
    await loadSeries()
    emit('series-updated')
  } catch (e: any) {
    ElMessage.error(e?.message || '取消失败')
  } finally {
    submittingCancelAction.value = false
  }
}

function scopeLabel(scope: string) {
  const map: Record<string, string> = {
    ONCE: '仅本次', FUTURE: '本次及后续', ALL: '整个系列',
  }
  return map[scope] ?? scope
}
</script>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  flex-wrap: wrap;
}

.action-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.section-title {
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
