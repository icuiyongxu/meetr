<template>
  <div>
    <div class="meetr-page-title">用户管理</div>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" :type="r === 'ADMIN' ? 'danger' : 'primary'" size="small" style="margin-right: 4px">
              {{ r === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              v-if="!row.roles.includes('ADMIN')"
              type="danger"
              size="small"
              link
              @click="assignAdmin(row)"
            >设为管理员</el-button>
            <el-button
              v-else
              type="info"
              size="small"
              link
              @click="revokeAdmin(row)"
            >撤销管理员</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { unwrap, http } from '@/api'
import { useBookingStore } from '@/stores/booking'

interface UserItem {
  id: number
  userId: string
  name: string
  status: string
  roles: string[]
}

const users = ref<UserItem[]>([])
const loading = ref(false)
const store = useBookingStore()

async function load() {
  loading.value = true
  try {
    const res = await unwrap(http.get<any>('/admin/users'))
    users.value = res ?? []
  } finally {
    loading.value = false
  }
}

async function assignAdmin(row: UserItem) {
  await ElMessageBox.confirm(`确认将 ${row.userId} 设为管理员？`, '提示', { type: 'warning' })
  await unwrap(http.put(`/admin/users/${row.id}/roles`, { roleCodes: ['USER', 'ADMIN'] }))
  ElMessage.success('已设为管理员')
  await load()
}

async function revokeAdmin(row: UserItem) {
  await ElMessageBox.confirm(`确认撤销 ${row.userId} 的管理员权限？`, '提示', { type: 'warning' })
  await unwrap(http.put(`/admin/users/${row.id}/roles`, { roleCodes: ['USER'] }))
  ElMessage.success('已撤销')
  await load()
}

onMounted(load)
</script>
