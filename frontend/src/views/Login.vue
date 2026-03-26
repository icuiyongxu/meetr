<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">Meetr</div>
      <div class="subtitle">会议室预定系统</div>

      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="inputUserId"
            placeholder="请输入工号 / userId"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
          进入系统
        </el-button>
      </el-form>

      <div class="hint">
        首次输入将自动注册为普通用户<br />
        管理员请使用您的工号登录
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookingStore } from '@/stores/booking'

const router = useRouter()
const store = useBookingStore()
const inputUserId = ref('')
const loading = ref(false)

async function handleLogin() {
  const id = inputUserId.value.trim()
  if (!id) {
    ElMessage.warning('请输入工号')
    return
  }

  loading.value = true
  try {
    // 写入 localStorage 模拟登录
    localStorage.setItem('meetr_user_id', id)
    store.ensureUser()
    await store.login()
    ElMessage.success(`欢迎，${store.userName || id}`)
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
