<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useOrderStore } from '@/stores/order'
import { useAuthStore } from '@/stores/auth'
import type { ShippingOption, ProductInfo, PricingCalculation } from '@/types'
import { useRouter } from 'vue-router'
import FadeIn from '@/components/FadeIn.vue'
import { amazonService } from '@/services/amazon'
import { pricingService } from '@/services/pricing'
import { cartService } from '@/services/cart'

const router = useRouter()
const orderStore = useOrderStore()
const authStore = useAuthStore()

const amazonUrl = ref('')
const productTitle = ref('')
const productAsin = ref('')
const productPrice = ref<number | null>(null)
const productImage = ref<string | null>(null)
const weight = ref<number | null>(null)
const weightFromApi = ref(false)
const showWeightGuideModal = ref(false)
const weightConverterUnit = ref<'kg' | 'lb'>('kg')
const weightConverterInput = ref<number | null>(null)
const shippingOption: ShippingOption = 'PICKUP'
const calculating = ref(false)
const showResult = ref(false)
const scraping = ref(false)
const scrapeError = ref<string | null>(null)
const showConfirmation = ref(false)
type InputMode = 'url' | 'manual'
const inputMode = ref<InputMode>('url')

interface PackageEstimateItem {
  id: number
  productName: string
  productPrice: number
  weight: number
  source: InputMode
}

interface PackageBreakdown {
  id: number
  items: PackageEstimateItem[]
  subtotalPrice: number
  subtotalWeight: number
  pricing: PricingCalculation | null
  error?: string
}

const cartItems = ref<PackageEstimateItem[]>([])
const cartLoading = ref(false) // Loading state for cart
const packageBreakdown = ref<PackageBreakdown[]>([])

const pricing = computed(() => orderStore.pricing)
const isCategoryC = computed(() => pricing.value?.categoryC ?? false)
const hasValidPrice = computed(() => productPrice.value && productPrice.value > 0)
const cartSubtotalPrice = computed(() => cartItems.value.reduce((acc, item) => acc + item.productPrice, 0))
const cartSubtotalWeight = computed(() => cartItems.value.reduce((acc, item) => acc + item.weight, 0))
const packageBreakdownTotal = computed(() => packageBreakdown.value.reduce((acc, pkg) => acc + (pkg.pricing?.totalPrice || 0), 0))
// Separate valid packages from category C
const validPackages = computed(() => packageBreakdown.value.filter(pkg => !pkg.pricing?.categoryC))
const categoryCPackages = computed(() => packageBreakdown.value.filter(pkg => pkg.pricing?.categoryC))
const validPackagesTotal = computed(() => validPackages.value.reduce((acc, pkg) => acc + (pkg.pricing?.totalPrice || 0), 0))
const hasValidPackages = computed(() => validPackages.value.length > 0)
const hasCategoryC = computed(() => categoryCPackages.value.length > 0)
const showHelperColumn = computed(() => showConfirmation.value)
const showSuggestedColumn = computed(() => packageBreakdown.value.length > 0)
const hasThirdColumn = computed(() => showHelperColumn.value || showSuggestedColumn.value)
const KG_TO_LB = 2.20462
const canConvertWeight = computed(() => weightConverterInput.value !== null && weightConverterInput.value > 0)
const convertedWeightLbs = computed(() => {
  if (!canConvertWeight.value || weightConverterInput.value === null) return null
  const raw = weightConverterUnit.value === 'kg'
    ? weightConverterInput.value * KG_TO_LB
    : weightConverterInput.value
  return Number(raw.toFixed(2))
})
const convertedWeightKg = computed(() => {
  if (!canConvertWeight.value || weightConverterInput.value === null) return null
  const raw = weightConverterUnit.value === 'lb'
    ? weightConverterInput.value / KG_TO_LB
    : weightConverterInput.value
  return Number(raw.toFixed(2))
})

function toFriendlyApiError(err: unknown): string {
  const message = err instanceof Error ? err.message : String(err || '')
  const normalized = message.toLowerCase()
  if (normalized.includes('network error') || normalized.includes('err_connection_refused') || normalized.includes('failed to fetch') || normalized.includes('connection refused')) {
    return 'No se pudo conectar con el servidor. Verifica que el backend esté activo en http://localhost:8080.'
  }
  return message || 'Ocurrió un error inesperado al procesar la solicitud.'
}

function useConvertedWeight() {
  if (convertedWeightLbs.value === null) return
  weight.value = convertedWeightLbs.value
  showWeightGuideModal.value = false
}

