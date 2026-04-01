<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { settingsService } from '@/services/settings'
import type { CalculatorSettings, UpdateSettingsRequest } from '@/types'
import FadeIn from '@/components/FadeIn.vue'

const authStore = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const success = ref<string | null>(null)
const error = ref<string | null>(null)

const BASE_SETTINGS: CalculatorSettings = {
  TAX_RATE: '',
  HANDLING_RATE: '',
  SHIPPING_RATE_PER_LB: '',
  MAX_SHIPPING: '',
  CUSTOMS_FEE: '',
  MAX_WEIGHT_FOR_STANDARD: '',
  MAX_PRICE_FOR_STANDARD: '',
  WHATSAPP_NUMBER: '',
  BANK_NAME: '',
  BANK_ACCOUNT_NAME: '',
  BANK_ACCOUNT_NUMBER: '',
  BANK_ACCOUNT_TYPE: '',
  PAYPAL_EMAIL: '',
  PAYPAL_CLIENT_ID: '',
}

const settings = ref<CalculatorSettings>({ ...BASE_SETTINGS })
const formData = ref<UpdateSettingsRequest>({})

function normalizeSettings(raw: Partial<Record<string, string>>): CalculatorSettings {
  return {
    TAX_RATE: raw.TAX_RATE ?? '',
    HANDLING_RATE: raw.HANDLING_RATE ?? '',
    SHIPPING_RATE_PER_LB: raw.SHIPPING_RATE_PER_LB ?? '',
    MAX_SHIPPING: raw.MAX_SHIPPING ?? '',
    CUSTOMS_FEE: raw.CUSTOMS_FEE ?? '',
    MAX_WEIGHT_FOR_STANDARD: raw.MAX_WEIGHT_FOR_STANDARD ?? '',
    MAX_PRICE_FOR_STANDARD: raw.MAX_PRICE_FOR_STANDARD ?? '',
    WHATSAPP_NUMBER: raw.WHATSAPP_NUMBER ?? '',
    BANK_NAME: raw.BANK_NAME ?? '',
    BANK_ACCOUNT_NAME: raw.BANK_ACCOUNT_NAME ?? '',
    BANK_ACCOUNT_NUMBER: raw.BANK_ACCOUNT_NUMBER ?? '',
    BANK_ACCOUNT_TYPE: raw.BANK_ACCOUNT_TYPE ?? '',
    PAYPAL_EMAIL: raw.PAYPAL_EMAIL ?? '',
    PAYPAL_CLIENT_ID: raw.PAYPAL_CLIENT_ID ?? '',
  }
}

onMounted(async () => {
  try {
    settings.value = normalizeSettings(await settingsService.getAll())
  } catch (_err) {
    error.value = 'Error al cargar la configuracion'
  } finally {
    loading.value = false
  }
})

function updateField(key: keyof CalculatorSettings, value: string) {
  formData.value = { ...formData.value, [toCamelCase(key)]: value }
}

function toCamelCase(str: string): string {
  return str.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
}

async function saveSettings() {
  saving.value = true
  error.value = null
  success.value = null

  try {
    settings.value = normalizeSettings(await settingsService.update(formData.value))
    formData.value = {}
    success.value = 'Configuracion guardada correctamente'
    setTimeout(() => {
      success.value = null
    }, 3000)
  } catch (_err) {
    error.value = 'Error al guardar la configuracion'
  } finally {
    saving.value = false
  }
}

function formatSettingName(key: string): string {
  const names: Record<string, string> = {
    TAX_RATE: 'Tasa de Impuesto (%)',
    HANDLING_RATE: 'Tasa de Handling (%)',
    SHIPPING_RATE_PER_LB: 'Costo de Envio por Libra',
    MAX_SHIPPING: 'Envio Maximo',
    CUSTOMS_FEE: 'Tarifa de Aduana',
    MAX_WEIGHT_FOR_STANDARD: 'Peso Maximo Estandar (lbs)',
    MAX_PRICE_FOR_STANDARD: 'Precio Maximo Estandar ($)',
    WHATSAPP_NUMBER: 'Numero de WhatsApp',
    BANK_NAME: 'Banco (texto)',
    BANK_ACCOUNT_NAME: 'Nombre de cuenta bancaria',
    BANK_ACCOUNT_NUMBER: 'Numero de cuenta bancaria',
    BANK_ACCOUNT_TYPE: 'Tipo de cuenta bancaria',
    PAYPAL_EMAIL: 'Correo de PayPal',
    PAYPAL_CLIENT_ID: 'PayPal Client ID',
  }
  return names[key] || key
}

