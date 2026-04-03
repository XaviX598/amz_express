<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { useAuthStore } from '@/stores/auth'
import { navGradientHeight, navScrolled } from '@/composables/useNavScroll'
import MotionButton from '@/components/MotionButton.vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileMenuOpen = ref(false)
const mobilePanel = ref<HTMLElement | null>(null)
let mobileTimeline: gsap.core.Timeline | null = null

const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.isAdmin)
const isSuperAdmin = computed(() => authStore.isSuperAdmin)
const userName = computed(() => authStore.user?.name || '')
const useDarkTopNavText = computed(() =>
  ['/login', '/register', '/forgot-password', '/reset-password'].includes(route.path)
)
const forceWhiteNavText = computed(() => route.path.startsWith('/ordenes'))
const shouldUseDarkNavText = computed(() =>
  !forceWhiteNavText.value && (navScrolled.value || useDarkTopNavText.value)
)

const showOrdersLink = computed(() => {
  return isAuthenticated.value && !isAdmin.value
})

const isRegularUser = computed(() => {
  return isAuthenticated.value && !isAdmin.value && !isSuperAdmin.value
})

const mainLinks = computed(() => {
  if (isSuperAdmin.value) {
    return [
      { to: '/admin/settings', label: 'Ajustes' },
    ]
  }

  const links = [
    { to: '/', label: 'Inicio' },
  ]

  if (!isAuthenticated.value) {
    links.push({ to: '/calculadora', label: 'Cotizador' })
  }

  if (showOrdersLink.value) {
    links.push({ to: '/ordenes', label: 'Mis pedidos' })
  }

  if (isAdmin.value) {
    links.push({ to: '/admin/ordenes', label: 'Gestion pedidos' })
  }

  return links
})

const primaryCta = computed(() => {
  if (isSuperAdmin.value) return { to: '/admin/usuarios', label: 'Ir al panel de usuarios' }
  if (isAuthenticated.value) return { to: '/calculadora', label: 'Nueva cotizacion' }
  return { to: '/register', label: 'Empieza ahora' }
})

// WhatsApp link with pre-filled message
const whatsappLink = computed(() => {
  const phone = '593985295277'
  const message = encodeURIComponent('Buen dia, quisiera ayuda para importar una compra desde Amazon.')
  return `https://wa.me/${phone}?text=${message}`
})

function isLinkActive(to: string) {
  if (to === '/') return route.path === '/'
  return route.path.startsWith(to)
}

function handleLogout() {
  authStore.logout()
  mobileMenuOpen.value = false
  router.push('/')
}

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  }
)

watch(mobileMenuOpen, async (open) => {
  document.body.style.overflow = open ? 'hidden' : ''

  if (open) {
    await nextTick()
    if (!mobilePanel.value) return

    mobileTimeline?.kill()
    mobileTimeline = gsap.timeline({ defaults: { ease: 'power2.out' } })
    mobileTimeline
      .fromTo(
        mobilePanel.value,
        { autoAlpha: 0, y: -12, scale: 0.98 },
        { autoAlpha: 1, y: 0, scale: 1, duration: 0.26 }
      )
      .fromTo(
        mobilePanel.value.querySelectorAll('.mobile-item'),
        { autoAlpha: 0, y: 12 },
        { autoAlpha: 1, y: 0, duration: 0.24, stagger: 0.04 },
        '-=0.12'
      )
  } else if (mobilePanel.value) {
    mobileTimeline?.kill()
    mobileTimeline = gsap.timeline({ defaults: { ease: 'power2.inOut' } })
    mobileTimeline.to(mobilePanel.value, { autoAlpha: 0, y: -8, duration: 0.15 })
  }
})

onUnmounted(() => {
  mobileTimeline?.kill()
  document.body.style.overflow = ''
})
</script>