async function scrapeAmazonUrl() {
  if (!authStore.isAuthenticated) {
    router.push('/login?redirect=/calculadora')
    return
  }
  if (!amazonUrl.value.trim()) {
    scrapeError.value = 'Ingresa una URL de Amazon'
    return
  }
  scraping.value = true
  scrapeError.value = null
  showConfirmation.value = false
  
  try {
    const response = await amazonService.scrape({ url: amazonUrl.value })
    if (response.success && response.data) {
      const data: ProductInfo = response.data
      productTitle.value = data.title || ''
      productAsin.value = data.asin || ''
      productImage.value = data.image || null
      if (data.price) {
        const priceNum = parseFloat(data.price.replace(/[^0-9.]/g, ''))
        if (!isNaN(priceNum)) productPrice.value = priceNum
      }
      if (data.weight) {
        weight.value = data.weight
        weightFromApi.value = true
      } else {
        weight.value = null
        weightFromApi.value = false
      }
      showConfirmation.value = true
    } else {
      scrapeError.value = response.error || 'No se pudo extraer información del producto'
    }
  } catch (err) {
    scrapeError.value = toFriendlyApiError(err)
  } finally {
    scraping.value = false
  }
}

async function confirmAndCalculate() {
  if (!hasValidPrice.value) return
  calculating.value = true
  showResult.value = false
  try {
    await orderStore.calculatePricing({
      productPrice: productPrice.value!,
      weight: weight.value || undefined,
      shippingOption: shippingOption,
    })
    showResult.value = true
    showConfirmation.value = false
  } finally {
    calculating.value = false
  }
}

async function calculateManual() {
  if (!hasValidPrice.value) {
    scrapeError.value = 'Ingresa un precio válido para calcular.'
    return
  }
  calculating.value = true
  showResult.value = false
  showConfirmation.value = false
  scrapeError.value = null
  try {
    const result = await orderStore.calculatePricing({
      productPrice: productPrice.value!,
      weight: weight.value || undefined,
      shippingOption: shippingOption,
    })
    if (!result) {
      scrapeError.value = toFriendlyApiError(orderStore.error || 'No se pudo calcular la estimación manual')
      return
    }
    showResult.value = true
  } finally {
    calculating.value = false
  }
}

function setInputMode(mode: InputMode) {
  inputMode.value = mode
  scrapeError.value = null
  showConfirmation.value = false
  showResult.value = false
  productAsin.value = ''
  productImage.value = null
}

function clearCurrentProductFields() {
  amazonUrl.value = ''
  productTitle.value = ''
  productAsin.value = ''
  productPrice.value = null
  productImage.value = null
  weight.value = null
}

function addCurrentProductToCart() {
  if (!productPrice.value || productPrice.value <= 0) {
    scrapeError.value = 'Ingresa un precio válido para agregar al carrito.'
    return
  }
  const productWeight = weight.value && weight.value > 0 ? weight.value : 1
  cartItems.value.push({
    id: Date.now() + Math.floor(Math.random() * 1000),
    productName: productTitle.value || 'Producto sin nombre',
    productPrice: productPrice.value,
    weight: productWeight,
    source: inputMode.value,
  })
  scrapeError.value = null
  showResult.value = false
  showConfirmation.value = false
  clearCurrentProductFields()
}

function removeCartItem(id: number) {
  cartItems.value = cartItems.value.filter((item) => item.id !== id)
}

// Calculate paid weight (rounds up to nearest whole lb)
function getPaidWeight(weight: number): number {
  return Math.ceil(weight)
}

// Calculate how much would be paid if adding item to a package
function calculatePaidWeightIfAdded(pkgWeight: number, itemWeight: number, pkgPrice: number, itemPrice: number): number {
  return getPaidWeight(pkgWeight + itemWeight)
}

