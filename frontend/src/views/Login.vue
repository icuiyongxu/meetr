<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">Meetr</div>
      <div class="subtitle">会议室预定系统</div>

      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.userId"
            placeholder="工号 / userId"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            placeholder="密码（首次登录可为空）"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
          登录
        </el-button>
      </el-form>

      <div class="hint">
        首次输入工号将自动注册<br />
        管理员账号：admin / 密码由初始化设置
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookingStore } from '@/stores/booking'

const router = useRouter()
const store = useBookingStore()
const loading = ref(false)

const form = reactive({
  userId: '',
  password: '',
})

async function handleLogin() {
  const userId = form.userId.trim()
  if (!userId) {
    ElMessage.warning('请输入工号')
    return
  }

  loading.value = true
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId, password: form.password || null }),
    })
    const json = await res.json()
    if (json.code !== 0) {
      ElMessage.error(json.message || '登录失败')
      return
    }
    const data = json.data
    localStorage.setItem('meetr_user_id', data.userId)
    localStorage.setItem('meetr_user_name', data.name || data.userId)
    store.ensureUser()
    store.isLoggedIn = true
    store.isAdmin = data.roles?.includes('ADMIN') ?? false
    if (data.name) store.setUserName(data.name)
    ElMessage.success(`欢迎，${data.name || data.userId}`)
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: white;
  border-radius: 12px;
  padding: 40px 36px;
  width: 360px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.logo {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-color-primary);
  text-align: center;
  letter-spacing: 2px;
}

.subtitle {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 28px;
  margin-top: 4px;
}

.hint {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #aaa;
  line-height: 1.6;
}
</style>
