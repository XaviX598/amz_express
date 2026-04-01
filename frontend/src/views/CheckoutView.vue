<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { addressService, type Address } from '@/services/address'
import { cartService } from '@/services/cart'
import { pricingService } from '@/services/pricing'
import { orderService } from '@/services/order'
import FadeIn from '@/components/FadeIn.vue'
import type { PaymentMethod, ShippingOption } from '@/types'

interface NewAddress {
  street: string
  city: string
  state: string
  postalCode: string
  phone: string
}

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()

interface CartItem {
  id: number
  productName: string
  productPrice: number
  weight: number
  source: 'url' | 'manual'
}

interface PackageBreakdown {
  id: number
  items: CartItem[]
  subtotalPrice: number
  subtotalWeight: number
  pricing: any
  categoryC: boolean
}

interface PaymentSettings {
  bankName: string
  bankAccountName: string
  bankAccountNumber: string
  bankAccountType: string
  paypalClientId: string
  paypalEmail: string
  whatsappNumber: string
}

interface TransferBank {
  code: 'PICHINCHA' | 'GUAYAQUIL'
  name: string
  accountNumber: string
  accountHolder: string
}

interface ShippingOptionItem {
  value: ShippingOption
  label: string
  description: string
}

const shippingOptions: ShippingOptionItem[] = [
  { value: 'PICKUP', label: 'Retiro en bodega', description: 'Recoges tu pedido en nuestra bodega.' },
  { value: 'UBER', label: 'Delivery Uber', description: 'Entrega en Quito por Uber.' },
  { value: 'SERVIENTREGA', label: 'Servientrega', description: 'Envío nacional por Servientrega.' }
]

const warehouseAddress = 'Alfonso Mora ss23 y Quero, en rompevelocidades'

const transferBanks: TransferBank[] = [
  {
    code: 'PICHINCHA',
    name: 'Banco Pichincha',
    accountNumber: '2204451612',
    accountHolder: 'Kevin Xavier Aguilar Velasco'
  },
  {
    code: 'GUAYAQUIL',
    name: 'Banco Guayaquil',
    accountNumber: '0000',
    accountHolder: 'Kevin Xavier Aguilar Velasco'
  }
]

const loading = ref(true)
const addresses = ref<Address[]>([])
const selectedAddressId = ref<number | null>(null)
const cartItems = ref<CartItem[]>([])
const packages = ref<PackageBreakdown[]>([])
const calculating = ref(false)
const error = ref<string | null>(null)
const processingOrder = ref(false)
const paymentSettings = ref<PaymentSettings | null>(null)
const selectedShippingOption = ref<ShippingOption>('PICKUP')
const selectedPaymentMethod = ref<PaymentMethod>('TRANSFERENCIA')
const paymentReference = ref('')
const selectedTransferBankCode = ref<TransferBank['code']>('PICHINCHA')
const transferProofFile = ref<File | null>(null)
const transferProofFileName = ref('')
const showAddressForm = ref(false)
const newAddress = ref<NewAddress>({
  street: '',
  city: '',
  state: '',
  postalCode: '',
  phone: ''
})
const savingAddress = ref(false)
const paypalLoaded = ref(false)
const paypalProcessing = ref(false)
const paypalLoading = ref(false)
const selectedTransferBank = computed(
  () => transferBanks.find(bank => bank.code === selectedTransferBankCode.value) ?? transferBanks[0]
)
const requiresShippingAddress = computed(() => selectedShippingOption.value !== 'PICKUP')

declare global {
  interface Window {
    paypal?: {
      Buttons: (config: {
        style?: Record<string, string>
        createOrder: (_data: unknown, actions: { order: { create: (payload: unknown) => Promise<string> } }) => Promise<string>
        onApprove: (_data: unknown, actions: { order: { capture: () => Promise<{ id: string }> } }) => Promise<void>
        onError: (err: unknown) => void
      }) => {
        render: (selector: string) => Promise<void>
      }
    }
  }
}

