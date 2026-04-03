<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { authService } from '@/services/auth'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'

const route = useRoute()
const router = useRouter()

const loadingValidation = ref(true)
const tokenValid = ref(false)
const validationMessage = ref('')
const password = ref('')
const confirmPassword = ref('')
const changing = ref(false)
const error = ref('')
const success = ref('')

const token = computed(() => String(route.query.token || ''))

async function validateToken() {
  loadingValidation.value = true
  error.value = ''

  if (!token.value) {
    tokenValid.value = false
    validationMessage.value = 'El enlace no es valido.'
    loadingValidation.value = false
    return
  }

  try {
    const response = await authService.validateResetToken(token.value)
    tokenValid.value = response.valid
    validationMessage.value = response.message
  } catch (err: any) {
    tokenValid.value = false
    validationMessage.value = err?.message || 'No se pudo validar el enlace.'
  } finally {
    loadingValidation.value = false
  }
}

async function submitResetPassword() {
  error.value = ''
  success.value = ''

  if (password.value.length < 6) {
    error.value = 'La contrasena debe tener al menos 6 caracteres.'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Las contrasenas no coinciden.'
    return
  }

  changing.value = true
  try {
    const response = await authService.resetPassword({
      token: token.value,
      newPassword: password.value,
      confirmPassword: confirmPassword.value,
    })
    success.value = response.message
    setTimeout(() => {
      router.push('/login')
    }, 1800)
  } catch (err: any) {
    error.value = err?.message || 'No se pudo cambiar la contrasena.'
  } finally {
    changing.value = false
  }
}

onMounted(() => {
  void validateToken()
})
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] px-4 sm:px-6 lg:px-8 relative overflow-hidden flex items-center justify-center">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute -top-20 left-1/2 -translate-x-1/2 w-[900px] h-[420px] bg-teal-500/10 blur-3xl pointer-events-none" />

    <FadeIn direction="up" :duration="500">
      <div class="glass rounded-3xl p-6 sm:p-8 border border-white/10 shadow-2xl shadow-black/20 w-full max-w-lg">
        <h1 class="text-3xl font-bold text-white text-center">Cambiar contrasena</h1>

        <div v-if="loadingValidation" class="mt-6 text-center text-zinc-400">
          Validando enlace...
        </div>

        <template v-else-if="!tokenValid">
          <div class="mt-6 p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl text-amber-300 text-sm text-center">
            {{ validationMessage || 'El enlace expiro o no es valido.' }}
          </div>
          <div class="mt-6 text-center">
            <RouterLink to="/forgot-password" class="text-teal-400 hover:text-teal-300 font-medium transition-colors">
              Solicitar un nuevo enlace
            </RouterLink>
          </div>
        </template>

        <template v-else>
          <p class="text-zinc-400 mt-2 text-center">Ingresa tu nueva contrasena.</p>

          <div v-if="error" class="mt-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-300 text-sm">
            {{ error }}
          </div>
          <div v-if="success" class="mt-6 p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-300 text-sm">
            {{ success }}
          </div>

          <form @submit.prevent="submitResetPassword" class="mt-6 space-y-5">
            <div>
              <label class="block text-sm font-medium text-zinc-300 mb-2">Nueva contrasena</label>
              <input
                v-model="password"
                type="password"
                autocomplete="new-password"
                placeholder="Minimo 6 caracteres"
                class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-zinc-300 mb-2">Repite la contrasena</label>
              <input
                v-model="confirmPassword"
                type="password"
                autocomplete="new-password"
                placeholder="Repite la contrasena"
                class="w-full bg-zinc-800/80 border border-zinc-700/60 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
              />
            </div>

            <MotionButton
              :label="changing ? 'Guardando...' : 'Actualizar contrasena'"
              :disabled="changing"
              variant="primary"
              size="lg"
              block
            />
          </form>
        </template>
      </div>
    </FadeIn>
  </div>
</template>
