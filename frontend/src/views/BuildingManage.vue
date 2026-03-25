<template>
  <div>
    <div class="meetr-page-title">楼栋管理</div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreate">+ 新增楼栋</el-button>
      </div>

      <el-table :data="buildings" v-loading="loading" style="width: 100%">
        <el-table-column label="楼栋名称" min-width="200">
          <template #default="{ row }">
            <div class="name">{{ row.name }}</div>
            <div v-if="row.address" class="sub">{{ row.address }}</div>
          </template>
        </el-table-column>
        <el-table-column label="园区/地点" prop="campus" min-width="160" />
        <el-table-column label="排序" prop="sortNo" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑楼栋' : '新增楼栋'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="form.name" placeholder="必填，如：总部大楼A座" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="园区/地点" prop="campus">
          <el-input v-model="form.campus" placeholder="如：总部园区" maxlength="100" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="如：北京市朝阳区xxx" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
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
import { getBuildings, createBuilding, updateBuilding } from '@/api/building'
import type { Building } from '@/types/building'

const buildings = ref<Building[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  campus: '',
  address: '',
  sortNo: 0,
  status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE',
})

const enabledSwitch = computed({
  get: () => form.status === 'ACTIVE',
  set: (v: boolean) => (form.status = v ? 'ACTIVE' : 'INACTIVE'),
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    buildings.value = await getBuildings()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.campus = ''
  form.address = ''
  form.sortNo = 0
  form.status = 'ACTIVE'
  dialogVisible.value = true
}

function openEdit(row: Building) {
  editingId.value = row.id
  form.name = row.name
  form.campus = row.campus || ''
  form.address = row.address || ''
  form.sortNo = row.sortNo
  form.status = row.status
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      campus: form.campus.trim() || undefined,
      address: form.address.trim() || undefined,
      sortNo: form.sortNo,
      status: form.status,
    }
    if (editingId.value) {
      await updateBuilding(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await createBuilding(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: Building) {
  await ElMessageBox.confirm(`确定删除楼栋「${row.name}」吗？该操作不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  try {
    await updateBuilding(row.id, { ...row, status: 'INACTIVE' } as any)
    ElMessage.success('已停用（数据保留）')
    await load()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.name {
  font-weight: 600;
}

.sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
