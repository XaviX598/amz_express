<script setup lang="ts">
import { onMounted, computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { useAuthStore } from '@/stores/auth'
import OrderProgress from '@/components/OrderProgress.vue'
import FadeIn from '@/components/FadeIn.vue'
import MotionButton from '@/components/MotionButton.vue'
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
  <div class="orders-page min-h-screen pt-[64px] pb-16 relative overflow-hidden">
    <div class="orders-backdrop absolute inset-0 pointer-events-none" />
    <div class="absolute top-0 left-1/2 -translate-x-1/2 w-full h-96 bg-[#35627A]/20 blur-3xl pointer-events-none" />

    <div class="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <FadeIn direction="up">
        <section class="orders-hero mb-7">
          <div class="flex-1">
            <p class="orders-kicker">Consola de seguimiento</p>
            <h1 class="text-3xl sm:text-4xl font-bold text-white">Mis pedidos</h1>
            <p class="orders-subtitle mt-2">Seguimiento integral de estados, pagos y entregas de tus importaciones.</p>
          </div>
          <div class="orders-hero__actions">
            <span class="orders-stat-pill">{{ filteredOrders.length }} activos</span>
          </div>
        </section>
      </FadeIn>

      <!-- Filters -->
      <FadeIn v-if="orders.length > 0" direction="up" :delay="100">
        <div class="orders-filters mb-6 p-4 rounded-xl">
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
                class="w-full pl-10 pr-4 py-2.5 orders-input rounded-lg text-white placeholder-zinc-500 focus:outline-none"
              />
            </div>

            <!-- Date range -->
            <div class="flex items-center gap-2">
              <input
                v-model="dateFrom"
                type="date"
                class="px-3 py-2.5 orders-input rounded-lg text-white text-sm focus:outline-none"
              />
              <span class="text-zinc-500">-</span>
              <input
                v-model="dateTo"
                type="date"
                class="px-3 py-2.5 orders-input rounded-lg text-white text-sm focus:outline-none"
              />
            </div>

            <!-- Clear filters -->
            <button
              @click="clearFilters"
              class="px-4 py-2.5 text-sm text-zinc-300 hover:text-white transition-colors bg-zinc-800/50 rounded-lg hover:bg-zinc-700/50 border border-[#a6a9d0]/30"
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
          <div class="w-12 h-12 border-4 border-[#a6a9d0]/20 border-t-[#e5aea9] rounded-full animate-spin" />
        </div>
      </FadeIn>

      <!-- Empty State -->
      <FadeIn v-else-if="orders.length === 0" direction="up">
        <div class="orders-empty rounded-2xl p-8 sm:p-12 text-center">
          <div class="w-20 h-20 bg-zinc-800/60 rounded-full flex items-center justify-center mx-auto mb-6 border border-[#a6a9d0]/30">
            <svg class="w-10 h-10 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
          </div>
          <h3 class="text-xl font-bold mb-2 text-white">No tienes pedidos</h3>
          <p v-if="searchQuery || dateFrom || dateTo" class="text-zinc-400 mb-6">No se encontraron pedidos con esos filtros</p>
          <p v-else class="text-zinc-400 mb-6">Empieza calculando el precio de un producto</p>
          <MotionButton
            v-if="!searchQuery && !dateFrom && !dateTo"
            to="/calculadora"
            label="Ir a la Calculadora"
            variant="primary"
            size="lg"
          />
          <MotionButton
            v-else
            @click="clearFilters"
            label="Limpiar filtros"
            variant="primary"
            size="lg"
          />
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
          <div class="orders-card rounded-2xl p-6 card-hover">
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
                <p class="text-zinc-400 text-sm">
                  {{ formatDate(order.createdAt) }}
                </p>
              </div>

              <!-- Price -->
              <div class="text-right lg:text-center">
                <p class="text-sm text-zinc-400">Total</p>
                <p class="text-2xl font-bold text-[#e5aea9]">
                  {{ formatCurrency(order.totalPrice) }}
                </p>
              </div>

              <!-- Actions -->
              <div class="flex items-center gap-4">
                <RouterLink
                  :to="`/ordenes/${order.id}`"
                  class="bg-zinc-800/80 hover:bg-zinc-700 border border-[#a6a9d0]/35 hover:border-[#e5aea9]/45 px-6 py-2.5 rounded-xl font-medium transition-all text-white"
                >
                  Ver Detalle
                </RouterLink>
              </div>
            </div>

            <!-- Progress -->
            <div class="mt-6 pt-6 border-t border-[#a6a9d0]/20">
              <OrderProgress :current-status="order.status" />
            </div>
          </div>
        </FadeIn>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-between mt-8 pt-6 border-t border-[#a6a9d0]/20">
          <div class="text-sm text-zinc-300">
            Página {{ currentPage }} de {{ totalPages }}
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="prevPage"
              :disabled="currentPage === 1"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white border border-[#a6a9d0]/30"
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
                    ? 'bg-[#35627A] text-white' 
                    : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border border-[#a6a9d0]/30'
                ]"
              >
                {{ page }}
              </button>
            </div>

            <button
              @click="nextPage"
              :disabled="currentPage === totalPages"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-zinc-800 hover:bg-zinc-700 text-white border border-[#a6a9d0]/30"
            >
              Siguiente
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  background:
    radial-gradient(circle at 10% 14%, rgba(53, 98, 122, 0.2), transparent 36%),
    radial-gradient(circle at 88% 18%, rgba(166, 169, 208, 0.16), transparent 34%),
    linear-gradient(180deg, #091018 0%, #060b11 100%);
}

.orders-backdrop {
  background-image: radial-gradient(rgba(229, 174, 169, 0.08) 0.6px, transparent 0.6px);
  background-size: 3px 3px;
  opacity: 0.2;
}

.orders-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 1rem;
  border: 1px solid rgba(166, 169, 208, 0.26);
  background:
    linear-gradient(150deg, rgba(8, 21, 31, 0.92), rgba(7, 16, 24, 0.82)),
    rgba(8, 20, 30, 0.78);
  box-shadow: 0 20px 42px rgba(0, 0, 0, 0.4);
}

.orders-kicker {
  display: inline-flex;
  align-items: center;
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  border-radius: 9999px;
  padding: 0.28rem 0.62rem;
  color: #f2d2cf;
  border: 1px solid rgba(229, 174, 169, 0.35);
  background: rgba(229, 174, 169, 0.12);
  margin-bottom: 0.5rem;
}

.orders-subtitle {
  color: #b6c7cf;
}

.orders-hero__actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.orders-stat-pill {
  font-size: 0.72rem;
  border-radius: 9999px;
  padding: 0.3rem 0.64rem;
  color: #d8e8ef;
  border: 1px solid rgba(166, 169, 208, 0.3);
  background: rgba(10, 27, 38, 0.6);
}

.orders-filters {
  border: 1px solid rgba(166, 169, 208, 0.24);
  background:
    linear-gradient(140deg, rgba(8, 20, 30, 0.88), rgba(8, 19, 28, 0.72)),
    rgba(8, 20, 30, 0.68);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.3);
}

.orders-input {
  background: rgba(5, 14, 22, 0.84);
  border: 1px solid rgba(166, 169, 208, 0.24);
}

.orders-input:focus {
  border-color: rgba(229, 174, 169, 0.45);
  box-shadow: 0 0 0 3px rgba(229, 174, 169, 0.16);
}

.orders-empty {
  border: 1px solid rgba(166, 169, 208, 0.24);
  background:
    linear-gradient(140deg, rgba(8, 20, 30, 0.88), rgba(8, 19, 28, 0.72)),
    rgba(8, 20, 30, 0.68);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.34);
}

.orders-card {
  border: 1px solid rgba(166, 169, 208, 0.24);
  background:
    linear-gradient(145deg, rgba(8, 20, 30, 0.92), rgba(7, 16, 25, 0.82)),
    rgba(8, 20, 30, 0.78);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.3);
}

.orders-card:hover {
  border-color: rgba(229, 174, 169, 0.34);
}

@media (max-width: 640px) {
  .orders-hero {
    align-items: flex-start;
  }

  .orders-hero__actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>

