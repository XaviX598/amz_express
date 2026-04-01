<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authService } from '@/services/auth'
import FadeIn from '@/components/FadeIn.vue'

const router = useRouter()
const authStore = useAuthStore()

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const localError = ref('')

// Verification state
const showVerification = ref(false)
const verificationEmail = ref('')
const verificationCode = ref('')
const verifying = ref(false)
const verificationError = ref('')
const resending = ref(false)
const resendCooldown = ref(0)
let resendTimer: ReturnType<typeof setInterval> | null = null

const resendLabel = computed(() => {
  if (resendCooldown.value <= 0) return '¿No recibiste el código? Reenviar'
  return `Reenviar en ${resendCooldown.value}s`
})
function startResendCooldown(seconds = 60) {
  resendCooldown.value = seconds
  if (resendTimer) clearInterval(resendTimer)
  resendTimer = setInterval(() => {
    if (resendCooldown.value <= 1) {
      resendCooldown.value = 0
      if (resendTimer) {
        clearInterval(resendTimer)
        resendTimer = null
      }
      return
    }
    resendCooldown.value -= 1
  }, 1000)
}

async function handleSubmit() {
  localError.value = ''
  
  if (!name.value || !email.value || !password.value) {
    localError.value = 'Por favor completa todos los campos'
    return
  }

  if (password.value !== confirmPassword.value) {
    localError.value = 'Las contraseñas no coinciden'
    return
  }

  if (password.value.length < 6) {
    localError.value = 'La contraseña debe tener al menos 6 caracteres'
    return
  }

  try {
    const response = await authService.register({
      name: name.value,
      email: email.value,
      password: password.value,
    })

    if (response.requiresVerification) {
      // Show verification modal
      verificationEmail.value = response.email
      showVerification.value = true
      startResendCooldown(60)
    } else {
      // Should not happen but handle it
      router.push('/ordenes')
    }
  } catch (err: any) {
    localError.value = err.message || 'Error al registrarse'
  }
}

async function handleVerify() {
  verificationError.value = ''
  
  if (verificationCode.value.length !== 6) {
    verificationError.value = 'El código debe tener 6 dígitos'
    return
  }

  verifying.value = true

  try {
    const response = await authService.verifyCode(verificationEmail.value, verificationCode.value)
    
    // Save auth data
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify({
      id: response.userId,
      name: response.name,
      email: response.email,
      role: response.role,
    }))
    
    // Update store
    authStore.user = { id: response.userId, name: response.name, email: response.email, role: response.role as any }
    authStore.token = response.token
    
    router.push('/')
  } catch (err: any) {
    verificationError.value = err.message || 'Código inválido'
  } finally {
    verifying.value = false
  }
}

async function handleResend() {
  if (resending.value || resendCooldown.value > 0) return

  verificationError.value = ''
  resending.value = true
  try {
    await authService.resendCode(verificationEmail.value)
    startResendCooldown(60)
  } catch (err: any) {
    verificationError.value = err.message || 'Error al reenviar código'
  } finally {
    resending.value = false
  }
}

