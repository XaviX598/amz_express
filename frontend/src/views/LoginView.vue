<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import FadeIn from '@/components/FadeIn.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const localError = ref('')

async function handleSubmit() {
  localError.value = ''
  
  if (!email.value || !password.value) {
    localError.value = 'Por favor completa todos los campos'
    return
  }

  const success = await authStore.login({
    email: email.value,
    password: password.value,
  })

  if (success) {
    const redirect = route.query.redirect as string
    if (redirect) {
      router.push(redirect)
      return
    }

    if (authStore.isSuperAdmin) {
      router.push('/admin/usuarios')
      return
    }

    router.push('/')
  } else {
    localError.value = authStore.error || 'Error al iniciar sesión'
  }
}
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] px-4 sm:px-6 lg:px-8 relative overflow-hidden flex items-center justify-center">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute -top-20 left-1/2 -translate-x-1/2 w-[900px] h-[420px] bg-teal-500/10 blur-3xl pointer-events-none" />

    <FadeIn direction="up" :duration="600">
      <div class="relative max-w-5xl mx-auto w-full">
        <div class="grid lg:grid-cols-2 gap-6 lg:gap-8 items-stretch">
          <div class="hidden lg:flex flex-col justify-between rounded-3xl border border-white/10 bg-zinc-900/70 p-8">
            <div>
              <p class="text-sm text-teal-400 font-semibold uppercase tracking-wide">Acceso seguro</p>
              <h2 class="text-4xl font-bold text-white mt-3 leading-tight">Gestiona tus pedidos sin complicaciones</h2>
              <p class="text-zinc-400 mt-4">Inicia sesión para revisar órdenes, tracking y estimaciones en un solo panel.</p>
            </div>

            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-950/70 p-4 mt-8">
              <p class="text-zinc-300 text-sm mb-3">
                Si necesitas contratar este tipo de sistemas para tu empresa, contáctanos.
              </p>
              <a
                href="https://wa.me/593985295277?text=Hola,%20quiero%20informacion%20sobre%20un%20sistema%20como%20este%20para%20mi%20empresa."
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center justify-center w-full rounded-xl bg-green-600 hover:bg-green-700 text-white text-sm font-semibold py-2.5 transition-colors"
              >
                Contactar por WhatsApp
              </a>
            </div>
          </div>

          <div class="glass rounded-3xl p-6 sm:p-8 border border-white/10 shadow-2xl shadow-black/20">
            <div class="text-center mb-7">
              <div class="size-14 bg-gradient-to-br from-teal-500 to-teal-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-teal-500/30">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </div>
              <h1 class="text-3xl font-bold text-white">Bienvenido de vuelta</h1>
              <p class="text-zinc-400 mt-2">Ingresa tus credenciales para continuar</p>
            </div>

            <div v-if="localError" class="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-300 text-sm">
              {{ localError }}
            </div>

            <form @submit.prevent="handleSubmit" class="space-y-5">
              <div>
                <label class="block text-sm font-medium text-zinc-300 mb-2">Email</label>
                <input
                  v-model="email"
                  type="email"
                  autocomplete="email"
                  placeholder="tu@email.com"
                  class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                />
              </div>

              <div>
                <label class="block text-sm font-medium text-zinc-300 mb-2">Contraseña</label>
                <input
                  v-model="password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="••••••••"
                  class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                />
                <div class="mt-2 text-right">
                  <RouterLink to="/forgot-password" class="text-sm text-teal-400 hover:text-teal-300 transition-colors">
                    ¿Olvidaste tu contraseña?
                  </RouterLink>
                </div>
              </div>

              <button
                type="submit"
                :disabled="authStore.loading"
                class="w-full btn-primary py-3.5 text-base disabled:opacity-60 disabled:cursor-not-allowed"
              >
                <span v-if="authStore.loading">Iniciando sesión...</span>
                <span v-else>Iniciar sesión</span>
              </button>
            </form>

            <div class="mt-6 text-center">
              <p class="text-zinc-400">
                ¿No tienes cuenta?
                <RouterLink to="/register" class="text-teal-400 hover:text-teal-300 font-medium transition-colors">
                  Regístrate
                </RouterLink>
              </p>
            </div>
          </div>
        </div>
      </div>
    </FadeIn>
  </div>
</template>