<template>
  <nav class="fixed top-0 left-0 right-0 z-50">
    <div v-if="navScrolled" :class="forceWhiteNavText ? 'nav-solid-dark' : 'nav-solid'" />
    <div
      v-else
      :class="forceWhiteNavText ? 'nav-gradient-dark' : 'nav-gradient'"
      :style="{ height: `${navGradientHeight}px` }"
    />

    <div class="relative border-b" :class="shouldUseDarkNavText ? 'border-[#cfd9db]' : 'border-white/20'">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="h-16 flex items-center justify-between gap-4">
          <RouterLink :to="isSuperAdmin ? '/admin/calculadora' : '/'" class="flex items-center gap-3 group">
            <div :class="['w-10 h-10 rounded-xl flex items-center justify-center transition-all duration-300 backdrop-blur', navScrolled ? 'bg-[#1a4a61] text-white' : 'bg-[#35627a] text-white shadow-lg shadow-[#35627a]/20']">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <div class="hidden sm:block leading-tight">
              <p :class="['font-display text-xl tracking-wider', shouldUseDarkNavText ? 'text-[#1a3f54]' : 'text-white']">AMZ Express</p>
              <p :class="['text-[11px] uppercase tracking-[0.16em]', shouldUseDarkNavText ? 'text-[#637782]' : 'text-zinc-200']">Importacion guiada</p>
            </div>
          </RouterLink>

          <div class="hidden lg:flex items-center gap-1">
            <RouterLink
              v-for="link in mainLinks"
              :key="link.to"
              :to="link.to"
              :class="[
                'px-3 py-2 rounded-lg font-display text-base tracking-wider transition-all duration-150',
                isLinkActive(link.to)
                  ? shouldUseDarkNavText
                    ? 'bg-[#1a4a61] text-white'
                    : 'bg-white/15 text-white'
                  : shouldUseDarkNavText
                    ? 'text-[#21495f] hover:bg-[#1a4a61] hover:text-white'
                    : 'text-white hover:text-[#ffd0cb]'
              ]"
            >
              {{ link.label }}
            </RouterLink>

            <a
              v-if="isRegularUser"
              :href="whatsappLink"
              target="_blank"
              rel="noopener noreferrer"
              :class="[
                'ml-2 px-3 py-2 rounded-lg font-display text-base tracking-wider transition-all duration-150',
                shouldUseDarkNavText ? 'text-[#21495f] hover:bg-[#1a4a61] hover:text-white' : 'text-white hover:text-[#ffd0cb]'
              ]"
            >
              Soporte WhatsApp
            </a>
          </div>

          <div class="hidden lg:flex items-center gap-3">
            <template v-if="isAuthenticated">
              <div class="hidden lg:flex items-center">
                <div :class="['w-8 h-8 rounded-full backdrop-blur flex items-center justify-center text-sm font-bold transition-all duration-300', navScrolled ? 'bg-[#1a4a61] text-white' : 'bg-[#35627a] text-white']">
                  {{ userName.charAt(0).toUpperCase() }}
                </div>
              </div>
              <button
                @click="handleLogout"
                :class="['font-display text-base tracking-wider transition-colors duration-150', shouldUseDarkNavText ? 'text-[#21495f] hover:text-[#102d3e]' : 'text-white hover:text-[#ffd0cb]']"
              >
                Cerrar sesion
              </button>
            </template>

            <template v-else>
              <RouterLink
                to="/login"
                :class="['font-display text-base tracking-wider transition-colors duration-150', shouldUseDarkNavText ? 'text-[#21495f] hover:text-[#102d3e]' : 'text-white hover:text-[#ffd0cb]']"
              >
                Ingresar
              </RouterLink>
            </template>

            <MotionButton :to="primaryCta.to" :label="primaryCta.label" variant="primary" size="md" />
          </div>

          <button
            type="button"
            class="lg:hidden inline-flex items-center justify-center w-10 h-10 rounded-lg border transition-colors"
            :class="shouldUseDarkNavText ? 'border-[#1a4a61]/50 text-[#1a4a61]' : 'border-white/40 text-white'"
            @click="toggleMobileMenu"
            :aria-expanded="mobileMenuOpen"
            aria-label="Abrir menu"
          >
            <svg v-if="!mobileMenuOpen" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <Transition name="mobile-menu">
      <div v-if="mobileMenuOpen" class="lg:hidden mobile-overlay" @click.self="mobileMenuOpen = false">
        <div ref="mobilePanel" class="mobile-panel">
          <div class="space-y-1">
            <RouterLink
              v-for="link in mainLinks"
              :key="`mobile-${link.to}`"
              :to="link.to"
              :class="[
                'mobile-item block px-3 py-2 rounded-lg font-display text-xl tracking-wider transition-colors',
                isLinkActive(link.to) ? 'bg-[#dce8ea] text-[#1a4a61]' : 'text-[#21495f] hover:text-[#b46258]'
              ]"
            >
              {{ link.label }}
            </RouterLink>
          </div>

          <a
            v-if="isRegularUser"
            :href="whatsappLink"
            target="_blank"
            rel="noopener noreferrer"
            class="mobile-item mt-4 block px-3 py-2 rounded-lg font-display text-xl tracking-wider text-[#21495f] hover:text-[#b46258]"
          >
            Soporte WhatsApp
          </a>

          <div class="mobile-item mt-6 pt-6 border-t border-[#cfd9db] space-y-3">
            <template v-if="isAuthenticated">
              <div class="mx-auto w-9 h-9 rounded-full bg-[#1a4a61] text-white flex items-center justify-center text-sm font-bold">
                {{ userName.charAt(0).toUpperCase() }}
              </div>
              <button @click="handleLogout" class="w-full py-2 rounded-lg bg-[#e5ecee] text-[#1a4a61] font-display tracking-wider">Cerrar sesion</button>
            </template>
            <template v-else>
              <RouterLink to="/login" class="block w-full text-center py-2 rounded-lg border border-[#c3d0d4] text-[#1a4a61] font-display tracking-wider">Ingresar</RouterLink>
            </template>
            <MotionButton :to="primaryCta.to" :label="primaryCta.label" variant="primary" size="md" block />
          </div>
        </div>
      </div>
    </Transition>
  </nav>
