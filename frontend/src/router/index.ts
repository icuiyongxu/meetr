import { createRouter, createWebHistory } from 'vue-router'

import CalendarView from '@/views/CalendarView.vue'
import BookingPage from '@/views/BookingPage.vue'
import MyBookings from '@/views/MyBookings.vue'
import RoomManage from '@/views/RoomManage.vue'
import ConfigManage from '@/views/ConfigManage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: CalendarView },
    { path: '/booking/:roomId', component: BookingPage },
    { path: '/my-bookings', component: MyBookings },
    { path: '/admin/rooms', component: RoomManage },
    { path: '/admin/config', component: ConfigManage },
  ],
})

export default router

