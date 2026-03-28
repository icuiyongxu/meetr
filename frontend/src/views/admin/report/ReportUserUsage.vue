<template>
  <div>
    <div class="meetr-page-title">用户预约统计</div>

    <el-card shadow="never">
      <div class="search-bar">
        <el-radio-group v-model="period" @change="load">
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

        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="report" v-loading="loading" stripe>
        <el-table-column prop="bookerName" label="姓名" width="140" />
        <el-table-column prop="bookerId" label="工号" width="130" />
        <el-table-column prop="totalBookings" label="预约总数" width="100" align="center" />
        <el-table-column prop="validBookings" label="有效预约" width="100" align="center">
          <template #default="{ row }">
            <span style="color: #67c23a">{{ row.validBookings }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalMinutes" label="总时长(分钟)" width="120" align="center">
          <template #default="{ row }">{{ row.totalMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column prop="canceledCount" label="取消次数" width="100" align="center">
          <template #default="{ row }"><span style="color:#909399">{{ row.canceledCount }}</span></template>
        </el-table-column>
        <el-table-column prop="rejectedCount" label="被驳回" width="100" align="center">
          <template #default="{ row }"><span style="color:#f56c6c">{{ row.rejectedCount }}</span></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserUsage } from '@/api/report'
import type { UserUsage } from '@/api/report'
import dayjs from 'dayjs'
import isToday from 'dayjs/plugin/isToday'
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore'
import weekday from 'dayjs/plugin/weekday'
dayjs.extend(isToday); dayjs.extend(isSameOrBefore); dayjs.extend(weekday)

const period = ref('month')
const dateRange = ref<[string, string] | null>(null)
const report = ref<UserUsage[]>([])
const loading = ref(false)

function currentRange(): [string, string] {
  const today = dayjs()
  if (period.value === 'week') {
    return [today.startOf('week').format('YYYY-MM-DD'), today.endOf('week').format('YYYY-MM-DD')]
  }
  if (period.value === 'month') {
    return [today.startOf('month').format('YYYY-MM-DD'), today.endOf('month').format('YYYY-MM-DD')]
  }
  return dateRange.value ?? [today.subtract(30, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
}

async function load() {
  loading.value = true
  try {
    const [startDate, endDate] = currentRange()
    report.value = await getUserUsage({ startDate, endDate })
  } finally {
    loading.value = false
  }
}

onMounted(load)
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
