<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'

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
  <div class="auth-page min-h-[calc(100dvh-64px)] pt-[72px] px-4 sm:px-6 lg:px-8 relative overflow-hidden flex items-center justify-center">
    <div class="auth-noise pointer-events-none absolute inset-0" />

    <FadeIn direction="up" :duration="560">
      <div class="relative auth-shell max-w-5xl mx-auto w-full">
        <div class="grid lg:grid-cols-[0.92fr,1.08fr] gap-0 rounded-3xl overflow-hidden border border-[#cad8dd] shadow-[0_30px_70px_rgba(26,74,97,0.16)]">
          <aside class="hidden lg:flex flex-col justify-between auth-side p-9">
            <div>
              <p class="auth-kicker">Acceso seguro</p>
              <h2 class="auth-title">Bienvenido de nuevo</h2>
              <p class="auth-copy">
                Gestioná tus pedidos, cotizaciones y seguimiento desde una sola consola con trazabilidad completa.
              </p>
            </div>

            <div class="auth-side-card">
              <p class="text-sm text-white mb-3">
                ¿Buscás una solución similar para tu empresa?
              </p>
              <MotionButton
                href="https://wa.me/593985295277?text=Hola,%20quiero%20informacion%20sobre%20un%20sistema%20como%20este%20para%20mi%20empresa."
                target="_blank"
                rel="noopener noreferrer"
                label="Contactar por WhatsApp"
                variant="primary"
                size="md"
                block
              />
            </div>
          </aside>

          <section class="auth-card p-6 sm:p-9">
            <div class="text-center mb-7">
              <div class="auth-icon size-14 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </div>
              <h1 class="text-3xl font-bold text-[#14384d]">Iniciar sesión</h1>
              <p class="text-[#62757f] mt-2">Ingresá tus credenciales para continuar</p>
            </div>

            <div v-if="localError" class="mb-6 p-4 auth-error text-sm">
              {{ localError }}
            </div>

            <form @submit.prevent="handleSubmit" class="space-y-5">
              <div>
                <label class="auth-label">Correo electrónico</label>
                <input
                  v-model="email"
                  type="email"
                  autocomplete="email"
                  placeholder="nombre@empresa.com"
                  class="auth-input"
                />
              </div>

              <div>
                <label class="auth-label">Contraseña</label>
                <input
                  v-model="password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="••••••••"
                  class="auth-input"
                />
                <div class="mt-2 text-right">
                  <RouterLink to="/forgot-password" class="text-sm text-[#35627A] hover:text-[#1a4a61] transition-colors">
                    ¿Olvidaste tu contraseña?
                  </RouterLink>
                </div>
              </div>

              <MotionButton
                type="submit"
                :label="authStore.loading ? 'Iniciando sesion...' : 'Iniciar sesion'"
                :disabled="authStore.loading"
                variant="primary"
                size="lg"
                block
              />
            </form>

            <div class="mt-6 text-center">
              <p class="text-[#62757f]">
                ¿No tienes cuenta?
                <RouterLink to="/register" class="text-[#1a4a61] hover:text-[#b46258] font-medium transition-colors">
                  Regístrate
                </RouterLink>
              </p>
            </div>
          </section>
        </div>
      </div>
    </FadeIn>
  </div>
</template>

<style scoped>
.auth-page {
  background:
    radial-gradient(circle at 16% 14%, rgba(166, 169, 208, 0.22), transparent 38%),
    radial-gradient(circle at 84% 16%, rgba(229, 174, 169, 0.2), transparent 42%),
    linear-gradient(180deg, #f5f7f7 0%, #edf3f2 100%);
}

.auth-noise {
  opacity: 0.25;
  background-image: radial-gradient(rgba(53, 98, 122, 0.12) 0.5px, transparent 0.5px);
  background-size: 3px 3px;
}

.auth-shell {
  backdrop-filter: blur(3px);
}

.auth-side {
  background: linear-gradient(160deg, #1a4a61 0%, #35627a 62%, #1f4e67 100%);
  color: #f5f5f5;
}

.auth-kicker {
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

.auth-title {
  margin-top: 1rem;
  font-size: 2rem;
  line-height: 1.04;
  letter-spacing: 0.02em;
  font-weight: 800;
}

.auth-copy {
  margin-top: 0.95rem;
  color: #d7e3e9;
  line-height: 1.6;
}

.auth-side-card {
  border: 1px solid rgba(245, 245, 245, 0.24);
  border-radius: 1rem;
  background: rgba(245, 245, 245, 0.12);
  padding: 1rem;
}

.auth-card {
  background: rgba(249, 251, 251, 0.92);
}

.auth-icon {
  background: linear-gradient(135deg, #1a4a61, #35627a);
  box-shadow: 0 12px 28px rgba(26, 74, 97, 0.24);
}

.auth-label {
  display: block;
  margin-bottom: 0.45rem;
  font-size: 0.86rem;
  font-weight: 600;
  color: #35627a;
}

.auth-input {
  width: 100%;
  border-radius: 0.78rem;
  border: 1px solid #d2dde0;
  background: #edf2f2;
  color: #14384d;
  padding: 0.88rem 0.9rem;
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

.auth-input::placeholder {
  color: #8b9ba2;
}

.auth-input:focus {
  border-color: #35627a;
  box-shadow: 0 0 0 3px rgba(53, 98, 122, 0.14);
  background: #f7faf9;
  outline: none;
}

.auth-error {
  border: 1px solid rgba(180, 98, 88, 0.35);
  border-radius: 0.8rem;
  background: rgba(180, 98, 88, 0.12);
  color: #8b2f24;
}

@media (max-width: 1024px) {
  .auth-card {
    border-radius: 1.2rem;
  }
}
</style>


