<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authService } from '@/services/auth'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'

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
const verificationMessage = ref('')
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
  if (seconds <= 0) {
    resendTimer = null
    return
  }
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
      verificationMessage.value = response.message || ''
      verificationCode.value = ''
      verificationError.value = ''
      showVerification.value = true
      startResendCooldown(response.codeSent ?? true ? 60 : 0)
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
  <div class="register-page min-h-[calc(100dvh-64px)] pt-[72px] px-4 sm:px-6 lg:px-8 relative overflow-hidden flex items-center justify-center">
    <div class="register-noise pointer-events-none absolute inset-0" />

    <FadeIn direction="up" :duration="560">
      <div class="relative max-w-5xl mx-auto w-full">
        <div class="grid lg:grid-cols-[0.9fr,1.1fr] gap-0 rounded-3xl overflow-hidden border border-[#cad8dd] shadow-[0_30px_70px_rgba(26,74,97,0.16)]">
          <aside class="hidden lg:flex flex-col justify-between register-side p-9">
            <div>
              <p class="register-kicker">Nueva cuenta</p>
              <h2 class="register-title">La precisión que tu negocio requiere</h2>
              <p class="register-copy">
                Organizá cada importación con control editorial de costos, peso, impuestos y tiempos.
              </p>
            </div>

            <div class="register-side-card">
              <p class="text-sm text-[#d8e4ea] mb-2">Recomendaciones</p>
              <ul class="text-sm text-[#ebf1f4] space-y-1">
                <li>Usá un correo activo para verificación.</li>
                <li>La contraseña debe tener mínimo 6 caracteres.</li>
                <li>Después del registro podés cotizar al instante.</li>
              </ul>
            </div>
          </aside>

          <section class="register-card p-6 sm:p-9">
            <div class="text-center mb-7">
              <div class="register-icon size-14 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                </svg>
              </div>
              <h1 class="text-3xl font-bold text-[#14384d]">Crear cuenta</h1>
              <p class="text-[#62757f] mt-2">Completá tus datos para registrarte</p>
            </div>

            <div v-if="localError" class="mb-6 p-4 register-error text-sm">
              {{ localError }}
            </div>

            <form @submit.prevent="handleSubmit" class="space-y-5">
              <div>
                <label class="register-label">Nombre completo</label>
                <input
                  v-model="name"
                  type="text"
                  autocomplete="name"
                  placeholder="Juan Perez"
                  class="register-input"
                />
              </div>

              <div>
                <label class="register-label">Correo electrónico</label>
                <input
                  v-model="email"
                  type="email"
                  autocomplete="email"
                  placeholder="nombre@empresa.com"
                  class="register-input"
                />
              </div>

              <div class="grid sm:grid-cols-2 gap-4">
                <div>
                  <label class="register-label">Contraseña</label>
                  <input
                    v-model="password"
                    type="password"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    class="register-input"
                  />
                </div>

                <div>
                  <label class="register-label">Confirmar contraseña</label>
                  <input
                    v-model="confirmPassword"
                    type="password"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    class="register-input"
                  />
                </div>
              </div>

              <MotionButton
                type="submit"
                :label="authStore.loading ? 'Creando cuenta...' : 'Crear cuenta'"
                :disabled="authStore.loading"
                variant="primary"
                size="lg"
                block
              />
            </form>

            <div class="mt-6 text-center">
              <p class="text-[#62757f]">
                ¿Ya tienes cuenta?
                <RouterLink to="/login" class="text-[#1a4a61] hover:text-[#b46258] font-medium transition-colors">
                  Inicia sesión
                </RouterLink>
              </p>
            </div>
          </section>
        </div>
      </div>
    </FadeIn>

    <!-- Verification Modal -->
    <Teleport to="body">
      <div v-if="showVerification" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-[rgba(12,24,33,0.58)] backdrop-blur-sm" />
        <FadeIn direction="scale" :duration="300">
          <div class="relative register-modal rounded-2xl p-6 sm:p-8 max-w-md w-full border border-[#cad8dd]">
            <div class="text-center mb-6">
              <div class="w-14 h-14 register-icon rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h3 class="text-xl font-bold text-[#14384d]">Verificá tu correo</h3>
              <p class="text-[#62757f] text-sm mt-2">
                Te enviamos un código de 6 dígitos a<br />
                <span class="text-[#35627A] font-semibold">{{ verificationEmail }}</span>
              </p>
              <p
                v-if="verificationMessage"
                class="mt-3 text-xs leading-relaxed text-[#4f6672]"
              >
                {{ verificationMessage }}
              </p>
            </div>

            <div v-if="verificationError" class="mb-4 p-3 register-error text-sm text-center">
              {{ verificationError }}
            </div>

            <form @submit.prevent="handleVerify" class="space-y-4">
              <div>
                <input
                  v-model="verificationCode"
                  type="text"
                  maxlength="6"
                  placeholder="000000"
                  class="register-input text-center text-2xl font-mono tracking-[10px]"
                  autocomplete="one-time-code"
                />
              </div>

              <MotionButton
                type="submit"
                :label="verifying ? 'Verificando...' : 'Verificar'"
                :disabled="verifying || verificationCode.length !== 6"
                variant="primary"
                size="lg"
                block
              />
            </form>

            <div class="mt-4 text-center">
              <button
                @click="handleResend"
                :disabled="resending || resendCooldown > 0"
                class="text-sm text-[#62757f] hover:text-[#1a4a61] transition-colors disabled:opacity-50"
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

<style scoped>
.register-page {
  background:
    radial-gradient(circle at 14% 18%, rgba(229, 174, 169, 0.24), transparent 36%),
    radial-gradient(circle at 86% 20%, rgba(166, 169, 208, 0.24), transparent 42%),
    linear-gradient(180deg, #f5f7f7 0%, #edf3f2 100%);
}

.register-noise {
  opacity: 0.26;
  background-image: radial-gradient(rgba(53, 98, 122, 0.12) 0.5px, transparent 0.5px);
  background-size: 3px 3px;
}

.register-side {
  background:
    linear-gradient(180deg, rgba(10, 34, 49, 0.2), rgba(10, 34, 49, 0.24)),
    linear-gradient(155deg, #173e53 0%, #245973 55%, #35627a 100%);
  color: #f5f5f5;
}

.register-kicker {
  display: inline-flex;
  align-items: center;
  padding: 0.38rem 0.78rem;
  border-radius: 9999px;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #d3e6f1;
  border: 1px solid rgba(245, 245, 245, 0.26);
  background: rgba(245, 245, 245, 0.08);
}

.register-title {
  margin-top: 1rem;
  font-size: 2rem;
  line-height: 1.06;
  letter-spacing: 0.02em;
  font-weight: 800;
}

.register-copy {
  margin-top: 0.95rem;
  color: #dce7ec;
  line-height: 1.6;
}

.register-side-card {
  border: 1px solid rgba(245, 245, 245, 0.24);
  border-radius: 1rem;
  background: rgba(245, 245, 245, 0.12);
  padding: 1rem;
}

.register-card {
  background: rgba(249, 251, 251, 0.94);
}

.register-modal {
  background: rgba(249, 251, 251, 0.98);
  box-shadow: 0 26px 60px rgba(12, 24, 33, 0.28);
}

.register-icon {
  background: linear-gradient(135deg, #1a4a61, #35627a);
  box-shadow: 0 12px 28px rgba(26, 74, 97, 0.24);
}

.register-label {
  display: block;
  margin-bottom: 0.45rem;
  font-size: 0.86rem;
  font-weight: 600;
  color: #35627a;
}

.register-input {
  width: 100%;
  border-radius: 0.78rem;
  border: 1px solid #d2dde0;
  background: #edf2f2;
  color: #14384d;
  padding: 0.88rem 0.9rem;
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.register-input::placeholder {
  color: #8b9ba2;
}

.register-input:focus {
  border-color: #35627a;
  box-shadow: 0 0 0 3px rgba(53, 98, 122, 0.14);
  background: #f7faf9;
  outline: none;
}

.register-error {
  border: 1px solid rgba(180, 98, 88, 0.35);
  border-radius: 0.8rem;
  background: rgba(180, 98, 88, 0.12);
  color: #8b2f24;
}

@media (max-width: 1024px) {
  .register-card {
    border-radius: 1.2rem;
  }
}
</style>




