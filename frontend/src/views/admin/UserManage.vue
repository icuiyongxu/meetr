<template>
  <div>
    <div class="meetr-page-title">用户管理</div>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">用户列表</span>
          <el-button type="primary" size="small" @click="openCreateDialog">新建用户</el-button>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="userId" label="工号" width="160" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="220">
          <template #default="{ row }">
            <el-tag
              v-for="r in row.roles"
              :key="r"
              :type="r === 'ADMIN' ? 'danger' : 'primary'"
              size="small"
              style="margin-right: 4px"
            >
              {{ roleName(r) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="邮件通知" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="!!row.emailEnabled"
              active-text=""
              inactive-text=""
              size="small"
              @change="toggleEmailEnabled(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
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
            <el-button type="info" size="small" link @click="openRoleDialog(row)">分配角色</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 / 编辑用户对话框 -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建用户' : '编辑用户'" width="420">
      <el-form :model="form" label-width="70" style="max-width: 360px">
        <el-form-item label="工号" v-if="formMode === 'create'">
          <el-input v-model="form.userId" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="用于接收邮件通知" />
        </el-form-item>
        <el-form-item label="邮件通知">
          <el-switch v-model="form.emailEnabled" />
        </el-form-item>
        <el-form-item :label="formMode === 'create' ? '密码' : '新密码'">
          <el-input
            v-model="form.password"
            :placeholder="formMode === 'create' ? '必填' : '留空则不修改'"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSaving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="320">
      <el-checkbox-group v-model="selectedRoleCodes">
        <el-checkbox value="ADMIN" border style="margin-bottom: 8px; width: 100%; text-align: left">
          管理员
        </el-checkbox>
        <el-checkbox value="USER" border style="width: 100%; text-align: left">
          普通用户
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, setUserStatus, deleteUser, assignRoles, type UserItem } from '@/api/user'

const users = ref<UserItem[]>([])
const loading = ref(false)

// --- 新建/编辑表单 ---
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formSaving = ref(false)
const form = ref({ id: 0, userId: '', name: '', password: '', email: '', emailEnabled: false })

function openCreateDialog() {
  formMode.value = 'create'
  form.value = { id: 0, userId: '', name: '', password: '', email: '', emailEnabled: false }
  formVisible.value = true
}

function openEditDialog(row: UserItem) {
  formMode.value = 'edit'
  form.value = { id: row.id, userId: row.userId, name: row.name, password: '', email: row.email || '', emailEnabled: !!row.emailEnabled }
  formVisible.value = true
}

async function submitForm() {
  if (formMode.value === 'create') {
    if (!form.value.userId.trim() || !form.value.password.trim()) {
      ElMessage.warning('工号和密码不能为空')
      return
    }
    formSaving.value = true
    try {
      await createUser({ userId: form.value.userId.trim(), name: form.value.name.trim(), password: form.value.password })
      ElMessage.success('创建成功')
      formVisible.value = false
      await load()
    } finally {
      formSaving.value = false
    }
  } else {
    if (!form.value.name.trim()) {
      ElMessage.warning('名称不能为空')
      return
    }
    formSaving.value = true
    try {
      await updateUser(form.value.id, {
        name: form.value.name.trim() || undefined,
        password: form.value.password || undefined,
        email: form.value.email || undefined,
        emailEnabled: form.value.emailEnabled,
      })
      ElMessage.success('保存成功')
      formVisible.value = false
      await load()
    } finally {
      formSaving.value = false
    }
  }
}

// --- 启用/停用 ---
async function toggleStatus(row: UserItem, status: string) {
  const action = status === 'ACTIVE' ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}用户 ${row.userId}？`, '提示', { type: 'warning' })
  await setUserStatus(row.id, status)
  ElMessage.success(`${action}成功`)
  await load()
}

// --- 删除 ---
async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户 ${row.userId}？此操作不可恢复！`, '警告', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await load()
}

// --- 分配角色 ---
const roleDialogVisible = ref(false)
const roleSaving = ref(false)
const roleTarget = ref<UserItem | null>(null)
const selectedRoleCodes = ref<string[]>([])

function openRoleDialog(row: UserItem) {
  roleTarget.value = row
  selectedRoleCodes.value = [...row.roles]
  roleDialogVisible.value = true
}

async function submitRoles() {
  if (!roleTarget.value) return
  roleSaving.value = true
  try {
    await assignRoles(roleTarget.value.id, selectedRoleCodes.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    await load()
  } finally {
    roleSaving.value = false
  }
}

async function toggleEmailEnabled(row: UserItem, enabled: boolean) {
  try {
    await updateUser(row.id, { emailEnabled: enabled })
    ElMessage.success(enabled ? '邮件通知已开启' : '邮件通知已关闭')
    await load()
  } catch {
    ElMessage.error('修改失败')
  }
}

function roleName(code: string) {
  return code === 'ADMIN' ? '管理员' : '普通用户'
}

async function load() {
  loading.value = true
  try {
    users.value = await getUsers()
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
