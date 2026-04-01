<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { addressService, type Address } from '@/services/address'
import FadeIn from '@/components/FadeIn.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

const addresses = ref<Address[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingAddress = ref<Address | null>(null)

const formData = ref({
  addressType: 'SHIPPING' as 'SHIPPING' | 'BILLING',
  street: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'Ecuador',
  phone: '',
  isDefault: false
})

const shippingAddresses = computed(() => addresses.value.filter(a => a.addressType === 'SHIPPING'))
const billingAddresses = computed(() => addresses.value.filter(a => a.addressType === 'BILLING'))

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await addressService.getAll()
  } catch (err) {
    console.error('Failed to load addresses:', err)
  } finally {
    loading.value = false
  }
}

function openForm(type: 'SHIPPING' | 'BILLING', address?: Address) {
  if (address) {
    editingAddress.value = address
    formData.value = { ...address }
  } else {
    editingAddress.value = null
    formData.value = {
      addressType: type,
      street: '',
      city: '',
      state: '',
      postalCode: '',
      country: 'Ecuador',
      phone: '',
      isDefault: false
    }
  }
  showForm.value = true
}

async function saveAddress() {
  try {
    if (editingAddress.value?.id) {
      await addressService.update(editingAddress.value.id, formData.value)
      uiStore.success('Direccion actualizada')
    } else {
      await addressService.create(formData.value)
      uiStore.success('Direccion guardada')
    }
    showForm.value = false
    await loadAddresses()
  } catch (err) {
    console.error('Failed to save address:', err)
    uiStore.error('No se pudo guardar la direccion')
  }
}

async function deleteAddress(id: number) {
  const accepted = await uiStore.openConfirm({
    title: 'Eliminar direccion',
    message: 'Estas seguro de eliminar esta direccion?',
    confirmText: 'Eliminar',
    cancelText: 'Cancelar',
    danger: true,
  })

  if (!accepted) return

  try {
    await addressService.delete(id)
    await loadAddresses()
    uiStore.success('Direccion eliminada')
  } catch (err) {
    console.error('Failed to delete address:', err)
    uiStore.error('No se pudo eliminar la direccion')
  }
}

function goToCheckout() {
  router.push('/checkout')
}

