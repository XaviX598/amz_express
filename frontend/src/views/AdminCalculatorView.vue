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
  <div class="admin-calculator-page min-h-[calc(100dvh-64px)] pt-[64px] pb-16 relative overflow-hidden">
    <div class="calculator-backdrop absolute inset-0 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-[#35627A]/20 blur-3xl" />
    <div class="relative max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 min-h-[calc(100dvh-128px)] flex items-center">
      <div class="w-full calculator-shell">
        <FadeIn direction="up">
          <section class="calc-hero mb-7">
            <div>
              <p class="calc-hero-tag">Calculadora SuperAdmin</p>
              <h1 class="calc-hero-title">
                Panel de
                <span>cotización avanzada</span>
              </h1>
              <p class="calc-hero-subtitle">
                Misma estructura operacional de la calculadora principal, con reglas internas para equipo administrativo
                y tarifas especiales de despacho.
              </p>
              <div class="calc-note mt-4">
                <p class="calc-note-title">Perfil SuperAdmin</p>
                <p>
                  Esta vista aplica cálculo sin impuestos ni handling, manteniendo el motor de agrupación 4x4 con tarifa
                  logística configurada.
                </p>
              </div>
            </div>
            <div class="calc-hero-visual" aria-hidden="true">
              <div class="calc-hero-visual__label">Régimen interno activo</div>
            </div>
          </section>
        </FadeIn>

        <FadeIn direction="up" :delay="80">
          <div class="calc-brief mb-6">
            <div class="calc-brief__lead">
              <p class="calc-brief__kicker">Protocolo SA</p>
              <p class="text-sm sm:text-base leading-relaxed">
                Ingresá producto por URL o manualmente para estimar costos internos con las reglas de SuperAdmin.
                El cálculo conserva el algoritmo de paquetes 4x4 para comparar escenarios logísticos.
              </p>
            </div>
            <div class="calc-brief__chips">
              <span class="calc-chip">Sin impuestos</span>
              <span class="calc-chip">Handling = 0</span>
              <span class="calc-chip">Tarifas SA</span>
            </div>
          </div>
        </FadeIn>

        <div :class="['calc-grid grid grid-cols-1 lg:grid-cols-2 gap-6 xl:gap-8 items-start']">
          <FadeIn direction="up" :delay="100">
            <div class="calculator-panel calc-panel calc-panel--primary rounded-2xl p-6 sm:p-8 border border-white/18 h-full overflow-hidden flex flex-col">
              <div v-if="!showResult" class="space-y-6 flex-1 flex flex-col justify-center w-full max-w-[560px] mx-auto">
                <div class="calc-panel-head flex items-center justify-between gap-4">
                  <div>
                    <p class="calc-panel-eyebrow">Módulo SA-01</p>
                    <h2 class="panel-title text-white">Laboratorio de producto</h2>
                  </div>
                  <div class="mode-switch">
                    <button @click="setInputMode('url')" :class="['mode-btn', inputMode === 'url' ? 'is-active' : '']">URL</button>
                    <button @click="setInputMode('manual')" :class="['mode-btn', inputMode === 'manual' ? 'is-active' : '']">Manual</button>
                  </div>
                </div>

                <div v-if="inputMode === 'url'" class="space-y-4">
                  <label class="block text-sm font-medium text-zinc-300 mb-2">URL del Producto en Amazon</label>
                  <div class="relative">
                    <input
                      v-model="amazonUrl"
                      type="url"
                      placeholder="https://www.amazon.com/dp/B0XXXXX"
                      class="w-full rounded-xl py-3.5 px-4"
                    />
                  </div>
                  <p class="text-xs text-amber-400">Funcionalidad de extracción en desarrollo</p>
                </div>

                <div v-else class="space-y-4">
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Nombre del producto</label>
                    <input
                      v-model="productTitle"
                      type="text"
                      placeholder="Ej: Audífonos Sony"
                      class="w-full rounded-xl py-3.5 px-4"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Precio (USD)</label>
                    <input
                      v-model.number="productPrice"
                      type="number"
                      step="0.01"
                      min="0"
                      placeholder="Ej: 129.99"
                      class="w-full rounded-xl py-3.5 px-4"
                    />
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-zinc-300 mb-2">Peso (libras)</label>
                    <input
                      v-model.number="weight"
                      type="number"
                      step="0.1"
                      min="0"
                      placeholder="Ej: 2.5"
                      class="w-full rounded-xl py-3.5 px-4"
                    />
                  </div>
                </div>

                <p v-if="scrapeError" class="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-300">
                  {{ scrapeError }}
                </p>

                <div class="calc-action-row">
                  <button @click="calculateSingleProductPricing" :disabled="!hasValidPrice" class="calc-action-btn is-primary">
                    Calcular
                  </button>
                  <button @click="addCurrentProductToCart" :disabled="!hasValidPrice" class="calc-action-btn is-secondary">
                    Agregar al carrito
                  </button>
                </div>
              </div>

              <div v-else class="space-y-6">
                <h2 class="text-xl sm:text-2xl font-bold text-white">Desglose de Precios</h2>
                <div class="space-y-3 text-sm sm:text-base">
                  <div class="flex justify-between">
                    <span class="text-zinc-400">Producto</span>
                    <span class="text-white">{{ formatCurrency(pricingResult?.productPrice) }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-zinc-400">Envío ({{ getShippingLabel(weight || 0) }})</span>
                    <span class="text-white">{{ formatCurrency(pricingResult?.shippingCost) }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-zinc-400">Aduana (fijo)</span>
                    <span class="text-white">{{ formatCurrency(pricingResult?.customs) }}</span>
                  </div>
                </div>
                <div class="border-t border-white/10 pt-4">
                  <div class="flex justify-between items-center">
                    <span class="text-lg font-bold text-white">Total</span>
                    <span class="text-3xl sm:text-4xl font-bold text-[#e5aea9]">{{ formatCurrency(pricingResult?.totalPrice) }}</span>
                  </div>
                </div>
                <p v-if="pricingResult?.categoryC" class="p-3 bg-yellow-500/20 border border-yellow-500/50 rounded-xl text-yellow-300 text-sm">
                  Este pedido aplica categoría C
                </p>
                <button @click="cancelConfirmation" class="w-full text-center text-sm text-zinc-400 hover:text-[#e5aea9] py-2">
                  Calcular otro producto
                </button>
              </div>
            </div>
          </FadeIn>

          <FadeIn direction="up" :delay="240" class="h-full">
            <div class="calculator-panel calc-panel rounded-2xl p-6 sm:p-8 border border-white/18 h-full flex flex-col overflow-hidden">
              <div class="calc-panel-head flex items-center justify-between mb-4 pb-4 border-b border-[#a6a9d0]/25">
                <div>
                  <p class="calc-panel-eyebrow">Módulo SA-02</p>
                  <h3 class="panel-title text-white">Carrito de productos</h3>
                </div>
                <span v-if="!cartLoading" class="calc-count-pill">{{ cartItems.length }} item(s)</span>
              </div>

              <div v-if="cartLoading" class="flex items-center justify-center py-8">
                <div class="flex flex-col items-center gap-3">
                  <svg class="w-8 h-8 animate-spin text-[#e5aea9]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  <span class="text-sm text-zinc-500">Cargando carrito...</span>
                </div>
              </div>

              <div v-else-if="cartItems.length === 0" class="calc-empty-note text-sm mb-4">
                Agrega productos y calcula paquetes automáticos 4x4
              </div>

              <div v-else class="space-y-2 max-h-64 overflow-y-auto pr-1 mb-4">
                <div v-for="item in cartItems" :key="item.id" class="calc-cart-item rounded-lg p-3">
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

              <button @click="estimateCartPackages" :disabled="calculating || cartItems.length === 0" class="calc-action-btn is-primary w-full">
                <span v-if="calculating">Calculando...</span>
                <span v-else>Calcular paquetes 4x4</span>
              </button>
            </div>
          </FadeIn>

          <FadeIn v-if="packageBreakdown.length > 0" direction="up" :delay="160" class="lg:col-span-2">
            <div class="calculator-panel calc-panel rounded-2xl p-6 sm:p-8 border border-white/18">
              <div class="calc-panel-head flex items-center justify-between mb-4 pb-4 border-b border-[#a6a9d0]/25">
                <div>
                  <p class="calc-panel-eyebrow">Módulo SA-03</p>
                  <h3 class="panel-title text-white">Paquetes sugeridos (4x4)</h3>
                </div>
                <button @click="clearCart" class="text-sm text-zinc-400 hover:text-white">Limpiar</button>
              </div>

              <div class="space-y-3">
                <div v-for="pkg in packageBreakdown" :key="pkg.id" class="calc-package-card rounded-xl border p-4">
                  <p class="text-white font-medium">Paquete {{ pkg.id }}</p>
                  <p class="text-xs text-zinc-400 mt-1">{{ pkg.items.length }} producto(s) · {{ formatCurrency(pkg.subtotalPrice) }} · {{ pkg.subtotalWeight.toFixed(2) }} lbs</p>
                  <div class="mt-3 flex justify-between text-sm">
                    <span class="text-zinc-400">Total</span>
                    <span class="text-[#e5aea9] font-semibold">{{ formatCurrency(pkg.pricing?.totalPrice) }}</span>
                  </div>
                  <details class="mt-3 rounded-lg border border-zinc-700/40 bg-zinc-900/60 p-3">
                    <summary class="cursor-pointer text-sm text-zinc-300 hover:text-white transition-colors">Detalles</summary>
                    <div class="mt-3 space-y-2 text-xs">
                      <div class="flex justify-between"><span class="text-zinc-500">Producto</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.productPrice) }}</span></div>
                      <div class="flex justify-between"><span class="text-zinc-500">Envío</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.shippingCost) }}</span></div>
                      <div class="flex justify-between"><span class="text-zinc-500">Aduana</span><span class="text-zinc-200">{{ formatCurrency(pkg.pricing?.customs) }}</span></div>
                      <div class="pt-2 border-t border-zinc-700/50 flex justify-between"><span class="text-zinc-300">Total final</span><span class="text-[#e5aea9]">{{ formatCurrency(pkg.pricing?.totalPrice) }}</span></div>
                    </div>
                  </details>
                  <div class="mt-2 text-xs text-yellow-300" v-if="pkg.pricing?.categoryC">Aplica categoría C</div>
                </div>
              </div>

              <div class="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
                <span class="text-zinc-300">Total de todos los paquetes</span>
                <span class="text-2xl font-bold text-[#e5aea9]">{{ formatCurrency(packageBreakdownTotal) }}</span>
              </div>
            </div>
          </FadeIn>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-calculator-page {
  background:
    radial-gradient(circle at 16% 10%, rgba(53, 98, 122, 0.18), transparent 36%),
    radial-gradient(circle at 82% 14%, rgba(166, 169, 208, 0.16), transparent 34%),
    linear-gradient(180deg, #0a1218 0%, #060b0f 100%);
}

.calculator-backdrop {
  background-image: radial-gradient(rgba(229, 174, 169, 0.08) 0.6px, transparent 0.6px);
  background-size: 3px 3px;
  opacity: 0.2;
}

.calculator-shell {
  animation: calculator-enter 0.45s ease-out;
}

.calc-hero {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(166, 169, 208, 0.28);
  background:
    linear-gradient(155deg, rgba(4, 11, 16, 0.94), rgba(7, 15, 22, 0.9)),
    rgba(5, 12, 18, 0.9);
  box-shadow: 0 22px 48px rgba(0, 0, 0, 0.46);
}

.calc-hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.66rem;
  border-radius: 0.35rem;
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #f2d2cf;
  border: 1px solid rgba(229, 174, 169, 0.34);
  background: rgba(229, 174, 169, 0.12);
}

