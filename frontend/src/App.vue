<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="router.push('/')">
        <div class="logo">Meetr</div>
        <div class="sub">Meeting Room Booking</div>
      </div>

      <el-menu
        class="top-menu"
        mode="horizontal"
        :default-active="activePath"
        router
        :ellipsis="false"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/my-bookings">我的预约</el-menu-item>
        <el-sub-menu index="/admin">
          <template #title>管理</template>
          <el-menu-item index="/admin/buildings">楼栋管理</el-menu-item>
          <el-menu-item index="/admin/rooms">会议室管理</el-menu-item>
          <el-menu-item index="/admin/config">规则配置</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="user">
        <el-tooltip placement="bottom" content="当前为无登录模式，userId 用于请求参数">
          <el-tag type="info" effect="plain">userId: {{ store.userId }}</el-tag>
        </el-tooltip>
        <el-input
          v-model="nameDraft"
          size="small"
          placeholder="你的名字（可选）"
          style="width: 160px"
          @blur="applyName"
          @keyup.enter="applyName"
        />
      </div>
    </el-header>

    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useBookingStore } from '@/stores/booking'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()

const activePath = computed(() => route.path)

const nameDraft = ref(store.userName || '')
watch(
  () => store.userName,
  (v) => {
    if (v && v !== nameDraft.value) nameDraft.value = v
  },
)

function applyName() {
  const v = nameDraft.value.trim()
  store.setUserName(v)
  if (v) ElMessage.success('已保存姓名')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
}

.brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  padding-right: 8px;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2px;
  color: var(--el-color-primary);
  line-height: 1.1;
}

.sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.1;
}

.top-menu {
  flex: 1;
  border-bottom: 0;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.main {
  padding: 16px;
  background: #f5f7fa;
}
</style>

