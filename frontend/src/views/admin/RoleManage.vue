<template>
  <div>
    <div class="meetr-page-title">角色与权限管理</div>

    <el-row :gutter="16">
      <!-- 角色列表 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">角色</span>
              <el-button type="primary" size="small" @click="showCreateRole">新建角色</el-button>
            </div>
          </template>

          <el-radio-group v-model="selectedRoleId" style="width: 100">
            <el-table :data="roles" border highlight-current-row @current-change="(row: SysRole) => { selectedRoleId = row?.id ?? null }">
              <el-table-column label="">
                <template #default="{ row }">
                  <el-radio :value="row.id" style="display: block; margin: 4px 0">{{ row.name }}</el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="code" label="编码" width="100" />
            </el-table>
          </el-radio-group>
        </el-card>
      </el-col>

      <!-- 权限配置 -->
      <el-col :span="16">
        <el-card v-if="selectedRole" shadow="never">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">{{ selectedRole.name }} — 权限配置</span>
              <el-button type="primary" size="small" :loading="savingPerm" @click="savePermissions">
                保存权限
              </el-button>
            </div>
          </template>

          <el-checkbox-group v-model="checkedPermIds">
            <el-row :gutter="8">
              <el-col
                v-for="perm in allPermissions"
                :key="perm.id"
                :span="8"
                style="margin-bottom: 8px"
              >
                <el-checkbox :value="perm.id" :label="perm.id" border style="width: 100%">
                  {{ perm.name }}
                  <div style="font-size: 11px; color: #999">{{ perm.code }}</div>
                </el-checkbox>
              </el-col>
            </el-row>
          </el-checkbox-group>
        </el-card>

        <el-empty v-else description="请选择一个角色" />
      </el-col>
    </el-row>

    <!-- 新建角色对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建角色" width="400">
      <el-form :model="createForm" label-width="80">
        <el-form-item label="角色编码">
          <el-input v-model="createForm.code" placeholder="如 ADMIN、MANAGER" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="createForm.name" placeholder="如 管理员、经理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingRole" @click="createRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { unwrap, http } from '@/api'

interface SysRole {
  id: number
  code: string
  name: string
  description?: string
}

interface SysPermission {
  id: number
  code: string
  name: string
  description?: string
}

const roles = ref<SysRole[]>([])
const allPermissions = ref<SysPermission[]>([])
const selectedRoleId = ref<number | null>(null)
const checkedPermIds = ref<number[]>([])
const savingPerm = ref(false)
const creatingRole = ref(false)
const createDialogVisible = ref(false)
const createForm = ref({ code: '', name: '' })

const selectedRole = computed(() => roles.value.find(r => r.id === selectedRoleId.value) ?? null)

// 用 watch 替代 @row-click，避免 radio 点击触发两次
watch(selectedRoleId, async (newId) => {
  if (newId == null) return
  const permIds = await unwrap<number[]>(http.get(`/admin/roles/${newId}/permissions`))
  checkedPermIds.value = permIds ?? []
})

async function loadRoles() {
  roles.value = await unwrap<SysRole[]>(http.get('/admin/roles') as any)
}

async function loadPermissions() {
  allPermissions.value = await unwrap<SysPermission[]>(http.get('/admin/roles/permissions') as any)
}

async function savePermissions() {
  if (!selectedRoleId.value) return
  savingPerm.value = true
  try {
    await unwrap(http.put(`/admin/roles/${selectedRoleId.value}/permissions`, {
      permissionIds: checkedPermIds.value,
    }))
    ElMessage.success('权限保存成功')
  } finally {
    savingPerm.value = false
  }
}

function showCreateRole() {
  createForm.value = { code: '', name: '' }
  createDialogVisible.value = true
}

async function createRole() {
  if (!createForm.value.code.trim() || !createForm.value.name.trim()) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  creatingRole.value = true
  try {
    await unwrap(http.post('/admin/roles', {
      code: createForm.value.code.trim().toUpperCase(),
      name: createForm.value.name.trim(),
    }))
    ElMessage.success('角色创建成功')
    createDialogVisible.value = false
    await loadRoles()
  } finally {
    creatingRole.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadPermissions()])
  if (roles.value.length > 0) {
    selectedRoleId.value = roles.value[0].id
  }
})
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
