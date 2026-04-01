<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import FadeIn from '@/components/FadeIn.vue'
import { cartService } from '@/services/cart'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const amazonUrl = ref('')
const productTitle = ref('')
const productPrice = ref<number | null>(null)
const weight = ref<number | null>(null)
const showWeightGuideModal = ref(false)
const calculating = ref(false)
const showResult = ref(false)
const scraping = ref(false)
const scrapeError = ref<string | null>(null)

type InputMode = 'url' | 'manual'
const inputMode = ref<InputMode>('url')

// Custom shipping rates for SuperAdmin
const SHIPPING_RATES: Record<number, number> = {
  1: 11.00, 2: 16.48, 3: 20.99, 4: 28.28,
  5: 37.61, 6: 42.14, 7: 46.13, 8: 50.12, 9: 54.11
}
const CUSTOMS_FEE = 21

// Pricing result
interface PricingResult {
  productPrice: number
  shippingCost: number
  customs: number
  totalPrice: number
  categoryC: boolean
}

const pricingResult = ref<PricingResult | null>(null)

// Cart for package calculation
interface CartItem {
  id: number
  productName: string
  productPrice: number
  weight: number
  source: InputMode
}

interface PackageBreakdown {
  id: number
  items: CartItem[]
  subtotalPrice: number
  subtotalWeight: number
  pricing: PricingResult | null
  error?: string
}

const cartItems = ref<CartItem[]>([])
const cartLoading = ref(false)
const packageBreakdown = ref<PackageBreakdown[]>([])

const hasValidPrice = computed(() => productPrice.value && productPrice.value > 0)
const cartSubtotalPrice = computed(() => cartItems.value.reduce((acc, item) => acc + item.productPrice, 0))
const cartSubtotalWeight = computed(() => cartItems.value.reduce((acc, item) => acc + item.weight, 0))
const packageBreakdownTotal = computed(() => packageBreakdown.value.reduce((acc, pkg) => acc + (pkg.pricing?.totalPrice || 0), 0))

function getShippingCost(w: number): number {
  const weightKey = Math.min(Math.ceil(w || 1), 9) as keyof typeof SHIPPING_RATES
  return SHIPPING_RATES[weightKey]
}

function calculatePricingForItem(price: number, w: number): PricingResult {
  const shippingCost = getShippingCost(w)
  const customs = CUSTOMS_FEE
  const totalPrice = price + shippingCost + customs
  const categoryC = price > 400 || w > 4
  
  return {
    productPrice: price,
    shippingCost: Math.round(shippingCost * 100) / 100,
    customs,
    totalPrice: Math.round(totalPrice * 100) / 100,
    categoryC
  }
}

function calculateSingleProductPricing() {
  if (!hasValidPrice.value) return
  
  const price = productPrice.value || 0
  const w = weight.value || 1
  
  pricingResult.value = calculatePricingForItem(price, w)
  showResult.value = true
}

// Calculate paid weight (rounds up to nearest whole lb)
function getPaidWeight(weight: number): number {
  return Math.ceil(weight)
}

// Calculate how much would be paid if adding item to a package
function calculatePaidWeightIfAdded(pkgWeight: number, itemWeight: number, pkgPrice: number, itemPrice: number): number {
  return getPaidWeight(pkgWeight + itemWeight)
}

