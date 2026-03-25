import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/style.css'
import App from '@/App.vue'
import router from '@/router'
import { useBookingStore } from '@/stores/booking'
import utc from 'dayjs/plugin/utc'
import timezone from 'dayjs/plugin/timezone'
import dayjs from 'dayjs'

dayjs.extend(utc)
dayjs.extend(timezone)

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// Initialize pseudo user for now (no auth).
const bookingStore = useBookingStore()
bookingStore.ensureUser()

app.mount('#app')

