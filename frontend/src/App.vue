<script setup lang="ts">
import { RouterView } from 'vue-router'
import Navbar from './components/Navbar.vue'
import AppNotifications from './components/AppNotifications.vue'
import { useAuthStore } from './stores/auth'
import { useNavScroll } from './composables/useNavScroll'
import { onMounted, onUnmounted } from 'vue'

const authStore = useAuthStore()
const { init, destroy } = useNavScroll()

onMounted(() => {
  authStore.checkAuth()
  init()
})

onUnmounted(() => {
  destroy()
})
</script>

<template>
  <div class="app-shell min-h-screen">
    <!-- Progress Bar -->
    <div class="progress-container">
      <div class="progress-bar" />
    </div>
    
    <Navbar />
    <RouterView v-slot="{ Component, route }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
    <AppNotifications />
  </div>
</template>

<style>
.app-shell {
  background:
    radial-gradient(ellipse 64% 40% at 18% -6%, rgba(229, 174, 169, 0.2) 0%, transparent 58%),
    radial-gradient(ellipse 54% 34% at 88% 16%, rgba(166, 169, 208, 0.22) 0%, transparent 56%),
    linear-gradient(180deg, #f5f7f7 0%, #edf3f2 100%);
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.route-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.route-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (prefers-reduced-motion: reduce) {
  .route-fade-enter-active,
  .route-fade-leave-active {
    transition: none;
  }
}
</style>