// Get PayPal Client ID 
function getPayPalClientId() {
  const fromSettings = paymentSettings.value?.paypalClientId?.trim()
  if (fromSettings) return fromSettings

  const fromEnv = String(import.meta.env.VITE_PAYPAL_CLIENT_ID || '').trim()
  if (fromEnv) return fromEnv

  // Fallback para pruebas en entorno local
  return 'sb'
}

function loadPayPalSdk(clientId: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const sdkUrl = `https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(clientId)}&currency=USD&intent=capture&locale=es_ES`
    const existingScript = document.querySelector<HTMLScriptElement>('script[data-paypal-sdk="true"]')

    if (existingScript) {
      if (existingScript.src === sdkUrl && window.paypal) {
        resolve()
        return
      }
      existingScript.remove()
    }

    const script = document.createElement('script')
    script.src = sdkUrl
    script.async = true
    script.setAttribute('data-paypal-sdk', 'true')
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('No se pudo cargar el SDK de PayPal'))
    document.head.appendChild(script)
  })
}

// Load PayPal button
async function loadPayPalButton() {
  if (paypalLoading.value || paypalLoaded.value) return

  const clientId = getPayPalClientId()
  if (!clientId) {
    error.value = 'No hay configuracion de PayPal disponible.'
    return
  }

  paypalLoading.value = true
  error.value = null

  try {
    await nextTick()
    const container = document.getElementById('paypal-button-container')
    if (!container) {
      throw new Error('No se encontro el contenedor de PayPal')
    }

    container.innerHTML = ''
    await loadPayPalSdk(clientId)

    if (!window.paypal?.Buttons) {
      throw new Error('PayPal no quedo disponible en la pagina')
    }

    await window.paypal.Buttons({
      style: {
        layout: 'vertical',
        color: 'blue',
        shape: 'rect',
        label: 'paypal'
      },
      createOrder: (_data, actions) => {
        return actions.order.create({
          purchase_units: [{
            amount: {
              value: totalPrice.value.toFixed(2),
              currency_code: 'USD'
            }
          }]
        })
      },
      onApprove: async (_data, actions) => {
        paypalProcessing.value = true
        try {
          const order = await actions.order.capture()
          paymentReference.value = order.id
          await confirmOrderWithPayment()
        } catch (err: any) {
          console.error('PayPal capture error:', err)
          error.value = 'Error al procesar el pago. Intenta de nuevo.'
        } finally {
          paypalProcessing.value = false
        }
      },
      onError: (err) => {
        console.error('PayPal error:', err)
        error.value = 'Error en el pago con PayPal. Intenta de nuevo.'
      }
    }).render('#paypal-button-container')

    paypalLoaded.value = true
  } catch (err: any) {
    console.error('PayPal init error:', err)
    error.value = err?.message || 'No se pudo inicializar PayPal.'
    paypalLoaded.value = false
  } finally {
    paypalLoading.value = false
  }
}

// Watch for PayPal selection
watch(selectedPaymentMethod, (method) => {
  if (method === 'PAYPAL') {
    paypalLoaded.value = false
    setTimeout(() => {
      void loadPayPalButton()
    }, 100)
  } else {
    paypalLoaded.value = false
  }
})

watch(selectedShippingOption, async () => {
  showAddressForm.value = false
  error.value = null
  await calculatePackages()
})

// Get paid weight (rounds up)
function getPaidWeight(weight: number): number {
  return Math.ceil(weight)
}

