<template>
  <div>
    <div class="meetr-page-title">会议室管理</div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索名称/楼层/备注" clearable style="width: 260px" />
        <el-select v-model="buildingId" clearable placeholder="全部楼栋" style="width: 220px">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        <el-button v-if="canManage" :disabled="loading" @click="openCreate">新增会议室</el-button>
      </div>

      <el-table :data="rooms" v-loading="loading" style="width: 100%">
        <el-table-column label="名称" min-width="160">
          <template #default="{ row }">
            <div class="nm">{{ row.name }}</div>
            <div class="sub" v-if="row.remark">{{ row.remark }}</div>
          </template>
        </el-table-column>

        <el-table-column label="楼栋/楼层" min-width="160">
          <template #default="{ row }">
            <div>{{ row.buildingName || row.buildingId }}</div>
            <div class="sub" v-if="row.floor">楼层 {{ row.floor }}</div>
          </template>
        </el-table-column>

        <el-table-column label="容量" width="90">
          <template #default="{ row }">{{ row.capacity }}</template>
        </el-table-column>

        <el-table-column label="设备" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="k in row.equipment" :key="k" size="small" effect="plain" class="tag">
              {{ equipmentLabel(k) }}
            </el-tag>
            <el-text v-if="!row.equipment?.length" type="info">无</el-text>
          </template>
        </el-table-column>

        <el-table-column v-if="canManage" label="启用" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ENABLED'"
              :loading="row._updating"
              @change="(v: boolean | string | number) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" :width="canManage ? 180 : 100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManage" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" plain @click="openKiosk(row)">大屏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑会议室' : '新增会议室'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="所属楼栋" prop="buildingId">
          <el-select v-model="form.buildingId" filterable placeholder="选择楼栋" style="width: 100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input v-model="form.floor" placeholder="如 10F / B1" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="设备" prop="equipmentItems">
          <el-select v-model="form.equipmentItems" multiple clearable placeholder="选择设备" style="width: 100%">
            <el-option v-for="o in equipmentOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" />
        </el-form-item>
        <el-form-item label="启用" prop="status">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getBuildings } from '@/api/building'
import { createRoom, getRooms, updateRoom, updateRoomStatus } from '@/api/room'
import { getEquipments, type Equipment } from '@/api/equipment'
import { useBookingStore } from '@/stores/booking'
import type { Building } from '@/types/building'
import type { Room } from '@/types/room'

type RoomRow = Room & { _updating?: boolean }

const store = useBookingStore()
const canManage = computed(() => store.isAdmin)

const buildings = ref<Building[]>([])
const equipmentList = ref<Equipment[]>([])
const rooms = ref<RoomRow[]>([])
const loading = ref(false)
const keyword = ref('')
const buildingId = ref<number | undefined>(undefined)

const equipmentOptions = computed(() =>
  equipmentList.value
    .filter(e => e.status === 'ACTIVE')
    .map(e => ({ label: e.name, value: e.code }))
)

function equipmentLabel(key: string) {
  return equipmentList.value.find(e => e.code === key)?.name || key
}

async function load() {
  loading.value = true
  try {
    const list = await getRooms({
      buildingId: buildingId.value,
      keyword: keyword.value.trim() || undefined,
    })
    rooms.value = list.map((r) => ({ ...r, _updating: false }))
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: RoomRow, enabled: boolean | string | number) {
  const next = enabled ? 'ENABLED' : 'DISABLED'
  row._updating = true
  try {
    await updateRoomStatus(row.id, next)
    row.status = next
    ElMessage.success('已更新状态')
  } finally {
    row._updating = false
  }
}

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const formRef = ref<FormInstance>()
const form = reactive({
  buildingId: 0,
  name: '',
  floor: '',
  capacity: 1,
  equipmentItems: [] as string[],
  remark: '',
  status: 'ENABLED' as 'ENABLED' | 'DISABLED',
})

const enabledSwitch = computed({
  get: () => form.status === 'ENABLED',
  set: (v: boolean) => (form.status = v ? 'ENABLED' : 'DISABLED'),
})

const rules: FormRules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'change', validator: (_rule, val, cb) => cb(val >= 1 ? undefined : new Error('容量不能小于1')) }],
}

function openCreate() {
  if (!buildings.value.length) {
    ElMessageBox.confirm('当前没有任何楼栋，请先创建楼栋', '提示', {
      confirmButtonText: '去创建楼栋',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      ElMessage.info('请通过"楼栋管理"页面先创建楼栋')
    }).catch(() => {})
    return
  }
  editingId.value = null
  form.buildingId = buildings.value[0]?.id || 0
  form.name = ''
  form.floor = ''
  form.capacity = 1
  form.equipmentItems = []
  form.remark = ''
  form.status = 'ENABLED'
  dialogVisible.value = true
}

function openEdit(row: RoomRow) {
  editingId.value = row.id
  form.buildingId = row.buildingId
  form.name = row.name
  form.floor = row.floor || ''
  form.capacity = row.capacity
  form.equipmentItems = [...(row.equipment || [])]
  form.remark = row.remark || ''
  form.status = row.status
  dialogVisible.value = true
}

function openKiosk(row: RoomRow) {
  window.open(`/kiosk/room/${row.id}`, '_blank')
}

async function save() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  saving.value = true
  try {
    const payload = {
      buildingId: form.buildingId,
      name: form.name.trim(),
      floor: form.floor.trim() || undefined,
      capacity: form.capacity,
      equipmentItems: form.equipmentItems,
      remark: form.remark.trim() || undefined,
      status: form.status,
    }
    if (editingId.value) {
      await updateRoom(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createRoom(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  buildings.value = await getBuildings()
  equipmentList.value = await getEquipments()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.nm {
  font-weight: 600;
}

.sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.tag {
  margin-right: 6px;
  margin-bottom: 6px;
}
</style>
