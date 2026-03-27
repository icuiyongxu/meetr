<template>
  <div class="kiosk">
    <!-- 顶部导航栏 -->
    <div class="kiosk-header">
      <div class="kiosk-header__left">
        <span class="kiosk-header__room">{{ data?.room?.buildingName }} · {{ data?.room?.name }}</span>
      </div>
      <div class="kiosk-header__right">
        <span class="kiosk-header__date">{{ data?.date }}</span>
        <span class="kiosk-header__clock">{{ currentTime }}</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="kiosk-body">
      <!-- 加载中 -->
      <div v-if="loading" class="kiosk-loading">
        <div class="kiosk-loading__spinner" />
        <span>加载中...</span>
      </div>

      <!-- 无预约 -->
      <div v-else-if="!currentBooking && upcomingBookings.length === 0" class="kiosk-empty">
        <div class="kiosk-empty__icon">📅</div>
        <div class="kiosk-empty__title">今日无预约</div>
        <div class="kiosk-empty__sub">会议室空闲，欢迎使用</div>
      </div>

      <!-- 有预约 -->
      <template v-else>
        <!-- 当前会议（如果有） -->
        <div v-if="currentBooking" class="kiosk-current">
          <div class="kiosk-current__badge">进行中</div>
          <div class="kiosk-current__time">
            {{ formatTimeRange(currentBooking.startTimeMs, currentBooking.endTimeMs) }}
          </div>
          <div class="kiosk-current__subject">{{ currentBooking.subject }}</div>
          <div class="kiosk-current__meta">
            <span>{{ currentBooking.bookerName }}</span>
            <span v-if="currentBooking.attendeeCount">{{ currentBooking.attendeeCount }}人</span>
          </div>
          <div class="kiosk-current__progress">
            <div class="kiosk-current__progress-bar" :style="{ width: currentProgress + '%' }" />
          </div>
        </div>

        <!-- 即将开始 / 今日其余预约 -->
        <div v-if="upcomingBookings.length > 0" class="kiosk-upcoming">
          <div class="kiosk-upcoming__title">今日预约</div>
          <div class="kiosk-upcoming__list">
            <div
              v-for="b in upcomingBookings"
              :key="b.id"
              class="kiosk-card"
              :class="cardClass(b)"
            >
              <div class="kiosk-card__time">
                {{ formatTimeRange(b.startTimeMs, b.endTimeMs) }}
              </div>
              <div class="kiosk-card__info">
                <div class="kiosk-card__subject">{{ b.subject }}</div>
                <div class="kiosk-card__meta">
                  {{ b.bookerName }}
                  <span v-if="b.attendeeCount"> · {{ b.attendeeCount }}人</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 底部刷新倒计时 -->
    <div class="kiosk-footer">
      <div class="kiosk-footer__left">
        <span class="kiosk-footer__label">自动刷新</span>
        <div class="kiosk-footer__bar">
          <div class="kiosk-footer__bar-fill" :style="{ width: refreshProgress + '%' }" />
        </div>
        <span class="kiosk-footer__countdown">{{ countdown }}s</span>
      </div>
      <div class="kiosk-footer__right">
        <span class="kiosk-footer__refresh" @click="load">刷新</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getKioskData } from '@/api/kiosk'
import type { KioskResponse } from '@/api/kiosk'

const REFRESH_INTERVAL = 30 // 秒

const route = useRoute()
const roomId = Number(route.params.roomId)

const loading = ref(true)
const data = ref<KioskResponse | null>(null)
const now = ref(Date.now())
const countdown = ref(REFRESH_INTERVAL)
const refreshProgress = computed(() => ((REFRESH_INTERVAL - countdown.value) / REFRESH_INTERVAL) * 100)