// Split items into 4x4 packages
function splitInto4x4Packages(items: CartItem[]): CartItem[][] {
  const MAX_PRICE = 400
  const MAX_WEIGHT = 4
  const packages: CartItem[][] = []
  
  const sorted = [...items].sort((a, b) => b.weight - a.weight)
  
  for (const item of sorted) {
    let bestPkgIndex = -1
    let bestPaidWeight = Infinity
    
    for (let i = 0; i < packages.length; i++) {
      const pkg = packages[i]
      const pkgPrice = pkg.reduce((acc, p) => acc + p.productPrice, 0)
      const pkgWeight = pkg.reduce((acc, p) => acc + p.weight, 0)
      
      if (pkgPrice + item.productPrice <= MAX_PRICE && pkgWeight + item.weight <= MAX_WEIGHT) {
        const paidWeight = getPaidWeight(pkgWeight + item.weight)
        
        if (paidWeight < bestPaidWeight) {
          bestPaidWeight = paidWeight
          bestPkgIndex = i
        } else if (paidWeight === bestPaidWeight) {
          const currentPkgWeight = pkg.reduce((acc, p) => acc + p.weight, 0)
          const currentPkgPrice = pkg.reduce((acc, p) => acc + p.productPrice, 0)
          const currentLoaded = Math.max(currentPkgWeight / MAX_WEIGHT, currentPkgPrice / MAX_PRICE)
          const bestLoaded = Math.max(pkgWeight / MAX_WEIGHT, pkgPrice / MAX_PRICE)
          
          if (currentLoaded > bestLoaded) {
            bestPkgIndex = i
          }
        }
      }
    }
    
    if (bestPkgIndex >= 0) {
      packages[bestPkgIndex].push(item)
    } else {
      packages.push([item])
    }
  }
  
  return packages
}

// Filter packages that are NOT category C
const validPackages = computed(() => packages.value.filter(p => !p.categoryC))
const categoryCPackages = computed(() => packages.value.filter(p => p.categoryC))

const totalPrice = computed(() => validPackages.value.reduce((acc, pkg) => acc + (pkg.pricing?.totalPrice || 0), 0))
const selectedAddress = computed(() => addresses.value.find(a => a.id === selectedAddressId.value))

function formatCurrency(value: number | undefined | null): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

async function saveAddress() {
  if (!newAddress.value.street || !newAddress.value.city || !newAddress.value.phone) {
    error.value = 'Completa los campos requeridos'
    return
  }
  
  savingAddress.value = true
  error.value = null
  
  try {
    const created = await addressService.create({
      addressType: 'SHIPPING',
      street: newAddress.value.street,
      city: newAddress.value.city,
      state: newAddress.value.state || '',
      postalCode: newAddress.value.postalCode || '',
      country: 'Ecuador',
      phone: newAddress.value.phone,
      isDefault: addresses.value.length === 0
    })
    
    addresses.value.unshift(created)
    selectedAddressId.value = created.id!
    showAddressForm.value = false
    
    // Reset form
    newAddress.value = { street: '', city: '', state: '', postalCode: '', phone: '' }
  } catch (err: any) {
    error.value = err.message || 'Error al guardar dirección'
  } finally {
    savingAddress.value = false
  }
}

async function calculatePackages() {
  if (cartItems.value.length === 0) {
    packages.value = []
    return
  }

  calculating.value = true
  const splitPackages = splitInto4x4Packages(cartItems.value)
  const results: PackageBreakdown[] = []

  for (let i = 0; i < splitPackages.length; i++) {
    const pkgItems = splitPackages[i]
    const subtotalPrice = pkgItems.reduce((acc, item) => acc + item.productPrice, 0)
    const subtotalWeight = pkgItems.reduce((acc, item) => acc + item.weight, 0)

    try {
      const pricingResult = await pricingService.calculate({
        productPrice: subtotalPrice,
        weight: subtotalWeight,
        shippingOption: selectedShippingOption.value
      })
      results.push({
        id: i + 1,
        items: pkgItems,
        subtotalPrice,
        subtotalWeight,
        pricing: pricingResult,
        categoryC: pricingResult?.categoryC ?? false
      })
    } catch (_err) {
      results.push({
        id: i + 1,
        items: pkgItems,
        subtotalPrice,
        subtotalWeight,
        pricing: null,
        categoryC: true
      })
    }
  }

  packages.value = results
  calculating.value = false
}

