<template>
  <div class="home">
    <div class="meetr-page-title">会议室列表</div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8" :md="7" :lg="6">
        <el-card shadow="never">
          <template #header>
            <div class="panel-title">筛选</div>
          </template>

          <el-form label-width="72px">
            <el-form-item label="楼栋">
              <el-select v-model="filters.buildingId" filterable clearable placeholder="全部楼栋" style="width: 100%">
                <el-option
                  v-for="b in buildings"
                  :key="b.id"
                  :label="b.name"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="容量">
              <el-input-number v-model="filters.capacity" :min="0" :max="999" controls-position="right" />
            </el-form-item>

            <el-form-item label="设备">
              <el-select v-model="filters.equipment" multiple clearable placeholder="不限" style="width: 100%">
                <el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>

            <el-form-item label="状态">
              <el-select v-model="filters.status" clearable placeholder="全部" style="width: 100%">
                <el-option label="可用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>

            <el-form-item label="关键词">
              <el-input v-model="filters.keyword" clearable placeholder="名称/楼层/备注" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="loadRooms">查询</el-button>
              <el-button :disabled="loading" @click="reset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="16" :md="17" :lg="18">
        <el-card shadow="never">
          <template #header>
            <div class="list-header">
              <span>共 {{ filteredRooms.length }} 间</span>
              <el-button text type="primary" :loading="loading" @click="loadRooms">刷新</el-button>
            </div>
          </template>

          <el-skeleton v-if="loading" :rows="8" animated />
          <el-empty v-else-if="!filteredRooms.length" description="暂无会议室" />

          <el-row v-else :gutter="12">
            <el-col
              v-for="room in filteredRooms"
              :key="room.id"
              :xs="24"
              :sm="12"
              :md="12"
              :lg="8"
              :xl="6"
              style="margin-bottom: 12px"
            >
              <RoomCard :room="room" @select="goBooking" />
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBuildings } from '@/api/building'
import { getRooms } from '@/api/room'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'
import RoomCard from '@/components/RoomCard.vue'
import { equipmentOptions } from '@/utils/equipment'

const router = useRouter()

const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const loading = ref(false)

const filters = reactive<{
  buildingId?: number
  capacity?: number
  equipment: string[]
  status?: 'ENABLED' | 'DISABLED'
  keyword?: string
}>({
  buildingId: undefined,
  capacity: undefined,
  equipment: [],
  status: 'ENABLED',
  keyword: '',
})

const filteredRooms = computed(() => {
  const eq = filters.equipment
  if (!eq.length) return rooms.value
  return rooms.value.filter((r) => eq.every((k) => (r.equipment || []).includes(k)))
})

async function loadBuildings() {
  buildings.value = await getBuildings()
}

async function loadRooms() {
  loading.value = true
  try {
    const list = await getRooms({
      buildingId: filters.buildingId,
      capacity: filters.capacity,
      status: filters.status,
      keyword: filters.keyword?.trim() || undefined,
    })
    rooms.value = list
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.buildingId = undefined
  filters.capacity = undefined
  filters.equipment = []
  filters.status = 'ENABLED'
  filters.keyword = ''
  loadRooms()
}

function goBooking(room: Room) {
  router.push(`/booking/${room.id}`)
}

onMounted(async () => {
  await loadBuildings()
  await loadRooms()
})
</script>

<style scoped>
.panel-title {
  font-weight: 600;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>