// 当前时间字符串
const currentTime = computed(() => {
  const d = new Date(now.value)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`
})

// 当前会议
const currentBooking = computed(() =>
  (data.value?.bookings ?? []).find(b => b.status === 'IN_PROGRESS') ?? null
)

// 当前会议进度
const currentProgress = computed(() => {
  if (!currentBooking.value) return 0
  const total = currentBooking.value.endTimeMs - currentBooking.value.startTimeMs
  const elapsed = now.value - currentBooking.value.startTimeMs
  return Math.min(100, Math.max(0, Math.round((elapsed / total) * 100)))
})

// 即将开始 + 尚未开始的
const upcomingBookings = computed(() =>
  (data.value?.bookings ?? []).filter(b => b.status === 'UPCOMING')
)

function cardClass(b: any) {
  return {
    'kiosk-card--ending': b.endTimeMs - now.value < 15 * 60_000, // 15分钟内结束
  }
}

function formatTimeRange(startMs: number, endMs: number) {
  const fmt = (ms: number) => {
    const d = new Date(ms)
    return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
  }
  return `${fmt(startMs)} - ${fmt(endMs)}`
}

async function load() {
  try {
    data.value = await getKioskData(roomId)
  } catch (e) {
    console.error('[Kiosk] load failed', e)
  } finally {
    loading.value = false
  }
}

let ticker: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await load()
  countdown.value = REFRESH_INTERVAL

  // 每秒更新时间（显示时钟）
  ticker = setInterval(() => {
    now.value = Date.now()
    countdown.value = Math.max(0, countdown.value - 1)
  }, 1000)

  // 定时刷新
  refreshTimer = setInterval(async () => {
    countdown.value = REFRESH_INTERVAL
    await load()
  }, REFRESH_INTERVAL * 1000)
})

onUnmounted(() => {
  if (ticker) clearInterval(ticker)
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
/* ===== 全局 ===== */
.kiosk {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0f1117;
  color: #fff;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

/* ===== 顶部 ===== */
.kiosk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 40px;
  background: linear-gradient(135deg, #1a1f2e 0%, #0f1117 100%);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.kiosk-header__room {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.kiosk-header__right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.kiosk-header__date {
  font-size: 22px;
  color: rgba(255,255,255,0.6);
}

.kiosk-header__clock {
  font-size: 40px;
  font-weight: 700;
  color: #60a5fa;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;
}

/* ===== 主内容 ===== */
.kiosk-body {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== 加载 ===== */
.kiosk-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: rgba(255,255,255,0.5);
  font-size: 20px;
}
.kiosk-loading__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(255,255,255,0.1);
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 无预约 ===== */
.kiosk-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.kiosk-empty__icon { font-size: 80px; opacity: 0.5; }
.kiosk-empty__title { font-size: 36px; font-weight: 600; color: rgba(255,255,255,0.8); }
.kiosk-empty__sub { font-size: 20px; color: rgba(255,255,255,0.4); }

/* ===== 当前会议 ===== */
.kiosk-current {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
  border-radius: 20px;
  padding: 36px 40px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.kiosk-current__badge {
  display: inline-block;
  background: rgba(255,255,255,0.25);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 100px;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.kiosk-current__time {
  font-size: 28px;
  color: rgba(255,255,255,0.85);
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}

.kiosk-current__subject {
  font-size: 56px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  margin-bottom: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kiosk-current__meta {
  display: flex;
  gap: 16px;
  font-size: 22px;
  color: rgba(255,255,255,0.8);
}

.kiosk-current__progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: rgba(255,255,255,0.2);
}

.kiosk-current__progress-bar {
  height: 100%;
  background: #fff;
  transition: width 1s linear;
}

/* ===== 即将开始 ===== */
.kiosk-upcoming__title {
  font-size: 18px;
  color: rgba(255,255,255,0.4);
  margin-bottom: 16px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.kiosk-upcoming__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.kiosk-card {
  display: flex;
  align-items: center;
  gap: 24px;
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid rgba(255,255,255,0.08);
  transition: background 0.2s;
}

.kiosk-card--ending {
  border-color: rgba(251,191,36,0.4);
  background: rgba(251,191,36,0.08);
}

.kiosk-card--ending .kiosk-card__time {
  color: #fbbf24;
}

.kiosk-card__time {
  font-size: 22px;
  font-weight: 600;
  color: #60a5fa;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  min-width: 160px;
}

.kiosk-card__info {
  flex: 1;
  min-width: 0;
}

.kiosk-card__subject {
  font-size: 26px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.kiosk-card__meta {
  font-size: 16px;
  color: rgba(255,255,255,0.45);
}

/* ===== 底部 ===== */
.kiosk-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  background: rgba(0,0,0,0.3);
  border-top: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
}

.kiosk-footer__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kiosk-footer__label {
  font-size: 14px;
  color: rgba(255,255,255,0.35);
  white-space: nowrap;
}

.kiosk-footer__bar {
  width: 200px;
  height: 4px;
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
  overflow: hidden;
}

.kiosk-footer__bar-fill {
  height: 100%;
  background: #60a5fa;
  transition: width 1s linear;
}

.kiosk-footer__countdown {
  font-size: 16px;
  color: rgba(255,255,255,0.5);
  font-variant-numeric: tabular-nums;
  min-width: 36px;
}

.kiosk-footer__refresh {
  font-size: 14px;
  color: rgba(255,255,255,0.35);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.1);
  transition: all 0.15s;
}
.kiosk-footer__refresh:hover {
  color: #fff;
  border-color: rgba(255,255,255,0.3);
}
</style>