</template>

<style scoped>
.nav-gradient {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(
    to bottom,
    rgba(245, 247, 247, 0.95) 0%,
    rgba(245, 247, 247, 0.72) 46%,
    rgba(245, 247, 247, 0) 100%
  );
  transition: height 0.05s linear;
  pointer-events: none;
}

.nav-gradient-dark {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(
    to bottom,
    rgba(6, 18, 30, 0.95) 0%,
    rgba(9, 25, 39, 0.74) 46%,
    rgba(9, 25, 39, 0) 100%
  );
  transition: height 0.05s linear;
  pointer-events: none;
}

.nav-solid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: linear-gradient(90deg, #f5f7f7 0%, #e8eff1 58%, #e5eaef 100%);
  border-top: 2px solid #dce4e8;
  box-shadow: 0 8px 20px rgba(26, 74, 97, 0.12);
  animation: nav-appear 0.2s ease-out;
}

.nav-solid-dark {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: linear-gradient(90deg, rgba(6, 18, 30, 0.95) 0%, rgba(17, 41, 59, 0.94) 55%, rgba(22, 49, 67, 0.95) 100%);
  border-top: 2px solid rgba(153, 186, 206, 0.18);
  box-shadow: 0 8px 20px rgba(3, 10, 18, 0.38);
  animation: nav-appear 0.2s ease-out;
}

.mobile-overlay {
  position: fixed;
  inset: 64px 0 0;
  background: rgba(52, 76, 91, 0.16);
  backdrop-filter: blur(3px);
  padding: 1rem;
}

.mobile-panel {
  max-width: 36rem;
  margin: 0 auto;
  border-radius: 1rem;
  border: 1px solid rgba(53, 98, 122, 0.2);
  background: rgba(245, 247, 247, 0.95);
  padding: 1rem;
}

.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: opacity 180ms ease;
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
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
