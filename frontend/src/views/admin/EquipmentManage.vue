<template>
  <div>
    <div class="meetr-page-title">设备管理</div>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">设备列表</span>
          <el-button v-if="canManage()" type="primary" size="small" @click="openCreateDialog">新增设备</el-button>
        </div>
      </template>

      <el-table :data="equipments" v-loading="loading" border>
        <el-table-column prop="code" label="编码" width="200" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage()" label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning"
              size="small"
              link
              @click="toggleStatus(row, 'DISABLED')"
            >停用</el-button>
            <el-button
              v-else
              type="success"
              size="small"
              link
              @click="toggleStatus(row, 'ACTIVE')"
            >启用</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增设备' : '编辑设备'" width="400">
      <el-form :model="form" label-width="70" style="max-width: 320px">
        <el-form-item label="编码" v-if="formMode === 'create'">
          <el-input v-model="form.code" placeholder="如 projector、tv" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="显示名称，如 投影仪" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSaving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipments, createEquipment, updateEquipment, deleteEquipment, type Equipment } from '@/api/equipment'
import { useBookingStore } from '@/stores/booking'

const store = useBookingStore()
const canManage = () => store.isAdmin

const equipments = ref<Equipment[]>([])
const loading = ref(false)

// --- 新建/编辑 ---
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formSaving = ref(false)
const form = ref({ id: 0, code: '', name: '' })

function openCreateDialog() {
  if (!canManage()) {
    ElMessage.warning('当前无权限管理设备')
    return
  }
  formMode.value = 'create'
  form.value = { id: 0, code: '', name: '' }
  formVisible.value = true
}

function openEditDialog(row: Equipment) {
  if (!canManage()) {
    ElMessage.warning('当前无权限管理设备')
    return
  }
  formMode.value = 'edit'
  form.value = { id: row.id, code: row.code, name: row.name }
  formVisible.value = true
}

async function submitForm() {
  if (!form.value.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  formSaving.value = true
  try {
    if (formMode.value === 'create') {
      if (!form.value.code.trim()) {
        ElMessage.warning('编码不能为空')
        return
      }
      await createEquipment({ code: form.value.code.trim(), name: form.value.name.trim() })
      ElMessage.success('创建成功')
    } else {
      await updateEquipment(form.value.id, { name: form.value.name.trim() })
      ElMessage.success('保存成功')
    }
    formVisible.value = false
    await load()
  } catch {
    // error handled by API layer
  } finally {
    formSaving.value = false
  }
}

// --- 启用/停用 ---
async function toggleStatus(row: Equipment, status: string) {
  const action = status === 'ACTIVE' ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}设备「${row.name}」？`, '提示', { type: 'warning' })
  await updateEquipment(row.id, { status })
  ElMessage.success(`${action}成功`)
  await load()
}

// --- 删除 ---
async function handleDelete(row: Equipment) {
  await ElMessageBox.confirm(`确认删除设备「${row.name}」？`, '警告', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteEquipment(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  loading.value = true
  try {
    equipments.value = await getEquipments()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.panel-title {
  font-weight: 600;
  font-size: 15px;
}
</style>
