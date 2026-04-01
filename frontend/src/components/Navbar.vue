<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { navGradientHeight, navScrolled } from '@/composables/useNavScroll'

const authStore = useAuthStore()
const router = useRouter()

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.isAdmin)
const isSuperAdmin = computed(() => authStore.isSuperAdmin)
const userName = computed(() => authStore.user?.name || '')

const showOrdersLink = computed(() => {
  return isAuthenticated.value && !isAdmin.value
})

const isRegularUser = computed(() => {
  return isAuthenticated.value && !isAdmin.value && !isSuperAdmin.value
})

// WhatsApp link with pre-filled message
const whatsappLink = computed(() => {
  const phone = '593985295277'
  const message = encodeURIComponent('Buen día, quisiera que me ayude con información sobre...')
  return `https://wa.me/${phone}?text=${message}`
})

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <nav class="fixed top-0 left-0 right-0 z-50">
    <!-- Solid navbar when scrolled past hero -->
    <div 
      v-if="navScrolled"
      class="nav-solid"
    />
    <!-- Gradient overlay when at top (integrated with hero) -->
    <div 
      v-else
      class="nav-gradient"
      :style="{ height: `${navGradientHeight.value}px` }"
    />
    <!-- Main Navbar -->
    <div class="relative">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo - SuperAdmin goes to calculator, others go to home -->
          <RouterLink :to="isSuperAdmin ? '/admin/calculadora' : '/'" class="flex items-center gap-3 group">
            <div :class="['w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 backdrop-blur',
                         navScrolled ? 'bg-zinc-700' : 'bg-teal-500/50']">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                      d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
          </RouterLink>

          <!-- Desktop Navigation -->
          <div class="hidden lg:flex items-center gap-1">
            <template v-if="!isSuperAdmin">
              <RouterLink 
                to="/" 
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Inicio
              </RouterLink>

              <RouterLink 
                to="/calculadora" 
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Cotiza tus compras
              </RouterLink>

              <RouterLink 
                v-if="showOrdersLink" 
                to="/ordenes" 
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Mis Pedidos
              </RouterLink>

              <!-- WhatsApp contact for regular users -->
              <a 
                v-if="isRegularUser"
                :href="whatsappLink"
                target="_blank"
                rel="noopener noreferrer"
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150 flex items-center gap-2',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
                Contactanos
              </a>

              <RouterLink 
                v-if="isAdmin" 
                to="/admin/ordenes" 
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Gestionar Pedidos
              </RouterLink>

              <RouterLink 
                v-if="isSuperAdmin" 
                to="/admin/calculadora" 
                :class="['px-4 py-2 font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Cotización SuperAdmin
              </RouterLink>
            </template>
          </div>

          <!-- Auth Section -->
          <div class="flex items-center gap-4">
            <template v-if="isAuthenticated">
              <div class="hidden sm:flex items-center gap-2">
                <div :class="['w-8 h-8 rounded-full backdrop-blur flex items-center justify-center text-sm font-bold transition-all duration-300',
                             navScrolled ? 'bg-zinc-700 text-white' : 'bg-white/20 text-white']">
                  {{ userName.charAt(0).toUpperCase() }}
                </div>
                <span :class="['font-display text-base tracking-wider transition-colors duration-150',
                               navScrolled ? 'text-black font-bold' : 'text-white']">{{ userName }}</span>
              </div>
              <button 
                @click="handleLogout"
                :class="['font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Cerrar Sesión
              </button>
            </template>
            
            <template v-else>
              <RouterLink 
                to="/login"
                :class="['hidden sm:block font-display text-base tracking-wider transition-colors duration-150',
                         navScrolled ? 'text-black font-bold hover:text-white' : 'text-white hover:text-teal-400']"
              >
                Iniciar Sesión
              </RouterLink>
              <RouterLink 
                to="/register"
                :class="['px-5 py-2 rounded-lg font-display text-base tracking-wider transition-all duration-150 font-bold',
                         navScrolled ? 'bg-zinc-800 hover:bg-white text-white hover:text-zinc-800' : 'bg-white hover:bg-zinc-900 text-teal-600 hover:text-white']"
              >
                Registrarse
              </RouterLink>
            </template>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>
/* Gradient that descends from top — integrated with hero */
.nav-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.85) 0%,
    rgba(0, 0, 0, 0.6) 40%,
    rgba(0, 0, 0, 0) 100%
  );
  transition: height 0.05s linear;
  pointer-events: none;
}

/* Solid navbar — teal background with dark bar when scrolled */
.nav-solid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: #14b8a6;
  border-top: 3px solid #0a0a0a;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  animation: nav-appear 0.2s ease-out;
}

@keyframes nav-appear {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

