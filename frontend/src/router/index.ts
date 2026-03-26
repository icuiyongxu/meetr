import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/', component: () => import('@/views/CalendarView.vue') },
  { path: '/my-bookings', component: () => import('@/views/MyBookings.vue') },
  { path: '/admin/config', component: () => import('@/views/ConfigManage.vue') },
  { path: '/admin/users', component: () => import('@/views/admin/UserManage.vue') },
  { path: '/admin/roles', component: () => import('@/views/admin/RoleManage.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