async function loadData() {
  loading.value = true
  error.value = null
  
  try {
    // Load addresses
    addresses.value = await addressService.getShipping()
    
    // Set default address
    const defaultAddr = addresses.value.find(a => a.isDefault)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id!
    } else if (addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].id!
    }
    
    // Load cart items
    const cartData = await cartService.getCart()
    cartItems.value = cartData.map((item: any) => ({
      id: item.id || Date.now() + Math.random(),
      productName: item.productName,
      productPrice: item.productPrice,
      weight: item.weight,
      source: item.source as 'url' | 'manual'
    }))
    
    // Load payment settings
    try {
      const settingsRes = await fetch('/api/payment/settings')
      if (settingsRes.ok) {
        paymentSettings.value = await settingsRes.json()
      }
    } catch (e) {
      console.error('Failed to load payment settings:', e)
    }

    await calculatePackages()
  } catch (err: any) {
    error.value = err.message || 'Error al cargar datos'
  } finally {
    loading.value = false
  }
}

function onTransferProofSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null

  if (!file) {
    transferProofFile.value = null
    transferProofFileName.value = ''
    return
  }

  if (!file.type.startsWith('image/')) {
    error.value = 'El comprobante debe ser una imagen (jpg, png, webp).'
    transferProofFile.value = null
    transferProofFileName.value = ''
    input.value = ''
    return
  }

  const maxBytes = 10 * 1024 * 1024
  if (file.size > maxBytes) {
    error.value = 'El comprobante supera 10MB. Sube una imagen mas ligera.'
    transferProofFile.value = null
    transferProofFileName.value = ''
    input.value = ''
    return
  }

  transferProofFile.value = file
  transferProofFileName.value = file.name
  error.value = null
}

async function confirmOrder() {
  if (requiresShippingAddress.value && !selectedAddressId.value) {
    error.value = 'Selecciona una dirección de envío'
    return
  }
  
  if (validPackages.value.length === 0) {
    error.value = 'No hay paquetes válidos para comprar'
    return
  }
  
  // If PayPal is selected, user must complete payment in the PayPal button
  if (selectedPaymentMethod.value === 'PAYPAL') {
    if (paypalLoaded.value) {
      error.value = 'Por favor completa el pago con PayPal usando el boton de arriba'
    } else {
      error.value = 'Cargando PayPal... por favor espera'
    }
    return
  }

  if (selectedPaymentMethod.value === 'TRANSFERENCIA' && !transferProofFile.value) {
    error.value = 'Debes subir la imagen del comprobante para continuar'
    return
  }
  
  // For other payment methods, create order directly
  await confirmOrderWithPayment()
}

