<template>
  <el-card shadow="never">
    <template #header>
      <div class="hdr">
        <span>预约表单</span>
        <el-tag v-if="roomId" type="info" effect="plain">roomId: {{ roomId }}</el-tag>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="会议主题" prop="subject">
        <el-input v-model="form.subject" placeholder="必填" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="选择开始时间"
          format="YYYY-MM-DD HH:mm"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          placeholder="选择结束时间"
          format="YYYY-MM-DD HH:mm"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="参会人数" prop="attendeeCount">
        <el-input-number v-model="form.attendeeCount" :min="1" :max="999" />
      </el-form-item>

      <el-form-item label="参会人" prop="attendeeIds">
        <el-select
          v-model="form.attendeeIds"
          multiple
          filterable
          placeholder="搜索并选择参会人"
          style="width: 100%"
          :loading="usersLoading"
        >
          <el-option
            v-for="u in allUsers"
            :key="u.userId"
            :label="u.name + ' (' + u.userId + ')'"
            :value="u.userId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交预约</el-button>
        <el-button :disabled="submitting" @click="resetToInitial">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { checkConflict, createBooking } from '@/api/booking'
import { getUsers } from '@/api/user'
import { useBookingStore } from '@/stores/booking'
import type { UserItem } from '@/api/user'
import type { BookingConflictDTO } from '@/types/booking'

const props = defineProps<{
  roomId: number
  initialSlot?: { start: Date; end: Date }
}>()

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'conflict', conflicts: BookingConflictDTO[]): void
}>()

const store = useBookingStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const allUsers = ref<UserItem[]>([])
const usersLoading = ref(false)

const initial = computed(() => {
  const nowPlus = dayjs().add(1, 'hour').startOf('minute').toDate()
  const endPlus = dayjs(nowPlus).add(1, 'hour').toDate()
  return {
    subject: '',
    startTime: props.initialSlot?.start || nowPlus,
    endTime: props.initialSlot?.end || endPlus,
    attendeeCount: 1,
    attendeeIds: [] as string[],
    remark: '',
  }
}

const form = reactive({
  subject: '',
  startTime: new Date(),
  endTime: new Date(),
  attendeeCount: 1,
  attendeeIds: [] as string[],
  remark: '',
})

function resetToInitial() {
  const v = initial.value
  form.subject = v.subject
  form.startTime = v.startTime
  form.endTime = v.endTime
  form.attendeeCount = v.attendeeCount
  form.attendeeIds = v.attendeeIds
  form.remark = v.remark
  emit('conflict', [])
}

watch(
  () => props.initialSlot,
  (slot) => {
    if (!slot) return
    form.startTime = slot.start
    form.endTime = slot.end
    emit('conflict', [])
  },
)

resetToInitial()

onMounted(async () => {
  usersLoading.value = true
  try {
    allUsers.value = await getUsers()
  } finally {
    usersLoading.value = false
  }
})

const rules: FormRules = {
  subject: [{ required: true, message: '请输入会议主题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  attendeeCount: [{ required: true, message: '请输入参会人数', trigger: 'change' }],
}

function validateLocal() {
  const start = dayjs(form.startTime)
  const end = dayjs(form.endTime)
  if (!start.isValid() || !end.isValid()) {
    ElMessage.error('开始/结束时间无效')
    return false
  }
  if (!start.isBefore(end)) {
    ElMessage.error('开始时间必须早于结束时间')
    return false
  }
  if (start.isBefore(dayjs())) {
    ElMessage.error('开始时间不能在过去')
    return false
  }
  return true
}

async function onSubmit() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  if (!validateLocal()) return

  submitting.value = true
  try {
    // Date → UTC 毫秒
    const startMs = dayjs.tz(form.startTime, 'Asia/Shanghai').valueOf()
    const endMs = dayjs.tz(form.endTime, 'Asia/Shanghai').valueOf()

    const conflictRes = await checkConflict({
      roomId: props.roomId,
      startTime: startMs,
      endTime: endMs,
    })

    const conflicts = conflictRes.conflictingBookings || []
    emit('conflict', conflictRes.conflict ? conflicts : [])
    if (conflictRes.conflict) {
      ElMessage.error('该时间段已有预约，请调整时间')
      return
    }

    // 如果后端对齐了时间，用 UTC 毫秒转回 Date 展示
    if (conflictRes.alignedStartTime && conflictRes.alignedEndTime) {
      form.startTime = new Date(conflictRes.alignedStartTime)
      form.endTime = new Date(conflictRes.alignedEndTime)
    }

    const result = await createBooking({
      roomId: props.roomId,
      subject: form.subject.trim(),
      bookerId: store.userId,
      bookerName: store.userName || undefined,
      startTime: dayjs.tz(form.startTime, 'Asia/Shanghai').valueOf(),
      endTime: dayjs.tz(form.endTime, 'Asia/Shanghai').valueOf(),
      attendeeCount: form.attendeeCount,
      attendeeIds: form.attendeeIds,
      remark: form.remark?.trim() || undefined,
    })

    if (!result.success) {
      if (result.conflicts?.length) {
        emit('conflict', result.conflicts)
        ElMessage.error('提交失败：存在冲突')
        return
      }
      if (result.violations?.length) {
        ElMessage.error(result.violations.map((v) => v.message).join('；'))
        return
      }
      ElMessage.error('提交失败')
      return
    }

    ElMessage.success('预约成功')
    emit('conflict', [])
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
