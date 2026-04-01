<script setup lang="ts">
import { onMounted, computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { useAuthStore } from '@/stores/auth'
import OrderProgress from '@/components/OrderProgress.vue'
import FadeIn from '@/components/FadeIn.vue'
import type { Order, OrderStatus } from '@/types'

const orderStore = useOrderStore()
const authStore = useAuthStore()

// Filtros
const searchQuery = ref('')
const dateFrom = ref('')
const dateTo = ref('')
const currentPage = ref(1)
const itemsPerPage = 10

onMounted(() => {
  orderStore.fetchMyOrders()
})

const orders = computed(() => orderStore.orders)
const loading = computed(() => orderStore.loading)

const totalPages = computed(() => Math.ceil(filteredOrders.value.length / itemsPerPage))

const filteredOrders = computed(() => {
  let result = orders.value
  
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase().trim()
    
    // Try to parse as number for order ID
    const orderId = parseInt(query)
    if (!isNaN(orderId)) {
      result = result.filter(o => o.id === orderId)
    } else {
      // Search by product name (case insensitive)
      result = result.filter(o => 
        o.productName?.toLowerCase().includes(query)
      )
    }
  }
  
  if (dateFrom.value) {
    const fromDate = new Date(dateFrom.value)
    result = result.filter(o => new Date(o.createdAt) >= fromDate)
  }
  
  if (dateTo.value) {
    const toDate = new Date(dateTo.value)
    toDate.setHours(23, 59, 59, 999)
    result = result.filter(o => new Date(o.createdAt) <= toDate)
  }
  
  return result
})

const paginatedOrders = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredOrders.value.slice(start, start + itemsPerPage)
})

// Reset page when filters change
watch([searchQuery, dateFrom, dateTo], () => {
  currentPage.value = 1
})

function clearFilters() {
  searchQuery.value = ''
  dateFrom.value = ''
  dateTo.value = ''
  currentPage.value = 1
}

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}

function prevPage() {
  if (currentPage.value > 1) currentPage.value--
}

function goToPage(page: number) {
  currentPage.value = page
}

const statusLabels: Record<OrderStatus, string> = {
  PENDIENTE_PAGO: 'Pendiente de Pago',
  PAGADO: 'Pagado',
  PEDIDO_REALIZADO: 'Pedido Realizado',
  LLEGO_BODEGA_USA: 'En bodega USA',
  EN_TRANSITO_ECUADOR: 'En tránsito',
  LLEGO_BODEGA_ECUADOR: 'Llegó a Ecuador',
  LISTO_ENTREGA: 'Listo para entrega',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
}

const statusColors: Record<OrderStatus, string> = {
  PENDIENTE_PAGO: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
  PAGADO: 'bg-green-500/20 text-green-400 border border-green-500/30',
  PEDIDO_REALIZADO: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
  LLEGO_BODEGA_USA: 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30',
  EN_TRANSITO_ECUADOR: 'bg-orange-500/20 text-orange-400 border border-orange-500/30',
  LLEGO_BODEGA_ECUADOR: 'bg-purple-500/20 text-purple-400 border border-purple-500/30',
  LISTO_ENTREGA: 'bg-green-500/20 text-green-400 border border-green-500/30',
  ENTREGADO: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
  CANCELADO: 'bg-red-500/20 text-red-400 border border-red-500/30',
}

function isTransferUnderReview(order: Order): boolean {
  return order.status === 'PENDIENTE_PAGO'
    && order.paymentMethod === 'TRANSFERENCIA'
    && (
      order.paymentReference?.includes('Comprobante')
      || order.notes?.includes('Comprobante enviado')
    )
}

