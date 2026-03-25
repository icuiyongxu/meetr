<template>
  <el-config-provider :locale="zhCn">
    <el-container class="layout" :class="`layout-${menuLayout}`">

      <!-- 左侧边栏（可选） -->
      <el-aside v-if="menuLayout === 'side'" class="aside" width="200px">
        <div class="brand" @click="router.push('/')">
          <div class="logo">Meetr</div>
          <div class="sub">会议室预定</div>
        </div>
        <el-menu
          class="side-menu"
          :default-active="activePath"
          router
          :collapse="false"
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
      </el-aside>

      <el-container>
        <!-- 顶部栏 -->
        <el-header class="header">
          <div class="brand" :class="{ 'brand-top-only': menuLayout === 'side' }" @click="router.push('/')">
            <div class="logo">Meetr</div>
            <div class="sub">会议室预定</div>
          </div>

          <!-- 顶部菜单（可选） -->
          <el-menu
            v-if="menuLayout === 'top'"
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

          <!-- 空白填充（侧边栏模式下） -->
          <div v-else class="top-spacer" />

          <!-- 右侧用户区 -->
          <div class="user">
            <!-- 布局切换按钮 -->
            <el-tooltip :content="menuLayout === 'side' ? '切换到顶部菜单' : '切换到侧边菜单'" placement="bottom">
              <el-button
                :icon="menuLayout === 'side' ? 'Top' : 'Grid'"
                circle
                text
                @click="toggleLayout"
              />
            </el-tooltip>

            <el-tooltip placement="bottom" content="当前为无登录模式，userId 用于请求参数">
              <el-tag type="info" effect="plain">userId: {{ store.userId }}</el-tag>
            </el-tooltip>
            <el-input
              v-model="nameDraft"
              size="small"
              placeholder="你的名字（可选）"
              style="width: 140px"
              @blur="applyName"
              @keyup.enter="applyName"
            />
          </div>
        </el-header>

        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useBookingStore } from '@/stores/booking'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()

const activePath = computed(() => route.path)

// 菜单布局：'top' | 'side'，默认侧边栏
const menuLayout = ref<'top' | 'side'>(
  (localStorage.getItem('meetr_menu_layout') as 'top' | 'side') || 'side'
)

function toggleLayout() {
  menuLayout.value = menuLayout.value === 'side' ? 'top' : 'side'
  localStorage.setItem('meetr_menu_layout', menuLayout.value)
}

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

/* ── 侧边栏布局 ── */
.layout-side .aside {
  background: #fff;
  border-right: 1px solid var(--el-border-color-light);
  overflow-y: auto;
}

.layout-side .aside .brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.layout-side .aside .logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.layout-side .aside .sub {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.side-menu {
  border-right: 0;
  background: transparent;
}

/* ── 顶部栏 ── */
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
  padding: 0 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.brand-top-only {
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
  padding-right: 12px;
  border-right: 1px solid var(--el-border-color-lighter);
}

.logo {
  font-size: 18px;
  font-weight: 700;
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

.top-spacer {
  flex: 1;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.main {
  padding: 16px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