onUnmounted(() => {
  if (resendTimer) {
    clearInterval(resendTimer)
    resendTimer = null
  }
})
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
              <p class="text-sm text-teal-400 font-semibold uppercase tracking-wide">Nueva cuenta</p>
              <h2 class="text-4xl font-bold text-white mt-3 leading-tight">Crea tu acceso y empieza a importar con control</h2>
              <p class="text-zinc-400 mt-4">Calcula costos, revisa estados y administra tus pedidos desde una sola cuenta.</p>
            </div>

            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-950/70 p-4 mt-8">
              <p class="text-zinc-500 text-sm mb-2">Recomendaciones</p>
              <ul class="text-sm text-zinc-300 space-y-1">
                <li>Usa un email activo para recibir actualizaciones.</li>
                <li>La contraseña debe tener al menos 6 caracteres.</li>
                <li>Podrás cotizar y crear pedidos inmediatamente.</li>
              </ul>
            </div>
          </div>

          <div class="glass rounded-3xl p-6 sm:p-8 border border-white/10 shadow-2xl shadow-black/20">
            <div class="text-center mb-7">
              <div class="size-14 bg-gradient-to-br from-teal-500 to-teal-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-teal-500/30">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>
              </div>
              <h1 class="text-3xl font-bold text-white">Crear cuenta</h1>
              <p class="text-zinc-400 mt-2">Completa tus datos para registrarte</p>
            </div>

            <div v-if="localError" class="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-300 text-sm">
              {{ localError }}
            </div>

            <form @submit.prevent="handleSubmit" class="space-y-5">
              <div>
                <label class="block text-sm font-medium text-zinc-300 mb-2">Nombre completo</label>
                <input
                  v-model="name"
                  type="text"
                  autocomplete="name"
                  placeholder="Juan Pérez"
                  class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                />
              </div>

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

              <div class="grid sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-zinc-300 mb-2">Contraseña</label>
                  <input
                    v-model="password"
                    type="password"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                  />
                </div>

                <div>
                  <label class="block text-sm font-medium text-zinc-300 mb-2">Confirmar contraseña</label>
                  <input
                    v-model="confirmPassword"
                    type="password"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                  />
                </div>
              </div>

              <button
                type="submit"
                :disabled="authStore.loading"
                class="w-full btn-primary py-3.5 text-base disabled:opacity-60 disabled:cursor-not-allowed"
              >
                <span v-if="authStore.loading">Creando cuenta...</span>
                <span v-else>Crear cuenta</span>
              </button>
            </form>

            <div class="mt-6 text-center">
              <p class="text-zinc-400">
                ¿Ya tienes cuenta?
                <RouterLink to="/login" class="text-teal-400 hover:text-teal-300 font-medium transition-colors">
                  Inicia sesión
                </RouterLink>
              </p>
            </div>
          </div>
        </div>
      </div>
    </FadeIn>

    <!-- Verification Modal -->
    <Teleport to="body">
      <div v-if="showVerification" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" />
        <FadeIn direction="scale" :duration="300">
          <div class="relative glass rounded-2xl p-6 sm:p-8 max-w-md w-full border border-white/10">
            <div class="text-center mb-6">
              <div class="w-14 h-14 bg-teal-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-7 h-7 text-teal-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h3 class="text-xl font-bold text-white">Verifica tu correo</h3>
              <p class="text-zinc-400 text-sm mt-2">
                Te enviamos un código de 6 dígitos a<br />
                <span class="text-teal-400">{{ verificationEmail }}</span>
              </p>
            </div>

            <div v-if="verificationError" class="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-xl text-red-300 text-sm text-center">
              {{ verificationError }}
            </div>

            <form @submit.prevent="handleVerify" class="space-y-4">
              <div>
                <input
                  v-model="verificationCode"
                  type="text"
                  maxlength="6"
                  placeholder="000000"
                  class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-4 px-4 text-center text-2xl font-mono tracking-[12px] text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                  autocomplete="one-time-code"
                />
              </div>

              <button
                type="submit"
                :disabled="verifying || verificationCode.length !== 6"
                class="w-full btn-primary py-3.5 text-base disabled:opacity-60 disabled:cursor-not-allowed"
              >
                <span v-if="verifying">Verificando...</span>
                <span v-else>Verificar</span>
              </button>
            </form>

            <div class="mt-4 text-center">
              <button
                @click="handleResend"
                :disabled="resending || resendCooldown > 0"
                class="text-sm text-zinc-400 hover:text-teal-400 transition-colors disabled:opacity-50"
              >
                <span v-if="resending">Enviando...</span>
                <span v-else>{{ resendLabel }}</span>
              </button>
            </div>
          </div>
        </FadeIn>
      </div>
    </Teleport>
  </div>
</template>




