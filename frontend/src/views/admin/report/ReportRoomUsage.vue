<template>
  <div>
    <div class="meetr-page-title">会议室使用率统计</div>

    <el-card shadow="never">
      <div class="search-bar">
        <el-radio-group v-model="period" @change="onPeriodChange">
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="custom">自定义</el-radio-button>
        </el-radio-group>

        <el-date-picker
          v-if="period === 'custom'"
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="load"
        />

        <el-select v-model="selectedBuildingId" clearable filterable placeholder="全部楼栋" style="min-width: 160px" @change="load">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>

        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="report" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="roomName" label="会议室" min-width="160" />
        <el-table-column prop="buildingName" label="楼栋" min-width="120" />
        <el-table-column prop="totalBookings" label="预约次数" align="center" />
        <el-table-column prop="totalMinutes" label="总时长(分钟)" align="center">
          <template #default="{ row }">{{ row.totalMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column prop="usagePercent" label="使用率" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.usagePercent" :precision="1" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="canceledCount" label="取消次数" align="center">
          <template #default="{ row }"><span style="color:#909399">{{ row.canceledCount }}</span></template>
        </el-table-column>
        <el-table-column prop="pendingCount" label="待审批" align="center">
          <template #default="{ row }"><span style="color:#f59e0b">{{ row.pendingCount }}</span></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBuildings } from '@/api/building'
import { getRoomUsage } from '@/api/report'
import type { Building } from '@/types/building'
import type { RoomUsage } from '@/types/report'
import dayjs from 'dayjs'
import isToday from 'dayjs/plugin/isToday'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import weekday from 'dayjs/plugin/weekday'
dayjs.extend(isToday); dayjs.extend(isSameOrBefore); dayjs.extend(weekday)

const period = ref('week')
const dateRange = ref<[string, string] | null>(null)
const selectedBuildingId = ref<number | undefined>()
const buildings = ref<Building[]>([])
const report = ref<RoomUsage[]>([])
const loading = ref(false)

function currentRange(): [string, string] {
  const today = dayjs()
  if (period.value === 'today') {
    return [today.format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
  }
  if (period.value === 'week') {
    return [today.startOf('week').format('YYYY-MM-DD'), today.endOf('week').format('YYYY-MM-DD')]
  }
  if (period.value === 'month') {
    return [today.startOf('month').format('YYYY-MM-DD'), today.endOf('month').format('YYYY-MM-DD')]
  }
  return dateRange.value ?? [today.subtract(30, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
}

function onPeriodChange() {
  load()
}

async function load() {
  loading.value = true
  try {
    const [startDate, endDate] = currentRange()
    report.value = await getRoomUsage({ startDate, endDate, buildingId: selectedBuildingId.value })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  buildings.value = await getBuildings()
  await load()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
</style>
