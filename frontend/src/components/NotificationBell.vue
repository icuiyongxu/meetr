<template>
  <div class="notif-bell-wrap">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" type="danger">
      <el-button circle plain size="default" @click="togglePanel">
        <el-icon :size="16"><Bell /></el-icon>
      </el-button>
    </el-badge>

    <el-popover
      :visible="panelVisible"
      placement="bottom-end"
      :width="380"
      trigger="click"
      @update:visible="(v: boolean) => (panelVisible = v)"
    >
      <template #reference>
        <span />
      </template>

      <template #default>
        <div class="notif-panel">
          <div class="notif-header">
            <span class="notif-title">通知</span>
            <el-button
              v-if="unreadCount > 0"
              link
              type="primary"
              size="small"
              :loading="markingAll"
              @click="onMarkAllRead"
            >
              全部已读
            </el-button>
          </div>

          <div class="notif-list">
            <el-skeleton v-if="loading" :rows="4" animated />
            <template v-else-if="upcomingNotifications.length">
              <div
                v-for="n in upcomingNotifications"
                :key="n.id"
                class="notif-item"
                :class="{ unread: !n.isRead }"
                @click="onItemClick(n)"
              >
                <div class="notif-item-icon" :style="{ color: eventIconColor(n.eventType) }">
                  <el-icon :size="18"><component :is="eventIcon(n.eventType)" /></el-icon>
                </div>
                <div class="notif-item-body">
                  <div class="notif-item-title">{{ n.title }}</div>
                  <div v-if="n.bookingStartTimeMs" class="notif-item-meeting-time">
                    <el-icon :size="12"><Clock /></el-icon>
                    {{ formatMeetingTime(n.bookingStartTimeMs) }}
                  </div>
                  <div class="notif-item-time">收到通知: {{ formatTime(n.createdAtMs) }}</div>
                </div>
                <el-button
                  v-if="!n.isRead"
                  link
                  size="small"
                  :loading="readingId === n.id"
                  @click.stop="onMarkRead(n)"
                >
                  已读
                </el-button>
              </div>
            </template>
            <el-empty v-else description="暂无通知" :image-size="60" />
          </div>
        </div>
      </template>
    </el-popover>

    <el-dialog v-model="detailVisible" title="通知详情" width="420px" append-to-body destroy-on-close>
      <div v-if="selectedNotification" class="notif-detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="类型">
            <el-tag size="small">{{ eventTypeLabel(selectedNotification.eventType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题">{{ selectedNotification.title }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedNotification.bookingStartTimeMs" label="会议开始">
            {{ formatMeetingTime(selectedNotification.bookingStartTimeMs) }}
          </el-descriptions-item>
          <el-descriptions-item label="收到时间">{{ formatTime(selectedNotification.createdAtMs) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <pre class="notif-content">{{ selectedNotification.content }}</pre>
      </div>
      <template #footer>
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
        <el-button v-if="selectedNotification?.bookingId" type="primary" size="small" @click="goToBooking">
          查看预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Plus, Edit, Close, Check, Clock, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { Notification } from '@/types/notification'
import { getNotifications, getUnreadCount, markAsRead, markAllRead } from '@/api/notification'
import { useBookingStore } from '@/stores/booking'

const store = useBookingStore()
const router = useRouter()

const panelVisible = ref(false)
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const markingAll = ref(false)
const readingId = ref<number | null>(null)
const detailVisible = ref(false)
const selectedNotification = ref<Notification | null>(null)

const upcomingNotifications = computed(() =>
  (notifications.value || []).filter((n) => {
    if (!n.bookingEndTimeMs) return true
    return n.bookingEndTimeMs > Date.now()
  }),
)

const eventTypeMap: Record<string, string> = {
  BOOKING_CREATED: '预约创建',
  BOOKING_UPDATED: '预约变更',
  BOOKING_CANCELED: '预约取消',
  BOOKING_APPROVAL_REQUIRED: '待审批',
  BOOKING_APPROVED: '预约通过',
  BOOKING_REJECTED: '预约驳回',
  BOOKING_REMINDER: '会议提醒',
}
function eventTypeLabel(type: string) {
  return eventTypeMap[type] ?? type
}

function eventIcon(eventType: string) {
  switch (eventType) {
    case 'BOOKING_CREATED': return Plus
    case 'BOOKING_UPDATED': return Edit
    case 'BOOKING_CANCELED': return Close
    case 'BOOKING_APPROVAL_REQUIRED': return Bell
    case 'BOOKING_APPROVED': return Check
    case 'BOOKING_REMINDER': return Timer
    default: return Bell
  }
}

function eventIconColor(eventType: string) {
  switch (eventType) {
    case 'BOOKING_CREATED': return '#16a34a'
    case 'BOOKING_UPDATED': return '#3b82f6'
    case 'BOOKING_CANCELED': return '#ef4444'
    case 'BOOKING_APPROVAL_REQUIRED': return '#f59e0b'
    case 'BOOKING_APPROVED': return '#16a34a'
    case 'BOOKING_REJECTED': return '#ef4444'
    case 'BOOKING_REMINDER': return '#f59e0b'
    default: return '#409eff'
  }
}

function formatMeetingTime(ms: number) {
  const d = new Date(ms)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const timeStr = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  if (isToday) return `今天 ${timeStr}`
  return `${d.getMonth() + 1}月${d.getDate()}日 ${timeStr}`
}

function formatTime(ms: number) {
  const diff = Date.now() - ms
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  const d = new Date(ms)
  return `${d.getMonth() + 1}-${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function goToBooking() {
  if (!selectedNotification.value?.bookingId) return
  detailVisible.value = false
  panelVisible.value = false
  router.push(`/my-bookings?bookingId=${selectedNotification.value.bookingId}`)
}

const POLL_INTERVAL = 10_000
let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadUnread() {
  if (!store.isLoggedIn) return
  try {
    unreadCount.value = (await getUnreadCount(store.userId)) ?? 0
  } catch (e) {
    console.error('[NotificationBell] loadUnread failed:', e)
  }
}

async function loadList() {
  if (!store.isLoggedIn) return
  loading.value = true
  try {
    const page = await getNotifications(store.userId, 0, 20)
    notifications.value = page?.list ?? []
    await loadUnread()
  } catch (e) {
    console.error('[NotificationBell] loadList failed:', e)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

async function togglePanel() {
  panelVisible.value = !panelVisible.value
  if (panelVisible.value) {
    await loadList()
  }
}

async function onMarkRead(n: Notification) {
  if (n.isRead) return
  readingId.value = n.id
  try {
    await markAsRead(n.id)
    n.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch {
    ElMessage.error('操作失败')
  } finally {
    readingId.value = null
  }
}

async function onMarkAllRead() {
  markingAll.value = true
  try {
    await markAllRead(store.userId)
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

function onItemClick(n: Notification) {
  selectedNotification.value = n
  detailVisible.value = true
  if (!n.isRead) {
    onMarkRead(n)
  }
  panelVisible.value = false
}

function startPolling() {
  if (pollTimer) return
  loadUnread()
  pollTimer = setInterval(loadUnread, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(
  () => store.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) startPolling()
    else stopPolling()
  },
  { immediate: true },
)

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.notif-bell-wrap {
  display: inline-flex;
  align-items: center;
}

.notif-panel {
  margin: -12px;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.notif-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.notif-list {
  max-height: 400px;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.1s;
}

.notif-item:hover {
  background: var(--el-fill-color-light);
}

.notif-item.unread {
  background: var(--el-color-primary-light-9);
}

.notif-item-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.notif-item-body {
  flex: 1;
  min-width: 0;
}

.notif-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
}

.notif-item-meeting-time {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 2px;
}

.notif-item-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.notif-content {
  font-family: inherit;
  font-size: 14px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  line-height: 1.7;
}
</style>