.calc-hero-title {
  margin-top: 0.9rem;
  font-size: clamp(1.8rem, 3.3vw, 3rem);
  line-height: 0.95;
  font-weight: 800;
  letter-spacing: -0.02em;
  text-transform: uppercase;
  color: #f7fcff;
}

.calc-hero-title span {
  color: #e5aea9;
  text-shadow: 0 0 18px rgba(229, 174, 169, 0.2);
}

.calc-hero-subtitle {
  margin-top: 0.9rem;
  max-width: 52ch;
  color: #b7c7ce;
  line-height: 1.6;
}

.calc-note {
  border: 1px solid rgba(166, 169, 208, 0.25);
  border-radius: 0.4rem;
  background: rgba(8, 22, 32, 0.72);
  padding: 0.75rem 0.9rem;
}

.calc-note-title {
  margin-bottom: 0.35rem;
  text-transform: uppercase;
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  color: #e5aea9;
  font-weight: 700;
}

.calc-note p {
  color: #c2d2da;
  font-size: 0.88rem;
  line-height: 1.55;
}

.calc-hero-visual {
  border-radius: 0.55rem;
  border: 1px solid rgba(166, 169, 208, 0.28);
  min-height: 240px;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.62)),
    url('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=1200&q=80') center/cover no-repeat;
  position: relative;
  overflow: hidden;
}

