<template>
  <!-- 登录页全屏 -->
  <router-view v-if="route.path === '/login'" />

  <!-- 主框架 -->
  <el-container v-else class="app-shell">
    <el-aside width="200px">
      <div class="logo-area">Meetr</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">
          <span>会议室日历</span>
        </el-menu-item>
        <el-menu-item index="/my-bookings">
          <span>我的预约</span>
        </el-menu-item>
        <el-sub-menu v-if="store.isAdmin" index="/admin">
          <template #title>管理</template>
          <el-menu-item index="/admin/buildings">楼栋管理</el-menu-item>
          <el-menu-item index="/admin/rooms">会议室管理</el-menu-item>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/roles">角色管理</el-menu-item>
          <el-menu-item index="/admin/equipments">设备管理</el-menu-item>
          <el-menu-item index="/admin/config">规则配置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">{{ currentPageTitle }}</div>
        <div class="header-right">
          <el-tag v-if="store.isAdmin" type="danger" effect="plain" style="margin-right: 6px">管理员</el-tag>
          <el-tooltip placement="bottom" content="当前为无登录模式，userId 用于请求参数">
            <el-tag type="info" effect="plain" style="margin-right: 8px">userId: {{ store.userId }}</el-tag>
          </el-tooltip>
          <el-button type="danger" plain size="small" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()
store.ensureUser()
onMounted(async () => {
  if (!store.userId) {
    router.push('/login')
    return
  }
  await store.login()
})

function logout() {
  localStorage.removeItem('meetr_user_id')
  localStorage.removeItem('meetr_user_name')
  store.userId = ''
  store.isAdmin = false
  store.isLoggedIn = false
  router.push('/login')
}

const titles: Record<string, string> = {
  '/dashboard': '会议室日历',
  '/my-bookings': '我的预约',
  '/admin/config': '规则配置',
  '/admin/users': '用户管理',
  '/admin/roles': '角色管理',
  '/admin/equipments': '设备管理',
  '/admin/buildings': '楼栋管理',
  '/admin/rooms': '会议室管理',
}
const currentPageTitle = computed(() => titles[route.path] ?? 'Meetr')
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}
</style>

<style scoped>
.login-shell {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.app-shell {
  height: 100vh;
}

.app-shell .el-aside {
  background: #f5f7fa;
  overflow-y: auto;
}

.app-shell .el-header {
  background: #fff;
  border-bottom: 1px solid #e4e8ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.app-shell .el-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}

.logo-area {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-primary);
  letter-spacing: 2px;
  border-bottom: 1px solid #e4e8ed;
}

.meetr-page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}
</style>
