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
          <NotificationBell style="margin-right: 12px" />
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-avatar-wrap">
              <el-avatar :size="32" :style="{ background: '#409eff', fontSize: '13px', cursor: 'pointer' }">
                {{ store.userName ? store.userName.slice(0, 1).toUpperCase() : store.userId.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ store.userName || store.userId }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled style="cursor: default; opacity: 1">
                  <el-icon><User /></el-icon>
                  {{ store.userName || store.userId }}
                </el-dropdown-item>
                <el-dropdown-item v-if="store.isAdmin">
                  <el-tag type="danger" size="small" effect="plain">管理员</el-tag>
                </el-dropdown-item>
                <el-dropdown-item command="editProfile">
                  <el-icon><Edit /></el-icon>
                  编辑资料
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 编辑资料弹窗 -->
          <el-dialog v-model="profileVisible" title="编辑资料" width="420px" append-to-body destroy-on-close>
            <el-form v-if="profileForm" :model="profileForm" label-width="70px" size="default">
              <el-form-item label="账号">
                <el-input :model-value="profileForm.userId" disabled />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="profileForm.name" placeholder="请输入昵称" maxlength="50" show-word-limit />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="profileForm.password" type="password" placeholder="不修改请留空" show-password />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" placeholder="用于接收邮件通知" />
              </el-form-item>
              <el-form-item label="邮件通知">
                <el-switch v-model="profileForm.emailEnabled" active-text="开启" inactive-text="关闭" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="profileVisible = false">取消</el-button>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存</el-button>
            </template>
          </el-dialog>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import NotificationBell from '@/components/NotificationBell.vue'
import { ArrowDown, User, SwitchButton, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateProfile } from '@/api/user'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()
store.ensureUser()
onMounted(async () => {
    await store.login()
    if (!store.isLoggedIn) {
      router.push('/login')
    }
  })
function logout() {
  localStorage.removeItem('meetr_user_id')
  localStorage.removeItem('meetr_user_name')
  store.userId = ''
  store.userName = ''
  store.isAdmin = false
  store.isLoggedIn = false
  router.push('/login')
}

const profileVisible = ref(false)
const profileSaving = ref(false)
const profileForm = ref<{ userId: string; name: string; password: string; email: string; emailEnabled: boolean } | null>(null)

async function loadProfile() {
  try {
    const data = await getUserProfile(store.userId)
    profileForm.value = {
      userId: data.userId,
      name: data.name || '',
      password: '',
      email: data.email || '',
      emailEnabled: data.emailEnabled ?? false,
    }
    profileVisible.value = true
  } catch {
    ElMessage.error('加载用户信息失败')
  }
}

async function saveProfile() {
  if (!profileForm.value) return
  profileSaving.value = true
  try {
    await updateProfile({
      userId: profileForm.value.userId,
      name: profileForm.value.name || undefined,
      password: profileForm.value.password || undefined,
      email: profileForm.value.email || undefined,
      emailEnabled: profileForm.value.emailEnabled,
    })
    store.userName = profileForm.value.name
    ElMessage.success('保存成功')
    profileVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    profileSaving.value = false
  }
}

function handleUserCommand(cmd: string) {
  if (cmd === 'logout') {
    logout()
  } else if (cmd === 'editProfile') {
    loadProfile()
  }
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

.user-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background #f5f5f5;
}
.user-avatar-wrap:hover {
  background: #f5f5f5;
}
.user-name {
  font-size: 14px;
  color: #333;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meetr-page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
}
</style>
