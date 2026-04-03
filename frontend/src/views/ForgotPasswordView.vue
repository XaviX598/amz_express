<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { authService } from '@/services/auth'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'

const email = ref('')
const sending = ref(false)
const error = ref('')
const success = ref('')
const resendCooldown = ref(0)
const COOLDOWN_SECONDS = 60
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const isCooldownActive = computed(() => resendCooldown.value > 0)
const isSubmitDisabled = computed(() => sending.value || isCooldownActive.value)

function startCooldown() {
  resendCooldown.value = COOLDOWN_SECONDS

  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }

  cooldownTimer = setInterval(() => {
    if (resendCooldown.value <= 1) {
      resendCooldown.value = 0
      if (cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
      return
    }

    resendCooldown.value -= 1
  }, 1000)
}

async function submitForgotPassword() {
  error.value = ''
  success.value = ''

  if (!email.value) {
    error.value = 'Ingresa tu correo para continuar.'
    return
  }

  startCooldown()
  sending.value = true

  try {
    const response = await authService.forgotPassword(email.value)
    success.value = response.message
  } catch (err: any) {
    error.value = err?.message || 'No se pudo procesar la solicitud.'
  } finally {
    sending.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
})
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] px-4 sm:px-6 lg:px-8 relative overflow-hidden flex items-center justify-center">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute -top-20 left-1/2 -translate-x-1/2 w-[900px] h-[420px] bg-teal-500/10 blur-3xl pointer-events-none" />

    <FadeIn direction="up" :duration="500">
      <div class="glass rounded-3xl p-6 sm:p-8 border border-white/10 shadow-2xl shadow-black/20 w-full max-w-lg">
        <h1 class="text-3xl font-bold text-white text-center">Recuperar contraseña</h1>
        <p class="text-zinc-400 mt-2 text-center">Te enviaremos un enlace para cambiar tu contraseña.</p>

        <div v-if="error" class="mt-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-300 text-sm">
          {{ error }}
        </div>
        <div v-if="success" class="mt-6 p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-300 text-sm">
          {{ success }}
        </div>

        <form @submit.prevent="submitForgotPassword" class="mt-6 space-y-5">
          <div>
            <label class="block text-sm font-medium text-zinc-300 mb-2">Correo</label>
            <input
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="tu@email.com"
              class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
            />
          </div>

          <MotionButton
            :label="sending ? 'Enviando...' : isCooldownActive ? `Reenviar en ${resendCooldown}s` : 'Enviar enlace'"
            :disabled="isSubmitDisabled"
            variant="primary"
            size="lg"
            block
          />
        </form>

        <div class="mt-6 text-center">
          <RouterLink to="/login" class="text-teal-400 hover:text-teal-300 font-medium transition-colors">
            Volver a iniciar sesión
          </RouterLink>
        </div>
      </div>
    </FadeIn>
  </div>
</template>