async function confirmOrderWithPayment() {
  processingOrder.value = true
  error.value = null
  
  try {
    const createdOrderIds: number[] = []
    const orderPaymentReference = selectedPaymentMethod.value === 'PAYPAL'
      ? paymentReference.value || undefined
      : undefined

    // Create one order per package
    for (const pkg of validPackages.value) {
      const createdOrder = await orderService.createOrder({
        productName: pkg.items.map(i => i.productName).join(', '),
        productPrice: pkg.subtotalPrice,
        weight: pkg.subtotalWeight,
        shippingOption: selectedShippingOption.value,
        paymentMethod: selectedPaymentMethod.value,
        paymentReference: orderPaymentReference
      })
      createdOrderIds.push(createdOrder.id)
    }

    if (selectedPaymentMethod.value === 'TRANSFERENCIA') {
      if (!transferProofFile.value) {
        throw new Error('Debes subir la imagen del comprobante')
      }
      try {
        await orderService.submitTransferProof({
          orderIds: createdOrderIds,
          bankCode: selectedTransferBank.value.code,
          bankName: selectedTransferBank.value.name,
          accountNumber: selectedTransferBank.value.accountNumber,
          accountHolder: selectedTransferBank.value.accountHolder,
          proofFile: transferProofFile.value
        })
      } catch (proofError: any) {
        await cartService.saveAllItems([])
        transferProofFile.value = null
        transferProofFileName.value = ''
        const reason = proofError?.message ? ` Detalle: ${proofError.message}` : ''
        uiStore.warning(`Pedido creado, pero no se pudo enviar el comprobante automaticamente.${reason} Abre tus pedidos y contactanos por WhatsApp.`)
        router.push('/ordenes')
        return
      }
    }
    
    // Clear cart after successful order
    await cartService.saveAllItems([])
    // Show success message based on payment method
    if (selectedPaymentMethod.value === 'PAYPAL') {
      uiStore.success('Pago realizado. Tu pedido esta confirmado.')
    } else if (selectedPaymentMethod.value === 'TRANSFERENCIA') {
      uiStore.success('Comprobante enviado. Tu pedido esta en comprobacion de pago.')
    } else {
      uiStore.success('Pedido creado. Te contactaremos para confirmar el pago.')
    }

    transferProofFile.value = null
    transferProofFileName.value = ''
    paymentReference.value = ''
    router.push('/ordenes')
  } catch (err: any) {
    error.value = err.message || 'Error al crear pedido'
  } finally {
    processingOrder.value = false
  }
}