onMounted(() => {
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return
  }
  loadAddresses()
})
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] pb-16 relative overflow-hidden">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="relative max-w-4xl mx-auto px-4 py-8">
      <FadeIn direction="up">
        <div class="flex items-center justify-between mb-8">
          <h1 class="text-3xl font-bold text-white">Mis Direcciones</h1>
          <button @click="goToCheckout" class="px-4 py-2 bg-teal-600 hover:bg-teal-500 text-white rounded-xl font-medium">
            Ir al Checkout
          </button>
        </div>
        
        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-12">
          <svg class="w-8 h-8 animate-spin text-teal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        
        <div v-else class="space-y-8">
          <!-- Shipping Addresses -->
          <div class="bg-zinc-900/70 rounded-2xl p-6 border border-zinc-700/50">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xl font-bold text-white">Dirección de Envío</h2>
              <button @click="openForm('SHIPPING')" class="text-teal-400 hover:text-teal-300 text-sm">+ Agregar</button>
            </div>
            
            <div v-if="shippingAddresses.length === 0" class="text-zinc-500 text-sm">
              No hay direcciones de envío agregadas
            </div>
            
            <div v-else class="space-y-3">
              <div v-for="addr in shippingAddresses" :key="addr.id" class="flex items-start justify-between p-4 bg-zinc-800/50 rounded-xl border border-zinc-700/30">
                <div>
                  <p class="text-white">{{ addr.street }}</p>
                  <p class="text-zinc-400 text-sm">{{ addr.city }}, {{ addr.state }} {{ addr.postalCode }}</p>
                  <p class="text-zinc-400 text-sm">{{ addr.country }}</p>
                  <p class="text-zinc-400 text-sm">Tel: {{ addr.phone }}</p>
                  <span v-if="addr.isDefault" class="text-xs bg-teal-500/20 text-teal-400 px-2 py-0.5 rounded mt-1 inline-block">Predeterminada</span>
                </div>
                <div class="flex gap-2">
                  <button @click="openForm('SHIPPING', addr)" class="text-zinc-400 hover:text-white text-sm">Editar</button>
                  <button @click="deleteAddress(addr.id!)" class="text-red-400 hover:text-red-300 text-sm">Eliminar</button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Billing Addresses -->
          <div class="bg-zinc-900/70 rounded-2xl p-6 border border-zinc-700/50">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-xl font-bold text-white">Dirección de Facturación</h2>
              <button @click="openForm('BILLING')" class="text-teal-400 hover:text-teal-300 text-sm">+ Agregar</button>
            </div>
            
            <div v-if="billingAddresses.length === 0" class="text-zinc-500 text-sm">
              No hay direcciones de facturación agregadas
            </div>
            
            <div v-else class="space-y-3">
              <div v-for="addr in billingAddresses" :key="addr.id" class="flex items-start justify-between p-4 bg-zinc-800/50 rounded-xl border border-zinc-700/30">
                <div>
                  <p class="text-white">{{ addr.street }}</p>
                  <p class="text-zinc-400 text-sm">{{ addr.city }}, {{ addr.state }} {{ addr.postalCode }}</p>
                  <p class="text-zinc-400 text-sm">{{ addr.country }}</p>
                  <span v-if="addr.isDefault" class="text-xs bg-teal-500/20 text-teal-400 px-2 py-0.5 rounded mt-1 inline-block">Predeterminada</span>
                </div>
                <div class="flex gap-2">
                  <button @click="openForm('BILLING', addr)" class="text-zinc-400 hover:text-white text-sm">Editar</button>
                  <button @click="deleteAddress(addr.id!)" class="text-red-400 hover:text-red-300 text-sm">Eliminar</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </FadeIn>
      
      <!-- Form Modal -->
      <Teleport to="body">
        <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showForm = false"></div>
          <div class="relative bg-zinc-900 border border-zinc-700 rounded-2xl p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <h3 class="text-xl font-bold text-white mb-4">
              {{ editingAddress ? 'Editar' : 'Nueva' }} Dirección
            </h3>
            
            <form @submit.prevent="saveAddress" class="space-y-4">
              <div>
                <label class="block text-sm text-zinc-400 mb-1">Tipo</label>
                <select v-model="formData.addressType" class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white">
                  <option value="SHIPPING">Envío</option>
                  <option value="BILLING">Facturación</option>
                </select>
              </div>
              
              <div>
                <label class="block text-sm text-zinc-400 mb-1">Dirección *</label>
                <input v-model="formData.street" type="text" required class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white" placeholder="Calle, número, referencia" />
              </div>
              
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm text-zinc-400 mb-1">Ciudad *</label>
                  <input v-model="formData.city" type="text" required class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white" />
                </div>
                <div>
                  <label class="block text-sm text-zinc-400 mb-1">Provincia</label>
                  <input v-model="formData.state" type="text" class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white" />
                </div>
              </div>
              
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm text-zinc-400 mb-1">Código Postal</label>
                  <input v-model="formData.postalCode" type="text" class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white" />
                </div>
                <div>
                  <label class="block text-sm text-zinc-400 mb-1">Teléfono *</label>
                  <input v-model="formData.phone" type="tel" required class="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-3 px-4 text-white" />
                </div>
              </div>
              
              <div>
                <label class="flex items-center gap-2 text-zinc-300 cursor-pointer">
                  <input v-model="formData.isDefault" type="checkbox" class="w-4 h-4 rounded bg-zinc-800 border-zinc-700 text-teal-500" />
                  <span class="text-sm">Establecer como predeterminada</span>
                </label>
              </div>
              
              <div class="flex gap-3 pt-4">
                <button type="button" @click="showForm = false" class="flex-1 py-3 border border-zinc-600 text-zinc-300 rounded-xl hover:bg-zinc-800">Cancelar</button>
                <button type="submit" class="flex-1 py-3 bg-teal-600 text-white rounded-xl hover:bg-teal-500">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      </Teleport>
    </div>
  </div>
</template>