.calc-hero-visual::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(53, 98, 122, 0.18), transparent 45%, rgba(166, 169, 208, 0.2));
  mix-blend-mode: screen;
}

.calc-hero-visual__label {
  position: absolute;
  left: 0.8rem;
  bottom: 0.8rem;
  z-index: 1;
  padding: 0.28rem 0.54rem;
  border-radius: 0.3rem;
  border: 1px solid rgba(166, 169, 208, 0.45);
  background: rgba(0, 0, 0, 0.5);
  color: #f5f5f5;
  font-size: 0.68rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.calc-brief {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.9rem;
  align-items: center;
  border-radius: 0.75rem;
  border: 1px solid rgba(166, 169, 208, 0.24);
  background:
    linear-gradient(100deg, rgba(7, 20, 30, 0.9), rgba(9, 24, 35, 0.7)),
    rgba(8, 20, 30, 0.74);
  padding: 0.9rem 1rem;
}

.calc-brief__lead p {
  color: #c2d2da;
}

.calc-brief__kicker {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.11em;
  padding: 0.24rem 0.64rem;
  margin-bottom: 0.45rem;
  color: #f2d2cf;
  border: 1px solid rgba(229, 174, 169, 0.34);
  background: rgba(229, 174, 169, 0.1);
}

.calc-brief__chips {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.calc-chip {
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 0.28rem 0.52rem;
  border-radius: 0.36rem;
  color: #eaf2f6;
  border: 1px solid rgba(166, 169, 208, 0.28);
  background: rgba(10, 28, 40, 0.74);
}

.calc-grid {
  align-items: stretch;
}

.calculator-panel {
  min-height: 520px;
  transition: transform 220ms ease, border-color 220ms ease, box-shadow 220ms ease;
  border-radius: 0.6rem;
}

.calc-panel {
  background:
    linear-gradient(165deg, rgba(13, 22, 31, 0.96), rgba(8, 14, 21, 0.9)),
    rgba(6, 12, 18, 0.9) !important;
  border-color: rgba(166, 169, 208, 0.26) !important;
  box-shadow:
    0 20px 38px rgba(0, 0, 0, 0.48),
    inset 0 1px 0 rgba(229, 174, 169, 0.08);
}

.calculator-panel:hover {
  transform: translateY(-3px);
  border-color: rgba(229, 174, 169, 0.36) !important;
  box-shadow:
    0 22px 42px rgba(0, 0, 0, 0.56),
    0 0 0 1px rgba(229, 174, 169, 0.09) inset;
}

.calc-panel-eyebrow {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: #a6a9d0;
  margin-bottom: 0.12rem;
}

.panel-title {
  font-size: 1rem;
  line-height: 1.4;
  font-weight: 700;
  color: #f1fbff !important;
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem;
  border-radius: 0.42rem;
  background: rgba(7, 17, 25, 0.72);
  border: 1px solid rgba(166, 169, 208, 0.24);
}

.mode-btn {
  border: none;
  background: transparent;
  color: #a8bbc5;
  padding: 0.34rem 0.72rem;
  border-radius: 0.32rem;
  font-size: 0.84rem;
  transition: background-color 180ms ease, color 180ms ease;
}

.mode-btn.is-active {
  background: linear-gradient(135deg, rgba(53, 98, 122, 0.95), rgba(166, 169, 208, 0.92));
  color: #f5f5f5;
  font-weight: 700;
}

.calc-action-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.65rem;
}

.calc-action-btn {
  border-radius: 0.72rem;
  border: 1px solid rgba(166, 169, 208, 0.3);
  padding: 0.75rem 1rem;
  font-weight: 600;
  transition: transform 160ms ease, border-color 160ms ease, filter 160ms ease;
}

.calc-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.calc-action-btn:not(:disabled):hover {
  transform: translateY(-1px);
  border-color: rgba(229, 174, 169, 0.5);
}

.calc-action-btn.is-primary {
  color: #f3fbff;
  background: linear-gradient(135deg, #35627a, #3f6f89);
}

.calc-action-btn.is-secondary {
  color: #e7eff4;
  background: linear-gradient(135deg, rgba(53, 98, 122, 0.72), rgba(166, 169, 208, 0.48));
}

.calc-count-pill {
  font-size: 0.72rem;
  border-radius: 9999px;
  padding: 0.24rem 0.58rem;
  color: #d8e8ef;
  border: 1px solid rgba(166, 169, 208, 0.3);
  background: rgba(10, 27, 38, 0.6);
}

.calc-empty-note {
  border: 1px dashed rgba(166, 169, 208, 0.3);
  border-radius: 0.7rem;
  padding: 0.7rem 0.8rem;
  color: #9eb3bf;
  background: rgba(8, 22, 32, 0.48);
}

.calc-cart-item {
  border: 1px solid rgba(166, 169, 208, 0.24);
  background:
    linear-gradient(130deg, rgba(9, 23, 34, 0.84), rgba(7, 18, 28, 0.68));
  box-shadow: inset 0 1px 0 rgba(245, 245, 245, 0.03);
}

.calc-package-card {
  border-color: rgba(166, 169, 208, 0.24) !important;
  background:
    linear-gradient(150deg, rgba(9, 22, 33, 0.86), rgba(7, 18, 27, 0.7)) !important;
}

.calculator-shell :is(input, select, textarea) {
  background: rgba(6, 18, 28, 0.9) !important;
  border: 1px solid rgba(166, 169, 208, 0.24) !important;
  color: #eef5f8 !important;
}

.calculator-shell :is(input, select, textarea)::placeholder {
  color: #93aab5 !important;
}

.calculator-shell :is(input, select, textarea):focus {
  border-color: #e5aea9 !important;
  box-shadow: 0 0 0 3px rgba(229, 174, 169, 0.2) !important;
}

.calculator-shell .text-white {
  color: #eef5f8 !important;
}

.calculator-shell .text-zinc-300,
.calculator-shell .text-zinc-400,
.calculator-shell .text-zinc-500 {
  color: #a9bdc7 !important;
}

.calculator-shell .text-amber-400,
.calculator-shell .text-yellow-300,
.calculator-shell .text-red-300 {
  color: #ff9b95 !important;
}

@media (max-width: 1200px) {
  .calc-hero {
    grid-template-columns: 1fr;
  }

  .calc-hero-visual {
    min-height: 200px;
  }

  .calc-brief {
    grid-template-columns: 1fr;
  }

  .calc-brief__chips {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .calculator-panel {
    min-height: auto;
  }

  .calc-action-row {
    grid-template-columns: 1fr;
  }
}

@keyframes calculator-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