onMounted(() => {
  if (!authStore.isAuthenticated) {
    router.push('/login?redirect=/checkout')
    return
  }
  loadData()
})
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] pb-16 relative overflow-hidden">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />
    
    <div class="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <FadeIn direction="up">
        <h1 class="text-3xl font-bold text-white mb-8">Checkout</h1>
        
        <!-- Loading state -->
        <div v-if="loading || calculating" class="flex items-center justify-center py-16">
          <div class="flex flex-col items-center gap-3">
            <svg class="w-10 h-10 animate-spin text-teal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            <span class="text-zinc-400">{{ calculating ? 'Calculando paquetes...' : 'Cargando...' }}</span>
          </div>
        </div>
        
        <div v-else class="space-y-6">
          <!-- Error message -->
          <div v-if="error" class="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
            {{ error }}
          </div>
          
          <!-- Empty cart -->
          <div v-if="cartItems.length === 0" class="text-center py-16">
            <p class="text-zinc-400 mb-4">Tu carrito está vacío</p>
            <button @click="router.push('/calculadora')" class="btn-primary">
              Ir a la calculadora
            </button>
          </div>
          
          <template v-else>
            <!-- Category C packages warning -->
            <div v-if="categoryCPackages.length > 0" class="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
              <p class="text-amber-300 font-medium mb-2">
                {{ categoryCPackages.length }} paquete(s) aplican categoría C
              </p>
              <p class="text-amber-200 text-sm mb-3">
                Estos paquetes exceden los límites de 4kg o $400 FOB y no pueden ser comprados directamente.
              </p>
              <a 
                href="https://wa.me/593998050600?text=Hola%2C%20me%20interesan%20los%20siguientes%20productos%20que%20aplican%20categor%C3%ADa%20C%3A%0A%0A" 
                target="_blank" 
                rel="noopener noreferrer"
                class="inline-flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm font-medium transition-colors"
              >
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
                Cotizar por WhatsApp
              </a>
            </div>
            
            <!-- Shipping option -->
            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-900/60 p-6">
              <h2 class="text-xl font-semibold text-white mb-4">Tipo de entrega</h2>
              <div class="space-y-3">
                <label
                  v-for="option in shippingOptions"
                  :key="option.value"
                  :class="['flex items-start p-4 rounded-xl border cursor-pointer transition-all', selectedShippingOption === option.value ? 'border-teal-500 bg-teal-500/10' : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50']"
                >
                  <input
                    v-model="selectedShippingOption"
                    type="radio"
                    :value="option.value"
                    class="hidden"
                  />
                  <div v-if="selectedShippingOption === option.value" class="w-2.5 h-2.5 rounded-full bg-teal-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div v-else class="w-2.5 h-2.5 rounded-full border border-zinc-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div class="flex-1">
                    <p class="text-white font-medium">{{ option.label }}</p>
                    <p class="text-zinc-400 text-sm">{{ option.description }}</p>
                  </div>
                </label>
              </div>
            </div>

            <!-- Address / pickup details -->
            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-900/60 p-6">
              <template v-if="requiresShippingAddress">
                <div class="flex items-center justify-between mb-4">
                  <h2 class="text-xl font-semibold text-white">Dirección de envío</h2>
                  <button
                    v-if="!showAddressForm"
                    @click="showAddressForm = true"
                    class="text-teal-400 hover:text-teal-300 text-sm underline"
                  >
                    + Agregar dirección
                  </button>
                </div>

                <!-- Add address form -->
                <div v-if="showAddressForm" class="mb-4 p-4 rounded-xl bg-zinc-800/50 border border-zinc-700/50">
                  <p class="text-teal-400 font-medium mb-3">Nueva dirección</p>
                  <div class="space-y-3">
                    <input
                      v-model="newAddress.street"
                      type="text"
                      placeholder="Dirección *"
                      class="w-full bg-zinc-900/60 border border-zinc-700/50 rounded-lg py-2 px-3 text-white placeholder-zinc-600"
                    />
                    <div class="grid grid-cols-2 gap-2">
                      <input
                        v-model="newAddress.city"
                        type="text"
                        placeholder="Ciudad *"
                        class="bg-zinc-900/60 border border-zinc-700/50 rounded-lg py-2 px-3 text-white placeholder-zinc-600"
                      />
                      <input
                        v-model="newAddress.state"
                        type="text"
                        placeholder="Provincia"
                        class="bg-zinc-900/60 border border-zinc-700/50 rounded-lg py-2 px-3 text-white placeholder-zinc-600"
                      />
                    </div>
                    <div class="grid grid-cols-2 gap-2">
                      <input
                        v-model="newAddress.postalCode"
                        type="text"
                        placeholder="Código postal"
                        class="bg-zinc-900/60 border border-zinc-700/50 rounded-lg py-2 px-3 text-white placeholder-zinc-600"
                      />
                      <input
                        v-model="newAddress.phone"
                        type="text"
                        placeholder="Teléfono *"
                        class="bg-zinc-900/60 border border-zinc-700/50 rounded-lg py-2 px-3 text-white placeholder-zinc-600"
                      />
                    </div>
                  </div>
                  <div class="flex gap-2 mt-3">
                    <button
                      @click="showAddressForm = false"
                      class="flex-1 py-2 px-3 rounded-lg border border-zinc-600 text-zinc-300 hover:bg-zinc-700/50 text-sm"
                    >
                      Cancelar
                    </button>
                    <button
                      @click="saveAddress"
                      :disabled="savingAddress"
                      class="flex-1 py-2 px-3 rounded-lg bg-teal-600 text-white hover:bg-teal-700 text-sm disabled:opacity-50"
                    >
                      {{ savingAddress ? 'Guardando...' : 'Guardar' }}
                    </button>
                  </div>
                </div>

                <div v-if="addresses.length === 0 && !showAddressForm" class="text-center py-4">
                  <p class="text-zinc-400 mb-3">No tienes direcciones guardadas</p>
                </div>

                <div v-else class="space-y-3">
                  <label
                    v-for="addr in addresses"
                    :key="addr.id"
                    :class="['flex items-start p-4 rounded-xl border cursor-pointer transition-all', selectedAddressId === addr.id ? 'border-teal-500 bg-teal-500/10' : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50']"
                  >
                    <input
                      v-model="selectedAddressId"
                      type="radio"
                      :value="addr.id"
                      class="hidden"
                    />
                    <div v-if="selectedAddressId === addr.id" class="w-2.5 h-2.5 rounded-full bg-teal-500 mt-1.5 mr-3 flex-shrink-0" />
                    <div v-else class="w-2.5 h-2.5 rounded-full border border-zinc-500 mt-1.5 mr-3 flex-shrink-0" />
                    <div class="flex-1">
                      <p class="text-white font-medium">{{ addr.street }}</p>
                      <p class="text-zinc-400 text-sm">{{ addr.city }}, {{ addr.state }} {{ addr.postalCode }}</p>
                      <p class="text-zinc-500 text-sm">{{ addr.phone }}</p>
                    </div>
                  </label>
                </div>
              </template>

              <template v-else>
                <h2 class="text-xl font-semibold text-white mb-4">Retiro en bodega</h2>
                <div class="rounded-xl bg-zinc-800/50 border border-zinc-700/50 p-4 space-y-2">
                  <p class="text-zinc-300 text-sm">Dirección de bodega:</p>
                  <p class="text-teal-300 font-medium">{{ warehouseAddress }}</p>
                </div>
              </template>
            </div>
            <!-- Payment method selection -->
            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-900/60 p-6">
              <h2 class="text-xl font-semibold text-white mb-4">Metodo de pago</h2>

              <div class="space-y-3">
                <label
                  :class="['flex items-start p-4 rounded-xl border cursor-pointer transition-all', selectedPaymentMethod === 'TRANSFERENCIA' ? 'border-teal-500 bg-teal-500/10' : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50']"
                >
                  <input
                    v-model="selectedPaymentMethod"
                    type="radio"
                    value="TRANSFERENCIA"
                    class="hidden"
                  />
                  <div v-if="selectedPaymentMethod === 'TRANSFERENCIA'" class="w-2.5 h-2.5 rounded-full bg-teal-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div v-else class="w-2.5 h-2.5 rounded-full border border-zinc-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div class="flex-1">
                    <p class="text-white font-medium">Transferencia bancaria</p>
                    <p class="text-zinc-400 text-sm">Sube el comprobante y lo enviamos al administrador para revision.</p>
                  </div>
                </label>

                <label
                  :class="['flex items-start p-4 rounded-xl border cursor-pointer transition-all', selectedPaymentMethod === 'PAYPAL' ? 'border-teal-500 bg-teal-500/10' : 'border-zinc-700/50 hover:border-zinc-600 bg-zinc-800/50']"
                >
                  <input
                    v-model="selectedPaymentMethod"
                    type="radio"
                    value="PAYPAL"
                    class="hidden"
                  />
                  <div v-if="selectedPaymentMethod === 'PAYPAL'" class="w-2.5 h-2.5 rounded-full bg-teal-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div v-else class="w-2.5 h-2.5 rounded-full border border-zinc-500 mt-1.5 mr-3 flex-shrink-0" />
                  <div class="flex-1">
                    <p class="text-white font-medium">PayPal</p>
                    <p class="text-zinc-400 text-sm">Paga con tu cuenta PayPal</p>
                  </div>
                </label>

              </div>

              <div v-if="selectedPaymentMethod === 'TRANSFERENCIA'" class="mt-4 p-4 rounded-xl bg-zinc-800/50 border border-zinc-700/50 space-y-4">
                <div>
                  <p class="text-teal-400 font-medium mb-2">Selecciona el banco</p>
                  <div class="grid grid-cols-2 gap-2">
                    <button
                      v-for="bank in transferBanks"
                      :key="bank.code"
                      type="button"
                      @click="selectedTransferBankCode = bank.code"
                      :class="[
                        'rounded-lg px-3 py-2 text-sm font-medium border transition-colors',
                        selectedTransferBankCode === bank.code
                          ? 'bg-teal-500/20 border-teal-500 text-teal-300'
                          : 'bg-zinc-900/60 border-zinc-700/50 text-zinc-300 hover:border-zinc-600'
                      ]"
                    >
                      {{ bank.name }}
                    </button>
                  </div>
                </div>

                <div class="rounded-lg bg-zinc-900/60 border border-zinc-700/50 p-3">
                  <p class="text-zinc-300 text-sm"><span class="text-zinc-500">Banco:</span> {{ selectedTransferBank.name }}</p>
                  <p class="text-zinc-300 text-sm"><span class="text-zinc-500">Cuenta:</span> {{ selectedTransferBank.accountNumber }}</p>
                  <p class="text-zinc-300 text-sm"><span class="text-zinc-500">Titular:</span> {{ selectedTransferBank.accountHolder }}</p>
                </div>

                <div class="pt-3 border-t border-zinc-700/50">
                  <label class="block text-sm text-zinc-300 mb-2">Sube tu comprobante (imagen)</label>
                  <input
                    type="file"
                    accept="image/*"
                    @change="onTransferProofSelected"
                    class="block w-full text-sm text-zinc-300 file:mr-4 file:rounded-md file:border-0 file:bg-teal-600 file:px-4 file:py-2 file:text-sm file:font-medium file:text-white hover:file:bg-teal-700"
                  />
                  <p v-if="transferProofFileName" class="text-xs text-zinc-400 mt-2">
                    Archivo seleccionado: {{ transferProofFileName }}
                  </p>
                  <p class="text-xs text-zinc-500 mt-2">
                    Al confirmar compra, enviaremos el comprobante automaticamente al administrador.
                  </p>
                </div>
              </div>

              <div v-if="selectedPaymentMethod === 'PAYPAL'" class="mt-4 p-4 rounded-xl bg-zinc-800/50 border border-zinc-700/50">
                <p class="text-teal-400 font-medium mb-2">Pago con PayPal</p>
                <p class="text-zinc-300 text-sm mb-3">
                  Haz clic en el boton de PayPal para completar el pago.
                </p>
                <div id="paypal-button-container"></div>
              </div>

            </div>

            <!-- Packages summary -->
            <div class="rounded-2xl border border-zinc-700/50 bg-zinc-900/60 p-6">
              <h2 class="text-xl font-semibold text-white mb-4">Paquetes a comprar</h2>
              
              <div class="space-y-4">
                <div v-for="pkg in validPackages" :key="pkg.id" class="rounded-xl border border-zinc-700/40 bg-zinc-800/50 p-4">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-white font-medium">Paquete {{ pkg.id }}</span>
                    <span class="text-teal-300 font-semibold">{{ formatCurrency(pkg.pricing?.totalPrice) }}</span>
                  </div>
                  <p class="text-zinc-400 text-sm">
                    {{ pkg.items.length }} producto(s) · {{ formatCurrency(pkg.subtotalPrice) }} · {{ pkg.subtotalWeight.toFixed(2) }} lbs
                  </p>
                  <p class="text-zinc-500 text-xs mt-1 line-clamp-1">
                    {{ pkg.items.map(i => i.productName).join(' · ') }}
                  </p>
                </div>
              </div>
              
              <div class="mt-4 pt-4 border-t border-zinc-700/50 flex items-center justify-between">
                <span class="text-lg text-white font-medium">Total</span>
                <span class="text-2xl font-bold text-teal-400">{{ formatCurrency(totalPrice) }}</span>
              </div>
            </div>
            
            <!-- Confirm button -->
            <button 
              @click="confirmOrder" 
              :disabled="processingOrder || (requiresShippingAddress && !selectedAddressId) || validPackages.length === 0"
              class="w-full py-4 flex items-center justify-center gap-2 btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg v-if="processingOrder" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <span v-if="processingOrder">Procesando...</span>
              <span v-else-if="selectedPaymentMethod === 'TRANSFERENCIA'">Pagar y enviar comprobante</span>
              <span v-else>Confirmar compra</span>
            </button>
          </template>
        </div>
      </FadeIn>
    </div>
  </div>
</template>





