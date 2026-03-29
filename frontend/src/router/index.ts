import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/', component: () => import('@/views/RootRedirect.vue') },
  { path: '/dashboard', component: () => import('@/views/CalendarView.vue') },
  { path: '/my-bookings', component: () => import('@/views/MyBookings.vue') },
  { path: '/admin/config', component: () => import('@/views/ConfigManage.vue') },
  { path: '/admin/buildings', component: () => import('@/views/BuildingManage.vue') },
  { path: '/admin/rooms', component: () => import('@/views/RoomManage.vue') },
  { path: '/admin/pending-bookings', component: () => import('@/views/admin/PendingBookings.vue') },
  { path: '/admin/users', component: () => import('@/views/admin/UserManage.vue') },
  { path: '/admin/roles', component: () => import('@/views/admin/RoleManage.vue') },
  { path: '/admin/equipments', component: () => import('@/views/admin/EquipmentManage.vue') },
  { path: '/admin/report/room-usage', component: () => import('@/views/admin/report/ReportRoomUsage.vue') },
  { path: '/admin/report/records', component: () => import('@/views/admin/report/ReportBookingRecords.vue') },
  { path: '/admin/report/user-usage', component: () => import('@/views/admin/report/ReportUserUsage.vue') },
  { path: '/admin/booking-search', component: () => import('@/views/admin/BookingSearch.vue') },
  { path: '/kiosk/room/:roomId', component: () => import('@/views/KioskView.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
