<template>
  <div>
    <div class="meetr-page-title">规则配置</div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="selectedRoomId" filterable clearable placeholder="选择会议室（不选为全局）" style="width: 360px">
          <el-option v-for="r in roomOptions" :key="r.id" :label="`${r.name}（${r.buildingName || r.buildingId}）`" :value="r.id" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-form ref="formRef" :model="form" label-width="160px" style="max-width: 760px" v-loading="loading">
        <el-form-item label="时间粒度 resolution">
          <el-select v-model="resolutionMinutes" style="width: 240px">
            <el-option label="15 分钟" :value="15" />
            <el-option label="30 分钟" :value="30" />
            <el-option label="60 分钟" :value="60" />
          </el-select>
          <el-text type="info" style="margin-left: 10px">后端单位为秒</el-text>
        </el-form-item>

        <el-form-item label="默认时长（分钟）">
          <el-input-number v-model="form.defaultDuration" :min="1" :max="1440" />
        </el-form-item>

        <el-form-item label="开放时间（开始-结束）">
          <div class="tw">
            <el-time-select v-model="form.morningStarts" start="00:00" step="00:15" end="23:45" />
            <span class="dash">-</span>
            <el-time-select v-model="form.eveningEnds" start="00:00" step="00:15" end="23:45" />
          </div>
        </el-form-item>

        <el-form-item label="最少提前预约（分钟）">
          <el-input-number v-model="form.minBookAheadMinutes" :min="0" :max="99999" />
        </el-form-item>

        <el-form-item label="最多提前预约（天）">
          <el-input-number v-model="form.maxBookAheadDays" :min="0" :max="3650" />
        </el-form-item>

        <el-form-item label="最短预约时长（分钟）">
          <el-input-number v-model="form.minDurationMinutes" :min="1" :max="1440" />
        </el-form-item>

        <el-form-item label="最长预约时长（分钟）">
          <el-input-number v-model="form.maxDurationMinutes" :min="1" :max="1440" />
        </el-form-item>

        <el-form-item label="同一天最多预约次数">
          <el-input-number v-model="form.maxPerDay" :min="1" :max="999" />
        </el-form-item>

        <el-form-item label="同一周最多预约次数">
          <el-input-number v-model="form.maxPerWeek" :min="1" :max="9999" />
        </el-form-item>

        <el-form-item label="是否需要审批">
          <el-switch v-model="form.approvalRequired" />
        </el-form-item>
      </el-form>

      <div class="footer">
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { getRooms } from '@/api/room'
import { getBookingRules, saveBookingRules } from '@/api/config'
import type { Room } from '@/types/room'

const roomOptions = ref<Room[]>([])
const selectedRoomId = ref<number | null>(null)

const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  roomId: null as number | null,
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

const resolutionMinutes = computed({
  get: () => Math.round(form.resolution / 60),
  set: (v: number) => {
    form.resolution = v * 60
  },
})

function parseHHmm(s: string) {
  const [h, m] = s.split(':').map((x) => Number(x))
  return (h || 0) * 60 + (m || 0)
}

function validate() {
  const s = parseHHmm(form.morningStarts)
  const e = parseHHmm(form.eveningEnds)
  if (s >= e) {
    ElMessage.error('开放时间：开始必须早于结束')
    return false
  }
  if (form.minDurationMinutes > form.maxDurationMinutes) {
    ElMessage.error('最短时长不能大于最长时长')
    return false
  }
  return true
}

async function loadRoomsOptions() {
  roomOptions.value = await getRooms({})
}

async function load() {
  loading.value = true
  try {
    const roomId = selectedRoomId.value
    const cfg = await getBookingRules(roomId)
    form.roomId = roomId
    form.resolution = cfg.resolution ?? 1800
    form.defaultDuration = cfg.defaultDuration ?? 60
    form.morningStarts = cfg.morningStarts ?? '08:00'
    form.eveningEnds = cfg.eveningEnds ?? '22:00'
    form.minBookAheadMinutes = cfg.minBookAheadMinutes ?? 0
    form.maxBookAheadDays = cfg.maxBookAheadDays ?? 30
    form.minDurationMinutes = cfg.minDurationMinutes ?? 15
    form.maxDurationMinutes = cfg.maxDurationMinutes ?? 480
    form.maxPerDay = cfg.maxPerDay ?? 3
    form.maxPerWeek = cfg.maxPerWeek ?? 10
    form.approvalRequired = cfg.approvalRequired ?? false
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!validate()) return
  saving.value = true
  try {
    await saveBookingRules({
      roomId: selectedRoomId.value,
      resolution: form.resolution,
      defaultDuration: form.defaultDuration,
      morningStarts: form.morningStarts,
      eveningEnds: form.eveningEnds,
      minBookAheadMinutes: form.minBookAheadMinutes,
      maxBookAheadDays: form.maxBookAheadDays,
      minDurationMinutes: form.minDurationMinutes,
      maxDurationMinutes: form.maxDurationMinutes,
      maxPerDay: form.maxPerDay,
      maxPerWeek: form.maxPerWeek,
      approvalRequired: form.approvalRequired,
      status: 'ENABLED',
    })
    ElMessage.success('已保存')
    await load()
  } finally {
    saving.value = false
  }
}

watch(selectedRoomId, () => {
  load()
})

onMounted(async () => {
  await loadRoomsOptions()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tw {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dash {
  color: var(--el-text-color-secondary);
}

.footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