function splitInto4x4Packages(items: CartItem[]): CartItem[][] {
  const MAX_PRICE = 400
  const MAX_WEIGHT = 4
  const packages: CartItem[][] = []
  
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

function estimateCartPackages() {
  if (cartItems.value.length === 0) {
    scrapeError.value = 'Agrega al menos un producto al carrito'
    return
  }
  // Clear previous results first
  packageBreakdown.value = []
  calculating.value = true
  scrapeError.value = null
  try {
    const splitPackages = splitInto4x4Packages(cartItems.value)
    const results: PackageBreakdown[] = []
    
    for (let i = 0; i < splitPackages.length; i++) {
      const pkgItems = splitPackages[i]
      const subtotalPrice = pkgItems.reduce((acc, item) => acc + item.productPrice, 0)
      const subtotalWeight = pkgItems.reduce((acc, item) => acc + item.weight, 0)
      
      const pricing = calculatePricingForItem(subtotalPrice, subtotalWeight)
      results.push({
        id: i + 1,
        items: pkgItems,
        subtotalPrice,
        subtotalWeight,
        pricing
      })
    }
    
    packageBreakdown.value = results
  } finally {
    calculating.value = false
  }
}

function addCurrentProductToCart() {
  if (!productPrice.value || productPrice.value <= 0) {
    scrapeError.value = 'Ingresa un precio válido'
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
  productTitle.value = ''
  productPrice.value = null
  weight.value = null
}

function removeCartItem(id: number) {
  cartItems.value = cartItems.value.filter((item) => item.id !== id)
}

function setInputMode(mode: InputMode) {
  inputMode.value = mode
  scrapeError.value = null
  showResult.value = false
}

function cancelConfirmation() {
  productTitle.value = ''
  productPrice.value = null
  weight.value = null
  showResult.value = false
  pricingResult.value = null
}

function clearCart() {
  cartItems.value = []
  packageBreakdown.value = []
}

function formatCurrency(value: number | undefined | null): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function getShippingLabel(weight: number): string {
  const w = Math.ceil(weight || 1)
  return `${Math.min(w, 9)} lbs`
}

// Load cart from backend on mount
async function loadCartFromBackend() {
  if (!authStore.isAuthenticated) return
  
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
  } catch (err) {
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
  
  if (!authStore.isAuthenticated) return
  
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
      <div class="w-full">
        <FadeIn direction="up">
          <div class="mb-6 rounded-2xl border border-teal-500/25 bg-zinc-900/70 px-5 py-4">
            <p class="text-sm sm:text-base text-zinc-300 leading-relaxed">
              Calculadora SuperAdmin - Sin impuestos, sin handling, tarifas de envío especiales
            </p>
          </div>
        </FadeIn>
        
        <div :class="['grid grid-cols-1 lg:grid-cols-2 gap-6 xl:gap-8']">
          <!-- Input Form -->
          <FadeIn direction="up" :delay="100">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20 h-full overflow-hidden flex flex-col">
              <div v-if="!showResult" class="space-y-6 flex-1 flex flex-col justify-center w-full max-w-[560px] mx-auto">
                <div class="flex items-center justify-between gap-4">
                  <h2 class="panel-title text-white">Datos del producto</h2>
                  <div class="inline-flex items-center p-1 rounded-xl bg-zinc-800/70 border border-zinc-700/50">
                    <button @click="setInputMode('url')" :class="['px-3 py-1.5 text-sm rounded-lg transition-all', inputMode === 'url' ? 'bg-teal-500 text-zinc-950 font-semibold' : 'text-zinc-300 hover:text-white']">URL</button>
                    <button @click="setInputMode('manual')" :class="['px-3 py-1.5 text-sm rounded-lg transition-all', inputMode === 'manual' ? 'bg-teal-500 text-zinc-950 font-semibold' : 'text-zinc-300 hover:text-white']">Manual</button>
                  </div>
                </div>
                
                <div v-if="inputMode === 'url'" class="space-y-4">
                  <label class="block text-sm font-medium text-zinc-300 mb-2">URL del Producto en Amazon</label>
                  <div class="relative">
                    <input v-model="amazonUrl" type="url" placeholder="https://www.amazon.com/dp/B0XXXXX" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500" />
                  </div>
                  <p class="text-xs text-amber-400">Funcionalidad de extracción en desarrollo</p>
                </div>
                
                <div v-else class="space-y-4">
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Nombre del producto</label>
                    <input v-model="productTitle" type="text" placeholder="Ej: Audífonos Sony" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Precio (USD)</label>
                    <input v-model.number="productPrice" type="number" step="0.01" min="0" placeholder="Ej: 129.99" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Peso (libras)</label>
                    <input v-model.number="weight" type="number" step="0.1" min="0" placeholder="Ej: 2.5" class="w-full bg-zinc-800/80 border border-zinc-700/50 rounded-xl py-3.5 px-4 text-white placeholder-zinc-600 focus:border-teal-500" />
                  </div>
                </div>
                
                <div class="flex gap-3">
                  <button @click="calculateSingleProductPricing" :disabled="!hasValidPrice" :class="['flex-1 py-3 rounded-xl font-medium transition-all', hasValidPrice ? 'bg-teal-600 hover:bg-teal-500 text-white' : 'bg-zinc-700 text-zinc-500 cursor-not-allowed']">
                    Calcular
                  </button>
                  <button @click="addCurrentProductToCart" :disabled="!hasValidPrice" :class="['flex-1 py-3 rounded-xl font-medium transition-all', hasValidPrice ? 'bg-zinc-700 hover:bg-zinc-600 text-white' : 'bg-zinc-700 text-zinc-500 cursor-not-allowed']">
                    Agregar al carrito
                  </button>
                </div>
              </div>
              
              <!-- Result -->
              <div v-else class="space-y-6">
                <h2 class="text-xl sm:text-2xl font-bold text-white">Desglose de Precios</h2>
                <div class="space-y-3 text-sm sm:text-base">
                  <div class="flex justify-between"><span class="text-zinc-400">Producto</span><span class="text-white">{{ formatCurrency(pricingResult?.productPrice) }}</span></div>
                  <div class="flex justify-between"><span class="text-zinc-400">Envío ({{ getShippingLabel(weight || 0) }})</span><span class="text-white">{{ formatCurrency(pricingResult?.shippingCost) }}</span></div>
                  <div class="flex justify-between"><span class="text-zinc-400">Aduana (fijo)</span><span class="text-white">{{ formatCurrency(pricingResult?.customs) }}</span></div>
                </div>
                <div class="border-t border-white/10 pt-4">
                  <div class="flex justify-between items-center"><span class="text-lg font-bold text-white">Total</span><span class="text-3xl sm:text-4xl font-bold text-teal-400">{{ formatCurrency(pricingResult?.totalPrice) }}</span></div>
                </div>
                <p v-if="pricingResult?.categoryC" class="p-3 bg-yellow-500/20 border border-yellow-500/50 rounded-xl text-yellow-300 text-sm">
                  Este pedido aplica categoría C
                </p>
                <button @click="cancelConfirmation" class="w-full text-center text-sm text-zinc-400 hover:text-teal-400 py-2">
                  Calcular otro producto
                </button>
              </div>
            </div>
          </FadeIn>
          
          <!-- Cart -->
          <FadeIn direction="up" :delay="240" class="h-full">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20 h-full flex flex-col overflow-hidden">
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
              
              <!-- Empty cart (after loading) -->
              <div v-else-if="cartItems.length === 0" class="text-sm text-zinc-500 mb-4">
                Agrega productos y calcula paquetes automáticos 4x4
              </div>
              
              <!-- Cart items -->
              <div v-else class="space-y-2 max-h-64 overflow-y-auto pr-1 mb-4">
                <div v-for="item in cartItems" :key="item.id" class="rounded-lg border border-zinc-700/40 bg-zinc-900/70 p-3">
                  <div class="flex items-start justify-between gap-2">
                    <div>
                      <p class="text-sm text-white font-medium line-clamp-2">{{ item.productName }}</p>
                      <p class="text-xs text-zinc-400 mt-1">{{ formatCurrency(item.productPrice) }} · {{ item.weight }} lbs</p>
                    </div>
                    <button @click="removeCartItem(item.id)" class="text-zinc-500 hover:text-white text-sm">X</button>
                  </div>
                </div>
              </div>
              
              <div class="pt-2 border-t border-zinc-700/50 flex items-center justify-between text-sm mb-4">
                <span class="text-zinc-400">Subtotal</span>
                <span class="text-white">{{ formatCurrency(cartSubtotalPrice) }} · {{ cartSubtotalWeight.toFixed(2) }} lbs</span>
              </div>
              
              <button @click="estimateCartPackages" :disabled="calculating || cartItems.length === 0" :class="['w-full py-3 flex items-center justify-center gap-2 rounded-xl font-medium transition-all', cartItems.length === 0 ? 'bg-zinc-700 text-zinc-500 cursor-not-allowed' : 'bg-teal-600 hover:bg-teal-500 text-white']">
                <svg v-if="calculating" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" /><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
                <span v-if="calculating">Calculando...</span>
                <span v-else>Calcular paquetes 4x4</span>
              </button>
            </div>
          </FadeIn>
          
          <!-- Package Results -->
          <FadeIn v-if="packageBreakdown.length > 0" direction="up" :delay="160" class="lg:col-span-2">
            <div class="calculator-panel glass rounded-2xl p-6 sm:p-8 border border-white/10 bg-zinc-900/75 shadow-2xl shadow-black/20">
              <div class="flex items-center justify-between mb-4">
                <h3 class="panel-title text-white">Paquetes sugeridos (4x4)</h3>
                <button @click="clearCart" class="text-sm text-zinc-400 hover:text-white">Limpiar</button>
              </div>
              
              <div class="space-y-3">
                <div v-for="pkg in packageBreakdown" :key="pkg.id" class="rounded-xl border border-zinc-700/50 bg-zinc-800/50 p-4">
                  <p class="text-white font-medium">Paquete {{ pkg.id }}</p>
                  <p class="text-xs text-zinc-400 mt-1">{{ pkg.items.length }} producto(s) · {{ formatCurrency(pkg.subtotalPrice) }} · {{ pkg.subtotalWeight.toFixed(2) }} lbs</p>
                  <div class="mt-3 flex justify-between text-sm">
                    <span class="text-zinc-400">Total</span>
                    <span class="text-teal-300 font-semibold">{{ formatCurrency(pkg.pricing?.totalPrice) }}</span>
                  </div>
                  <details class="mt-3">
                    <summary class="cursor-pointer text-sm text-zinc-300">Detalles</summary>
                    <div class="mt-2 space-y-1 text-xs">
                      <div class="flex justify-between"><span class="text-zinc-500">Producto</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.productPrice) }}</span></div>
                      <div class="flex justify-between"><span class="text-zinc-500">Envío</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.shippingCost) }}</span></div>
                      <div class="flex justify-between"><span class="text-zinc-500">Aduana</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.customs) }}</span></div>
                      <div class="flex justify-between pt-1 border-t border-zinc-700"><span class="text-zinc-300">Total</span><span class="text-teal-300">{{ formatCurrency(pkg.pricing?.totalPrice) }}</span></div>
                    </div>
                  </details>
                  <div class="mt-2 text-xs text-yellow-400" v-if="pkg.pricing?.categoryC">Aplica categoría C</div>
                </div>
              </div>
              
              <div class="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
                <span class="text-zinc-300">Total de todos los paquetes</span>
                <span class="text-2xl font-bold text-teal-400">{{ formatCurrency(packageBreakdownTotal) }}</span>
              </div>
            </div>
          </FadeIn>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calculator-panel { min-height: 400px; }
.panel-title { font-size: 1rem; line-height: 1.4; font-weight: 600; }
</style>