function formatDescription(key: string): string {
  const descriptions: Record<string, string> = {
    TAX_RATE: 'Porcentaje de impuestos (0.15 = 15%)',
    HANDLING_RATE: 'Porcentaje de manejo (0.0927 = 9.27%)',
    SHIPPING_RATE_PER_LB: 'Costo por libra para envio',
    MAX_SHIPPING: 'Monto maximo de envio',
    CUSTOMS_FEE: 'Tarifa fija de aduana en dolares',
    MAX_WEIGHT_FOR_STANDARD: 'Peso maximo para categoria estandar',
    MAX_PRICE_FOR_STANDARD: 'Precio maximo para categoria estandar',
    WHATSAPP_NUMBER: 'Numero para contacto de WhatsApp',
    BANK_NAME: 'Nombre del banco mostrado al cliente',
    BANK_ACCOUNT_NAME: 'Titular de la cuenta para transferencias',
    BANK_ACCOUNT_NUMBER: 'Numero de cuenta para transferencias',
    BANK_ACCOUNT_TYPE: 'Ahorros, corriente, etc.',
    PAYPAL_EMAIL: 'Correo receptor de pagos PayPal',
    PAYPAL_CLIENT_ID: 'Client ID de PayPal (Live o Sandbox)',
  }
  return descriptions[key] || ''
}
</script>

<template>
  <div class="min-h-screen pt-[64px] pb-16">
    <!-- Gradient Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />

    <div class="relative max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <FadeIn direction="up">
        <div class="text-center mb-12">
          <h1 class="text-4xl md:text-5xl font-bold mb-4">
            <span class="gradient-text">Configuración</span> de Calculadora
          </h1>
          <p class="text-lg text-zinc-400 max-w-2xl mx-auto">
            Ajusta los parámetros de cálculo de precios para la importación
          </p>
        </div>
      </FadeIn>

      <!-- Loading State -->
      <FadeIn v-if="loading" direction="up" :delay="100">
        <div class="glass rounded-2xl p-12 text-center border border-white/5">
          <div class="w-16 h-16 border-4 border-teal-500/20 border-t-teal-500 rounded-full animate-spin mx-auto mb-6" />
          <p class="text-zinc-400">Cargando configuración...</p>
        </div>
      </FadeIn>

      <!-- Settings Form -->
      <FadeIn v-else direction="up" :delay="100">
        <div class="glass rounded-2xl p-6 sm:p-8 border border-white/5">
          <!-- Success/Error Messages -->
          <div v-if="success" class="mb-6 p-4 bg-green-500/20 border border-green-500/30 rounded-xl">
            <p class="text-green-400 text-center font-medium">{{ success }}</p>
          </div>
          <div v-if="error" class="mb-6 p-4 bg-red-500/20 border border-red-500/30 rounded-xl">
            <p class="text-red-400 text-center font-medium">{{ error }}</p>
          </div>

          <form @submit.prevent="saveSettings" class="space-y-6">
            <!-- Settings Fields -->
            <div class="space-y-4">
              <div v-for="(value, key) in settings" :key="key" class="space-y-2">
                <label :for="key" class="block text-sm font-medium text-zinc-300">
                  {{ formatSettingName(key) }}
                </label>
                <input
                  :id="key"
                  :value="formData[toCamelCase(key)] ?? settings[key as keyof CalculatorSettings]"
                  @input="updateField(key as keyof CalculatorSettings, ($event.target as HTMLInputElement).value)"
                  type="text"
                  class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
                />
                <p class="text-xs text-zinc-500">{{ formatDescription(key) }}</p>
              </div>
            </div>

            <!-- Save Button -->
            <button
              type="submit"
              :disabled="saving"
              class="w-full btn-primary py-3.5 text-base font-semibold flex items-center justify-center gap-2"
            >
              <svg v-if="saving" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              <span v-if="saving">Guardando...</span>
              <span v-else>Guardar Configuración</span>
            </button>
          </form>
        </div>
      </FadeIn>

      <!-- Info Box -->
      <FadeIn direction="up" :delay="200">
        <div class="mt-8 p-4 bg-zinc-800/50 rounded-xl border border-zinc-700/30">
          <div class="flex items-start space-x-3">
            <svg class="w-5 h-5 text-teal-400 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p class="text-sm text-zinc-400">
                Los cambios en la configuración afectarán inmediatamente los cálculos de precios para todos los usuarios.
                Asegúrate de que los valores sean correctos antes de guardar.
              </p>
            </div>
          </div>
        </div>
      </FadeIn>
    </div>
  </div>
</template>