function splitInto4x4Packages(items: PackageEstimateItem[]): PackageEstimateItem[][] {
  const MAX_PRICE = 400
  const MAX_WEIGHT = 4
  const packages: PackageEstimateItem[][] = []
  
  // Sort items by weight descending (larger items first)
  const sorted = [...items].sort((a, b) => b.weight - a.weight)
  
  for (const item of sorted) {
    // Find all packages that can fit this item
    let bestPkgIndex = -1
    let bestPaidWeight = Infinity
    
    for (let i = 0; i < packages.length; i++) {
      const pkg = packages[i]
      const pkgPrice = pkg.reduce((acc, p) => acc + p.productPrice, 0)
      const pkgWeight = pkg.reduce((acc, p) => acc + p.weight, 0)
      
      // Check if item fits
      if (pkgPrice + item.productPrice <= MAX_PRICE && pkgWeight + item.weight <= MAX_WEIGHT) {
        const paidWeight = calculatePaidWeightIfAdded(pkgWeight, item.weight, pkgPrice, item.productPrice)
        
        // Choose package with lowest paid weight
        if (paidWeight < bestPaidWeight) {
          bestPaidWeight = paidWeight
          bestPkgIndex = i
        } else if (paidWeight === bestPaidWeight) {
          // If tied, prefer package closer to max (8 lbs or $400)
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
    
    // Add to best package or create new one
    if (bestPkgIndex >= 0) {
      packages[bestPkgIndex].push(item)
    } else {
      // Create new package if no existing package can fit
      packages.push([item])
    }
  }
  
  return packages
}

async function estimateCartPackages() {
  if (cartItems.value.length === 0) {
    scrapeError.value = 'Agrega al menos un producto al carrito para estimar paquetes.'
    return
  }
  // Clear previous results first
  packageBreakdown.value = []
  calculating.value = true
  scrapeError.value = null
  await new Promise(resolve => setTimeout(resolve, 50)) // Small delay to ensure clear
  try {
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
          shippingOption: shippingOption,
        })
        results.push({ id: i + 1, items: pkgItems, subtotalPrice, subtotalWeight, pricing: pricingResult })
      } catch (err) {
        const message = toFriendlyApiError(err)
        results.push({ id: i + 1, items: pkgItems, subtotalPrice, subtotalWeight, pricing: null, error: message })
      }
    }
    packageBreakdown.value = results
  } finally {
    calculating.value = false
  }
}

function cancelConfirmation() {
  showConfirmation.value = false
  productTitle.value = ''
  productAsin.value = ''
  productPrice.value = null
  productImage.value = null
  weight.value = null
  showResult.value = false
}

async function createOrder() {
  if (!hasValidPrice.value || isCategoryC.value) return
  const order = await orderStore.createOrder({
    productName: productTitle.value || undefined,
    productAsin: productAsin.value || undefined,
    productPrice: productPrice.value!,
    weight: weight.value || undefined,
    shippingOption: shippingOption,
    amazonUrl: amazonUrl.value || undefined,
  })
  if (order) {
    router.push(`/ordenes/${order.id}`)
  }
}

function formatCurrency(value: number | undefined | null): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

watch([productPrice, weight], () => {
  if (hasValidPrice.value && showResult.value) {
    orderStore.calculatePricing({
      productPrice: productPrice.value!,
      weight: weight.value || undefined,
      shippingOption: shippingOption,
    })
  }
})

// Load cart from backend on mount
async function loadCartFromBackend() {
  // Check if user is properly authenticated (has token)
  if (!authStore.token || !authStore.isAuthenticated) return
  
  cartLoading.value = true
  try {
    const items = await cartService.getCart()
    cartItems.value = items.map((item: any) => ({
      id: item.id || Date.now() + Math.random(),
      productName: item.productName,
      productPrice: item.productPrice,
      weight: item.weight,
      source: item.source as InputMode
    }))
    console.log('Cart loaded from backend:', cartItems.value.length, 'items')
  } catch (err: any) {
    // Ignore 401 errors (not authenticated)
    if (err.response?.status === 401) {
      console.log('User not authenticated, skipping cart load')
      return
    }
    console.error('Failed to load cart:', err)
  } finally {
    cartLoading.value = false
  }
}

// Save cart to backend when it changes
let isInitialLoad = true
watch(cartItems, async (newItems) => {
  // Skip first trigger (initial load from backend)
  if (isInitialLoad) {
    isInitialLoad = false
    return
  }
  
  // Only save if authenticated and has valid token
  if (!authStore.token || !authStore.isAuthenticated) return
  
  try {
    const itemsToSave = newItems.map(item => ({
      productName: item.productName,
      productPrice: item.productPrice,
      weight: item.weight,
      source: item.source
    }))
    await cartService.saveAllItems(itemsToSave)
    console.log('Cart saved to backend:', newItems.length, 'items')
  } catch (err) {
    console.error('Failed to save cart:', err)
  }
}, { deep: true })

// Load cart on component mount
onMounted(() => {
  if (authStore.isAuthenticated) {
    loadCartFromBackend()
  }
})
</script>