function getStatusLabel(order: Order): string {
  if (isTransferUnderReview(order)) {
    return 'Comprobando pago'
  }
  return statusLabels[order.status]
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('es-EC', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function formatCurrency(value: number | undefined): string {
  if (!value) return '$0.00'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value)
}
</script>

<template>
  <div class="min-h-screen pt-[64px] pb-16">
    <!-- Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-zinc-900 via-zinc-900 to-zinc-950 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-teal-500/5 blur-3xl" />

    <div class="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <FadeIn direction="up">
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <div>
            <h1 class="text-3xl font-bold text-white">Mis Pedidos</h1>
            <p class="text-zinc-400 mt-1">Seguimiento de todos tus pedidos</p>
          </div>
          <RouterLink to="/calculadora" class="btn-primary px-5 py-2.5">
            Nuevo Pedido
          </RouterLink>
        </div>
      </FadeIn>

      <!-- Filters -->
      <FadeIn v-if="orders.length > 0" direction="up" :delay="100">
        <div class="mb-6 p-4 glass rounded-xl border border-white/5">
          <div class="flex flex-wrap gap-4 items-center">
            <!-- Search by ID or product -->
            <div class="relative flex-1 min-w-[200px]">
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <svg class="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Buscar por #ID o producto..."
                class="w-full pl-10 pr-4 py-2.5 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-white placeholder-zinc-500 focus:outline-none focus:border-teal-500/50"
              />
            </div>

            <!-- Date range -->
            <div class="flex items-center gap-2">
              <input
                v-model="dateFrom"
                type="date"
                class="px-3 py-2.5 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-white text-sm focus:outline-none focus:border-teal-500/50"
              />
              <span class="text-zinc-500">-</span>
              <input
                v-model="dateTo"
                type="date"
                class="px-3 py-2.5 bg-zinc-800/80 border border-zinc-700/50 rounded-lg text-white text-sm focus:outline-none focus:border-teal-500/50"
              />
            </div>

            <!-- Clear filters -->
            <button
              @click="clearFilters"
              class="px-4 py-2.5 text-sm text-zinc-400 hover:text-white transition-colors bg-zinc-800/50 rounded-lg hover:bg-zinc-700/50"
            >
              Limpiar
            </button>

            <!-- Results count -->
            <div class="text-sm text-zinc-500 ml-auto">
              {{ filteredOrders.length }} pedidos
            </div>
          </div>
        </div>
      </FadeIn>

      <!-- Loading -->
      <FadeIn v-if="loading" direction="up">
        <div class="flex justify-center py-12">
          <div class="w-12 h-12 border-4 border-teal-500/20 border-t-teal-500 rounded-full animate-spin" />
        </div>
      </FadeIn>

      <!-- Empty State -->
      <FadeIn v-else-if="orders.length === 0" direction="up">
        <div class="glass rounded-2xl p-8 sm:p-12 text-center border border-white/5">
          <div class="w-20 h-20 bg-zinc-800/50 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg class="w-10 h-10 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
          </div>
          <h3 class="text-xl font-bold mb-2 text-white">No tienes pedidos</h3>
          <p v-if="searchQuery || dateFrom || dateTo" class="text-zinc-400 mb-6">No se encontraron pedidos con esos filtros</p>
          <p v-else class="text-zinc-400 mb-6">Empieza calculando el precio de un producto</p>
          <RouterLink v-if="!searchQuery && !dateFrom && !dateTo" to="/calculadora" class="btn-primary inline-block px-6 py-3">
            Ir a la Calculadora
          </RouterLink>
          <button v-else @click="clearFilters" class="btn-primary inline-block px-6 py-3">
            Limpiar filtros
          </button>
        </div>
      </FadeIn>

      <!-- Orders List -->
      <div v-else class="space-y-6">
        <FadeIn
          v-for="(order, index) in paginatedOrders"
          :key="order.id"
          direction="up"
          :delay="index * 100"
        >
          <div class="glass rounded-2xl p-6 card-hover border border-white/5">
            <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
              <!-- Order Info -->
              <div class="flex-1">
                <div class="flex items-center gap-4 mb-4">
                  <span class="text-2xl font-bold text-white">#{{ order.id }}</span>
                  <span :class="['px-3 py-1 rounded-full text-sm font-medium', statusColors[order.status]]">
                    {{ getStatusLabel(order) }}
                  </span>
                </div>
                
                <h3 class="text-lg font-medium mb-1 text-white">
                  {{ order.productName || 'Producto Amazon' }}
                </h3>
                <p v-if="order.productAsin" class="text-zinc-500 text-sm mb-2">
                  ASIN: {{ order.productAsin }}
                </p>
                <p class="text-zinc-500 text-sm">
                  {{ formatDate(order.createdAt) }}
                </p>
              </div>

              <!-- Price -->
              <div class="text-right lg:text-center">
                <p class="text-sm text-zinc-500">Total</p>
                <p class="text-2xl font-bold text-teal-400">
                  {{ formatCurrency(order.totalPrice) }}
                </p>
              </div>

              <!-- Actions -->
              <div class="flex items-center gap-4">
                <RouterLink
                  :to="`/ordenes/${order.id}`"
                  class="bg-zinc-800/80 hover:bg-zinc-700 border border-zinc-700/50 hover:border-zinc-600 px-6 py-2.5 rounded-xl font-medium transition-all text-white"
                >
                  Ver Detalle
                </RouterLink>
              </div>
            </div>

            <!-- Progress -->
            <div class="mt-6 pt-6 border-t border-white/5">
              <OrderProgress :current-status="order.status" />
            </div>
          </div>
        </FadeIn>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-between mt-8 pt-6 border-t border-white/10">
          <div class="text-sm text-zinc-400">
            Página {{ currentPage }} de {{ totalPages }}
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="prevPage"
              :disabled="currentPage === 1"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white"
            >
              Anterior
            </button>
            
            <div class="flex gap-1">
              <button
                v-for="page in totalPages"
                :key="page"
                @click="goToPage(page)"
                :class="[
                  'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                  page === currentPage 
                    ? 'bg-teal-500 text-white' 
                    : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300'
                ]"
              >
                {{ page }}
              </button>
            </div>

            <button
              @click="nextPage"
              :disabled="currentPage === totalPages"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

