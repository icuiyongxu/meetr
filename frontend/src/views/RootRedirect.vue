<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'

const router = useRouter()
const store = useBookingStore()

if (store.isLoggedIn) {
  router.replace('/dashboard')
} else {
  router.replace('/login')
}
</script>