<template>
  <div class="min-h-[calc(100dvh-64px)] pt-[64px] pb-16 relative overflow-hidden">
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />
    <div class="relative max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 min-h-[calc(100dvh-128px)] flex items-center">
      <div class="w-full calculator-shell">
        <FadeIn direction="up">
          <div class="mb-6 rounded-2xl border border-teal-500/25 bg-zinc-900/70 px-5 py-4">
            <p class="text-sm sm:text-base text-zinc-300 leading-relaxed">
              Ingresa la URL de Amazon o completa los datos manualmente (precio y peso) para calcular el valor estimado de tu paquete.
              Este análisis te ayuda a validar si puede entrar en la categoría 4x4 o si aplica categoría C según las reglas de Aduana.
              <a href="https://www.aduana.gob.ec/servicio-al-ciudadano/envios-courier-postal/" target="_blank" rel="noopener noreferrer" class="text-teal-400 hover:text-teal-300 underline underline-offset-4 ml-1">Más información oficial</a>
            </p>
          </div>
        </FadeIn>
        <div :class="['grid grid-cols-1 lg:grid-cols-2 gap-6 xl:gap-8 items-stretch', hasThirdColumn ? 'xl:grid-cols-3' : '']">
          <FadeIn direction="up" :delay="100">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20 h-full overflow-hidden flex flex-col">
              <div v-if="!showConfirmation && !showResult" class="space-y-6 flex-1 flex flex-col justify-center w-full max-w-[560px] mx-auto">
                <div class="flex items-center justify-between gap-4">
                  <h2 class="panel-title text-white">Datos del producto</h2>
                  <div class="inline-flex items-center p-1 rounded-xl bg-zinc-800/70 border border-zinc-700/50">
                    <button @click="setInputMode('url')" :class="['px-3 py-1.5 text-sm rounded-lg transition-all', inputMode === 'url' ? 'bg-teal-500 text-zinc-950 font-semibold' : 'text-zinc-300 hover:text-white']">URL</button>
                    <button @click="setInputMode('manual')" :class="['px-3 py-1.5 text-sm rounded-lg transition-all', inputMode === 'manual' ? 'bg-teal-500 text-zinc-950 font-semibold' : 'text-zinc-300 hover:text-white']">Manual</button>
                  </div>
                </div>
                <div :class="['mode-height-wrapper', inputMode === 'manual' ? 'manual' : 'url']">
                  <Transition name="mode-expand" mode="out-in">
                    <div v-if="inputMode === 'url'" key="url" class="mode-content-block">
                      <label class="block text-sm font-medium text-zinc-300 mb-2">URL del Producto en Amazon</label>
                      <div class="relative">
                        <input v-model="amazonUrl" type="url" placeholder="https://www.amazon.com/dp/B0XXXXX" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 pr-12 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all" @keyup.enter="scrapeAmazonUrl" />
                        <div class="absolute right-3 top-1/2 -translate-y-1/2">
                          <svg class="w-5 h-5 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
                        </div>
                      </div>
                      <p v-if="scrapeError" class="text-red-400 text-sm mt-2">{{ scrapeError }}</p>
                    </div>
                    <div v-else key="manual" class="space-y-4 mode-content-block">
                      <div>
                        <label class="block text-sm font-medium text-zinc-300 mb-2">Nombre del producto (opcional)</label>
                        <input v-model="productTitle" type="text" placeholder="Ej: Audífonos Sony WH-1000XM5" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all" />
                      </div>
                      <div>
                        <label class="block text-sm font-medium text-zinc-300 mb-2">Precio del producto (USD)</label>
                        <input v-model.number="productPrice" type="number" step="0.01" min="0" placeholder="Ej: 129.99" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all" />
                      </div>
                      <div>
                        <label class="block text-sm font-medium text-zinc-300 mb-2">Peso (libras)</label>
                        <input v-model.number="weight" type="number" step="0.1" min="0" placeholder="Ej: 2.5" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all" />
                        <p class="text-xs text-amber-400/80 mt-2">¿No sabes el peso? <button @click="showWeightGuideModal = true" class="underline hover:text-amber-300">Cómo conseguir el peso</button></p>
                      </div>
                    </div>
                  </Transition>
                </div>
                <div class="mt-auto pt-2 border-t border-zinc-700/50">
                  <div class="flex items-center justify-between text-sm mb-4">
                    <span class="text-zinc-500">Estado extracción</span>
                    <span class="text-zinc-500">{{ authStore.isAuthenticated ? 'Habilitado' : 'Bloqueado' }}</span>
                  </div>
                  <button v-if="inputMode === 'url' && authStore.isAuthenticated" @click="scrapeAmazonUrl" :disabled="scraping || calculating" class="w-full py-3 flex items-center justify-center gap-2 btn-primary">
                    <svg v-if="scraping || calculating" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" /><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" /></svg>
                    <span v-if="scraping">Extrayendo información...</span>
                    <span v-else-if="calculating">Calculando...</span>
                    <span v-else>Extraer datos</span>
                  </button>
                  <button v-else-if="inputMode === 'url' && !authStore.isAuthenticated" type="button" disabled title="Inicia sesión para poder extraer datos" class="w-full py-3 flex items-center justify-center gap-2 btn-primary opacity-60 cursor-not-allowed">Extraer datos</button>
                  <button v-else @click="addCurrentProductToCart" :disabled="scraping || calculating" class="w-full py-3 flex items-center justify-center gap-2 btn-primary">
                    <span v-if="calculating">Calculando...</span>
                    <span v-else>Agregar producto al carrito</span>
                  </button>
                </div>
                <p v-if="inputMode === 'manual' && scrapeError" class="text-red-400 text-sm mt-2">{{ scrapeError }}</p>
              </div>
              <div v-else-if="showConfirmation" class="space-y-6 w-full max-w-[560px] mx-auto my-auto">
                <h2 class="text-xl sm:text-2xl font-bold mb-6 text-white">Confirmar Datos del Producto</h2>
                <div class="bg-zinc-800/50 rounded-xl p-6 border border-zinc-700/30">
                  <div v-if="productImage" class="flex justify-center mb-4">
                    <img :src="productImage" :alt="productTitle" class="max-w-[200px] max-h-[200px] object-contain rounded-lg bg-white" />
                  </div>
                  <div v-else class="flex justify-center mb-4">
                    <div class="w-[200px] h-[200px] bg-zinc-700/50 rounded-lg flex items-center justify-center">
                      <svg class="w-16 h-16 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>
                    </div>
                  </div>
                  <h3 class="text-lg font-semibold text-white text-center mb-2 line-clamp-2">{{ productTitle }}</h3>
                  <p class="text-xl text-teal-400 font-bold text-center mb-6">Precio: {{ formatCurrency(productPrice ?? 0) }}</p>
                  <div class="mb-6">
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Peso (libras)</label>
                    <div v-if="weightFromApi && weight" class="relative">
                      <input :value="weight" type="number" step="0.1" disabled class="w-full bg-zinc-900/60 border border-teal-500/50 rounded-xl py-3.5 px-4 pr-12 text-teal-400 font-medium cursor-not-allowed opacity-80" />
                      <span class="absolute right-4 top-1/2 -translate-y-1/2 text-teal-500">lbs</span>
                    </div>
                    <div v-else class="space-y-2">
                      <div class="relative">
                        <input v-model.number="weight" type="number" step="0.1" min="0" placeholder="Ingresa el peso en lbs" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 pr-12 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all" />
                        <span class="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-500">lbs</span>
                      </div>
                      <p class="text-xs text-amber-400/80">Disculpa, no se pudo extraer el peso de este producto. <button @click="showWeightGuideModal = true" class="underline hover:text-amber-300">¿Cómo consigo el peso?</button></p>
                    </div>
                    <p class="text-xs text-zinc-500 mt-2">{{ weight ? `Peso: ${weight} lbs` : 'Si no conoces el peso, lo estimamos en 1 lb' }}</p>
                  </div>
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button @click="cancelConfirmation" class="py-3 px-4 rounded-xl border border-zinc-600 text-zinc-300 hover:bg-zinc-700/50 transition-all font-medium">Volver</button>
                    <button type="button" @click="addCurrentProductToCart" class="py-3 px-4 rounded-xl border border-zinc-600 text-white hover:bg-zinc-700/50 transition-all font-medium">Agregar al carrito</button>
                  </div>
                </div>
              </div>
              <div v-if="showResult && pricing && !isCategoryC" class="mt-6">
                <h2 class="text-xl sm:text-2xl font-bold mb-6 text-white">Desglose de Precios</h2>
                <div class="space-y-4">
                  <div class="flex justify-between text-base sm:text-lg"><span class="text-zinc-400">Producto</span><span class="text-white">{{ formatCurrency(pricing.productPrice) }}</span></div>
                  <div class="flex justify-between text-base sm:text-lg"><span class="text-zinc-400">Impuestos</span><span class="text-white">{{ formatCurrency(pricing.taxes) }}</span></div>
                  <div class="flex justify-between text-base sm:text-lg"><span class="text-zinc-400">Handling</span><span class="text-white">{{ formatCurrency(pricing.handling) }}</span></div>
                  <div class="flex justify-between text-base sm:text-lg"><span class="text-zinc-400">Envío ({{ weight ? `${weight} lbs` : 'estimado' }})</span><span class="text-white">{{ formatCurrency(pricing.shippingCost) }}</span></div>
                  <div class="flex justify-between text-base sm:text-lg"><span class="text-zinc-400">Aduana (fijo)</span><span class="text-white">{{ formatCurrency(pricing.customs) }}</span></div>
                </div>
                <div class="border-t border-white/10 pt-6 mt-6">
                  <div class="flex justify-between items-center"><span class="text-lg font-bold text-white">Total</span><span class="text-3xl sm:text-4xl font-bold text-teal-400">{{ formatCurrency(pricing.totalPrice) }}</span></div>
                </div>
                <p v-if="pricing.categoryC" class="mt-4 p-3 bg-yellow-500/20 border border-yellow-500/50 rounded-xl text-yellow-300 text-sm">Este pedido aplica categoría C porque excede los límites de 4kg o $400 FOB. El valor final puede variar.</p>
                <button @click="cancelConfirmation" class="w-full text-center text-sm text-zinc-400 hover:text-teal-400 transition-colors py-2 mt-4">Calcular otro producto</button>
              </div>
            </div>
          </FadeIn>
          <FadeIn direction="up" :delay="240" class="h-full">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20 h-full lg:h-[calc(100dvh-230px)] flex flex-col overflow-hidden">
              <div class="flex items-center justify-between mb-4">
                <h3 class="panel-title text-white">Carrito de productos</h3>
                <span v-if="!cartLoading" class="text-xs text-zinc-400">{{ cartItems.length }} item(s)</span>
              </div>
              
              <!-- Loading state -->
              <div v-if="cartLoading" class="flex items-center justify-center py-8">
                <div class="flex flex-col items-center gap-3">
                  <svg class="w-8 h-8 animate-spin text-teal-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  <span class="text-sm text-zinc-500">Cargando carrito...</span>
                </div>
              </div>
              
              <div class="flex-1 min-h-0 flex flex-col">
                <!-- Empty cart (after loading) -->
                <div v-if="!cartLoading && cartItems.length === 0" class="text-sm text-zinc-500 mb-4">
                  Aún no agregas productos. Agrega varios y luego calcula la agrupación automática de paquetes 4x4.
                </div>

                <!-- Cart items -->
                <div v-else-if="!cartLoading" class="space-y-2 flex-1 min-h-0 overflow-y-auto pr-1 mb-4">
                  <div v-for="item in cartItems" :key="item.id" class="rounded-lg border border-zinc-700/40 bg-zinc-900/70 p-3">
                    <div class="flex items-start justify-between gap-2">
                      <div>
                        <p class="text-sm text-white font-medium line-clamp-2 break-words">{{ item.productName }}</p>
                        <p class="text-xs text-zinc-400 mt-1">{{ formatCurrency(item.productPrice) }} · {{ item.weight }} lbs · {{ item.source === 'url' ? 'URL' : 'Manual' }}</p>
                      </div>
                      <button type="button" @click="removeCartItem(item.id)" class="text-zinc-500 hover:text-white text-sm" aria-label="Quitar producto">X</button>
                    </div>
                  </div>
                </div>

                <div class="pt-2 border-t border-zinc-700/50 flex items-center justify-between text-sm mb-4 mt-auto">
                  <span class="text-zinc-400">Subtotal carrito</span>
                  <span class="text-white">{{ formatCurrency(cartSubtotalPrice) }} · {{ cartSubtotalWeight.toFixed(2) }} lbs</span>
                </div>
              </div>
              <button type="button" @click="estimateCartPackages" :disabled="calculating || cartItems.length === 0" :class="['w-full py-3 flex items-center justify-center gap-2 btn-primary disabled:cursor-not-allowed', cartItems.length === 0 ? 'opacity-60' : '']">
                <svg v-if="calculating" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" /><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" /></svg>
                <span v-if="calculating">Calculando paquetes...</span>
                <span v-else>Calcular paquetes automáticos (4x4)</span>
              </button>
            </div>
          </FadeIn>
          <FadeIn v-if="showHelperColumn || showSuggestedColumn" direction="up" :delay="160" class="h-full">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20 h-full lg:h-[calc(100dvh-230px)] flex flex-col overflow-hidden">
              <template v-if="showHelperColumn">
                <h3 class="panel-title text-white mb-4">Completa los campos</h3>
                <div class="rounded-xl border border-zinc-700/50 bg-zinc-800/50 p-4 text-sm text-zinc-300">
                  El tipo de entrega ahora se selecciona en el checkout.
                </div>
                <div class="mt-auto text-sm text-zinc-400">Revisa los datos extraídos y agrega el producto al carrito.</div>
              </template>
              <template v-else>
                <div class="flex items-center justify-between mb-4">
                  <h3 class="panel-title text-white">Paquetes sugeridos</h3>
                  <button type="button" @click="packageBreakdown = []" class="text-sm text-zinc-400 hover:text-white transition-colors">Limpiar</button>
                </div>
                <div class="flex-1 min-h-0 flex flex-col">
                  <div class="space-y-3 flex-1 min-h-0 overflow-y-auto pr-1">
                    <div v-for="pkg in packageBreakdown" :key="pkg.id" :class="['rounded-xl border p-4', pkg.pricing?.categoryC ? 'border-amber-500/50 bg-amber-500/5' : 'border-zinc-700/50 bg-zinc-800/50']">
                      <div v-if="pkg.error" class="mb-3 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-300">{{ pkg.error }}</div>
                      <p class="text-white font-medium">Paquete {{ pkg.id }}</p>
                      <p class="text-xs text-zinc-400 mt-1">{{ pkg.items.length }} producto(s) · {{ formatCurrency(pkg.subtotalPrice) }} · {{ pkg.subtotalWeight.toFixed(2) }} lbs</p>
                      <div class="text-xs text-zinc-500 mt-2 line-clamp-2">{{ pkg.items.map((it) => it.productName).join(' · ') }}</div>
                      <div class="mt-3 flex justify-between text-sm" v-if="pkg.pricing"><span class="text-zinc-400">Total</span><span class="text-teal-300 font-semibold">{{ formatCurrency(pkg.pricing.totalPrice) }}</span></div>
                      <details v-if="pkg.pricing" class="mt-3 rounded-lg border border-zinc-700/40 bg-zinc-900/60 p-3">
                        <summary class="cursor-pointer text-sm text-zinc-300 hover:text-white transition-colors">Detalles</summary>
                        <div class="mt-3 space-y-2 text-xs">
                          <div class="flex items-center justify-between"><span class="text-zinc-500">Producto</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing.productPrice) }}</span></div>
                          <div class="flex items-center justify-between"><span class="text-zinc-500">Impuestos</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing.taxes) }}</span></div>
                          <div class="flex items-center justify-between"><span class="text-zinc-500">Handling</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing.handling) }}</span></div>
                          <div class="flex items-center justify-between"><span class="text-zinc-500">Envío</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing.shippingCost) }}</span></div>
                          <div class="flex items-center justify-between"><span class="text-zinc-500">Aduana</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing.customs) }}</span></div>
                          <div class="pt-2 border-t border-zinc-700/50 flex items-center justify-between"><span class="text-zinc-300">Total final</span><span class="text-teal-300 font-semibold">{{ formatCurrency(pkg.pricing.totalPrice) }}</span></div>
                        </div>
                      </details>
                      <div class="mt-2 text-xs text-amber-400" v-if="pkg.pricing?.categoryC">Categoría C - Queda en carrito</div>
                    </div>
                  </div>
                  <div class="mt-4 pt-4 border-t border-white/10">
                    <div class="flex items-center justify-between mb-3">
                      <span class="text-zinc-300">Total paquetes válidos</span>
                      <span class="text-2xl font-bold text-teal-400">{{ formatCurrency(validPackagesTotal) }}</span>
                    </div>
                    
                    <!-- Botón Comprar (solo si hay paquetes válidos) -->
                    <button 
                      v-if="hasValidPackages" 
                      @click="router.push('/checkout')" 
                      class="w-full py-3 flex items-center justify-center gap-2 btn-primary mb-2"
                    >
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" /></svg>
                      Comprar
                    </button>
                    
                    <!-- Categoría C: quedan en el carrito + WhatsApp -->
                    <div v-if="hasCategoryC" class="space-y-2">
                      <p class="text-xs text-amber-400">Categoría C: estos productos quedan en el carrito</p>
                      <a 
                        href="https://wa.me/593998050600?text=Hola%2C%20me%20interesan%20los%20siguientes%20productos%20que%20aplican%20categor%C3%ADa%20C%3A%0A%0A" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        class="w-full py-3 flex items-center justify-center gap-2 bg-green-600 hover:bg-green-700 text-white rounded-xl font-medium transition-colors"
                      >
                        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
                        Cotizar por WhatsApp
                      </a>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </FadeIn>
        </div>
      </div>
    </div>
  </div>
  <Teleport to="body">
    <div v-if="showWeightGuideModal" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showWeightGuideModal = false"></div>
      <div class="relative bg-zinc-900 border border-zinc-700 rounded-2xl p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl">
        <button @click="showWeightGuideModal = false" class="absolute top-4 right-4 text-zinc-400 hover:text-white">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
        <h3 class="text-xl font-bold text-white mb-4">Cómo conseguir el peso</h3>
        <div class="space-y-3 text-zinc-300 text-sm">
          <div class="bg-zinc-800/50 rounded-xl p-3 border border-zinc-700/50">
            <h4 class="font-semibold text-teal-400 mb-1">PC</h4>
            <p class="text-zinc-400">Amazon en ingles -> producto -> "Customer Reviews" -> busca "weight" o "pounds"</p>
          </div>
          <div class="bg-zinc-800/50 rounded-xl p-3 border border-zinc-700/50">
            <h4 class="font-semibold text-teal-400 mb-1">Movil</h4>
            <p class="text-zinc-400">App Amazon -> producto -> "All reviews" -> busca "weight" o "lbs"</p>
          </div>
          <div class="bg-zinc-800/50 rounded-xl p-4 border border-zinc-700/50 space-y-3">
            <h4 class="font-semibold text-teal-400">Conversor de peso</h4>
            <div class="grid grid-cols-2 gap-2">
              <button
                type="button"
                @click="weightConverterUnit = 'kg'"
                :class="[
                  'py-2 rounded-lg border text-sm transition-all',
                  weightConverterUnit === 'kg' ? 'bg-teal-500 text-zinc-950 border-teal-400 font-semibold' : 'border-zinc-600 text-zinc-300 hover:text-white'
                ]"
              >
                Ingreso en kg
              </button>
              <button
                type="button"
                @click="weightConverterUnit = 'lb'"
                :class="[
                  'py-2 rounded-lg border text-sm transition-all',
                  weightConverterUnit === 'lb' ? 'bg-teal-500 text-zinc-950 border-teal-400 font-semibold' : 'border-zinc-600 text-zinc-300 hover:text-white'
                ]"
              >
                Ingreso en lb
              </button>
            </div>
            <input
              v-model.number="weightConverterInput"
              type="number"
              step="0.01"
              min="0"
              :placeholder="weightConverterUnit === 'kg' ? 'Ej: 1.25 kg' : 'Ej: 2.75 lb'"
              class="w-full bg-zinc-900/70 border border-zinc-700/60 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 transition-all"
            />
            <div class="rounded-xl border border-zinc-700/60 bg-zinc-900/60 p-3 text-xs">
              <p v-if="weightConverterUnit === 'kg'" class="text-zinc-400">
                En libras (lb):
                <span class="text-white font-semibold">{{ convertedWeightLbs !== null ? `${convertedWeightLbs} lb` : '--' }}</span>
              </p>
              <p v-else class="text-zinc-400">
                En kilogramos (kg):
                <span class="text-white font-semibold">{{ convertedWeightKg !== null ? `${convertedWeightKg} kg` : '--' }}</span>
              </p>
            </div>
            <button
              type="button"
              @click="useConvertedWeight"
              :disabled="convertedWeightLbs === null"
              class="w-full py-2.5 rounded-xl bg-teal-600 text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Usar este valor en el peso del producto
            </button>
          </div>
        </div>
        <button @click="showWeightGuideModal = false" class="w-full mt-4 py-3 rounded-xl bg-teal-600 text-white font-medium">Entendido</button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.calculator-shell { animation: calculator-enter 0.45s ease-out; }
.calculator-panel { min-height: 520px; }
.panel-title { font-size: 1rem; line-height: 1.4; font-weight: 600; }
.mode-height-wrapper { overflow: hidden; transition: max-height 260ms ease; }
.mode-height-wrapper.url { max-height: 140px; }
.mode-height-wrapper.manual { max-height: 460px; }
.mode-content-block { transform-origin: top; }
.mode-expand-enter-active, .mode-expand-leave-active { transition: opacity 220ms ease, transform 220ms ease; }
.mode-expand-enter-from, .mode-expand-leave-to { opacity: 0; transform: translateY(-6px) scaleY(0.98); }
@media (max-width: 640px) { .panel-title { font-size: 1rem; } .mode-height-wrapper.url, .mode-height-wrapper.manual { max-height: none; } }
@media (max-width: 1280px) { .calculator-panel { min-height: 460px; } }
@media (max-width: 1024px) { .calculator-panel { min-height: auto; } }
@keyframes calculator-enter { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>



